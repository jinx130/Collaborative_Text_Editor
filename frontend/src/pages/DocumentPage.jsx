import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { deleteDocument, fetchDocument, updateDocument } from '../api/documentApi.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { getToken } from '../api/authApi.js';
import { createCollaborationSocket, wsUrl } from '../api/collaborationSocket.js';
import { createClientStateMachine } from '../ot/clientStateMachine.js';
import { asComponents, invertOperation, isNoop, transformCursor } from '../ot/operations.js';
import Editor from '../components/Editor.jsx';
import ShareDialog from '../components/ShareDialog.jsx';
import PresenceList from '../components/PresenceList.jsx';
import ConnectionStatus from '../components/ConnectionStatus.jsx';
import RevisionBadge from '../components/RevisionBadge.jsx';

// Page 2: OT live editor (primary) + REST for title/meta (fallback).
export default function DocumentPage({ id, onBack }) {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [updatedAt, setUpdatedAt] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [myRole, setMyRole] = useState('OWNER');
  const [showShare, setShowShare] = useState(false);
  const [otState, setOtState] = useState('synced');
  const [conn, setConn] = useState('connecting');
  const [users, setUsers] = useState([]);

  const { user, logout, handleUnauthorized } = useAuth();
  // Random per-tab connection id; the DISPLAY name always comes from the
  // account (Google-Docs style) — never invented client-side.
  const clientId = useMemo(
    () => `c-${Math.random().toString(36).slice(2, 8)}`,
    []
  );
  const name = user?.name ?? 'Anonymous';
  const machineRef = useRef(null);
  const socketRef = useRef(null);
  const revRef = useRef(0);
  const contentRef = useRef('');
  const undoRef = useRef([]);
  const redoRef = useRef([]);
  const titleTimer = useRef(null);
  const presenceTimer = useRef(null);
  if (!machineRef.current) machineRef.current = createClientStateMachine('', 0);

  function syncUi() {
    const m = machineRef.current;
    setContent(m.getDoc());
    contentRef.current = m.getDoc();
    setRevision(m.getRevision());
    revRef.current = m.getRevision();
    setOtState(m.getState());
  }

  const sendOp = useCallback((op) => {
    const sock = socketRef.current;
    if (!sock || !op || isNoop(op)) return;
    sock.sendOperation(op, revRef.current);
  }, []);

  // Initial REST load for title/meta; content snapshot comes from WS JOINED.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const doc = await fetchDocument(id);
        if (!cancelled && doc) {
          setTitle(doc.title ?? '');
          setUpdatedAt(doc.updatedAt ?? null);
          // Seed content immediately so the page isn't blank before WS joins.
          machineRef.current.setSnapshot(doc.content ?? '', doc.revision ?? 0);
          setContent(doc.content ?? '');
          contentRef.current = doc.content ?? '';
          setRevision(doc.revision ?? 0);
          revRef.current = doc.revision ?? 0;
          setError('');
        }
      } catch (e) {
        if (e?.status === 401) handleUnauthorized();
        else if (!cancelled) setError('Could not load document.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
      clearTimeout(titleTimer.current);
      clearTimeout(presenceTimer.current);
    };
  }, [id, handleUnauthorized]);

  // Live OT socket lifecycle. Reconnects when the tab's account changes,
  // carrying that account's token on every message.
  const token = getToken();
  useEffect(() => {
    const sock = createCollaborationSocket({
      url: wsUrl(),
      documentId: id,
      clientId,
      name,
      token,
      onStatus: (s) => setConn(s),
    });
    socketRef.current = sock;

    const offJoined = sock.on('joined', (msg) => {
      setError('');
      setMyRole(msg.role ?? 'OWNER');
      machineRef.current.setSnapshot(msg.content ?? '', msg.revision ?? 0);
      // Rebase undo history on fresh snapshot.
      undoRef.current = [];
      redoRef.current = [];
      syncUi();
    });
    const offAck = sock.on('ack', (msg) => {
      setError('');
      const nextOp = machineRef.current.handleAck(msg.revision ?? revRef.current);
      syncUi();
      if (nextOp) sendOp(nextOp);
    });
    const offRemote = sock.on('remote', (msg) => {
      const remote = asComponents(msg.operation);
      if (remote.length === 0) return;
      machineRef.current.handleRemote(remote, msg.revision ?? revRef.current, transformCursor);
      syncUi();
    });
    const offPresence = sock.on('presence', (msg) => {
      setUsers(msg.users ?? []);
    });
    const offError = sock.on('error', (msg) => {
      if (msg.message === 'login required') handleUnauthorized();
      else setError(msg.message ?? 'Sync error');
    });

    return () => {
      offJoined();
      offAck();
      offRemote();
      offPresence();
      offError();
      sock.close();
      socketRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, clientId, name, token, user?.email]);

  function scheduleTitleSave(nextTitle) {
    clearTimeout(titleTimer.current);
    titleTimer.current = setTimeout(async () => {
      setSaving(true);
      try {
        const updated = await updateDocument(id, { title: nextTitle });
        setUpdatedAt(updated.updatedAt ?? null);
      } catch (e) {
        if (e?.status === 401) handleUnauthorized();
        else if (e?.status === 403) setError('View only — you cannot edit this document.');
        else setError('Could not save title.');
      } finally {
        setSaving(false);
      }
    }, 500);
  }

  function handleTitleChange(value) {
    setTitle(value);
    scheduleTitleSave(value);
  }

  const myRoleRef = useRef(myRole);
  myRoleRef.current = myRole;

  function handleLocalEdit(nextText, op, cursor) {
    if (myRoleRef.current === 'VIEWER') return; // server would reject too
    const before = contentRef.current;
    const opToSend = machineRef.current.applyLocal(op);
    // Track undo (store pre-image for delete inversion).
    if (op && !isNoop(op)) {
      undoRef.current.push({ op, before });
      if (undoRef.current.length > 100) undoRef.current.shift();
      redoRef.current = [];
    }
    syncUi();
    if (opToSend) sendOp(opToSend);
    // Presence heartbeat with cursor.
    clearTimeout(presenceTimer.current);
    presenceTimer.current = setTimeout(() => {
      socketRef.current?.sendPresence(cursor ?? nextText.length);
    }, 150);
  }

  function handleUndo() {
    const entry = undoRef.current.pop();
    if (!entry) return;
    const inv = invertOperation(entry.op, entry.before);
    redoRef.current.push({ op: entry.op, before: entry.before });
    const opToSend = machineRef.current.applyLocal(inv);
    syncUi();
    if (opToSend) sendOp(opToSend);
  }

  async function handleDelete() {
    if (!window.confirm('Delete this document?')) return;
    try {
      socketRef.current?.leave();
      await deleteDocument(id);
      onBack();
    } catch (e) {
      if (e?.status === 401) handleUnauthorized();
      else if (e?.status === 403) setError('Only the owner can delete this document.');
      else setError('Could not delete document.');
    }
  }

  if (loading) {
    return (
      <div className="layout">
        <main className="editor-area">
          <p className="muted">Loading…</p>
        </main>
      </div>
    );
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <button className="new-btn" onClick={onBack}>
          ← All documents
        </button>
        <div className="side-section">
          <p className="muted small">Live collaborators</p>
          <PresenceList users={users} selfId={clientId} />
        </div>
      </aside>

      <main className="editor-area">
        {error && <p className="error">{error}</p>}
        <div className="editor-header">
          <input
            className="title-input"
            value={title}
            onChange={(e) => handleTitleChange(e.target.value)}
            placeholder="Document title"
          />
          <div className="header-actions">
            <span className="muted small" title={user?.email ?? ''}>{user?.name}</span>
            <button className="new-btn" onClick={() => setShowShare(true)} title="Share with others">
              Share
            </button>
            <button className="new-btn" onClick={logout} title="Log out">
              Log out
            </button>
            <ConnectionStatus status={conn} otState={otState} />
            <RevisionBadge revision={revision} />
            <span className="muted small">{saving ? 'Saving title…' : 'Title saved'}</span>
            <button className="new-btn" onClick={handleUndo} disabled={undoRef.current.length === 0} title="Undo (OT invert)">
              Undo
            </button>
            <button className="delete-btn" onClick={handleDelete}>
              Delete
            </button>
          </div>
        </div>
        {myRole === 'VIEWER' && <p className="muted small">View only — ask the owner for Editor access to make changes.</p>}
        <Editor
          value={content}
          onLocalEdit={handleLocalEdit}
          disabled={myRole === 'VIEWER' || (conn === 'connecting' && !content)}
        />
        {showShare && (
          <ShareDialog docId={id} isOwner={myRole === 'OWNER'} onClose={() => setShowShare(false)} />
        )}
        {updatedAt && (
          <p className="muted small">Last updated: {new Date(updatedAt).toLocaleString()}</p>
        )}
      </main>
    </div>
  );
}
