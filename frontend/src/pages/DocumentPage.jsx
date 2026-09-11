import { useCallback, useEffect, useRef, useState } from 'react';
import { deleteDocument, fetchDocument, updateDocument } from '../api/documentApi.js';

// Page 2: open + edit a single document (Google Docs editor).
export default function DocumentPage({ id, onBack }) {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [updatedAt, setUpdatedAt] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const saveTimer = useRef(null);
  const stateRef = useRef({ title, content });
  stateRef.current = { title, content };

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const doc = await fetchDocument(id);
        if (!cancelled && doc) {
          setTitle(doc.title ?? '');
          setContent(doc.content ?? '');
          setUpdatedAt(doc.updatedAt ?? null);
          setError('');
        }
      } catch {
        if (!cancelled) setError('Could not load document.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
      clearTimeout(saveTimer.current);
    };
  }, [id]);

  const scheduleSave = useCallback(
    (nextTitle, nextContent) => {
      clearTimeout(saveTimer.current);
      saveTimer.current = setTimeout(async () => {
        setSaving(true);
        try {
          const updated = await updateDocument(id, { title: nextTitle, content: nextContent });
          setUpdatedAt(updated.updatedAt ?? null);
        } catch {
          setError('Could not save changes.');
        } finally {
          setSaving(false);
        }
      }, 500);
    },
    [id]
  );

  function handleTitleChange(value) {
    setTitle(value);
    scheduleSave(value, stateRef.current.content);
  }

  function handleContentChange(value) {
    setContent(value);
    scheduleSave(stateRef.current.title, value);
  }

  async function handleDelete() {
    if (!window.confirm('Delete this document?')) return;
    try {
      await deleteDocument(id);
      onBack();
    } catch {
      setError('Could not delete document.');
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
            <span className="muted small">{saving ? 'Saving…' : 'Saved'}</span>
            <button className="delete-btn" onClick={handleDelete}>
              Delete
            </button>
          </div>
        </div>
        <textarea
          className="content-input"
          value={content}
          onChange={(e) => handleContentChange(e.target.value)}
          placeholder="Start typing…"
        />
        {updatedAt && (
          <p className="muted small">Last updated: {new Date(updatedAt).toLocaleString()}</p>
        )}
      </main>
    </div>
  );
}
