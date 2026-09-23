import { useEffect, useState } from 'react';
import { createDocument, fetchDocuments, fetchSharedDocuments } from '../api/documentApi.js';
import { useAuth } from '../auth/AuthContext.jsx';

// Page 1: "My documents" + "Shared with me" (Google Docs home).
export default function Dashboard({ onOpen }) {
  const { user, logout, handleUnauthorized } = useAuth();
  const [docs, setDocs] = useState([]);
  const [shared, setShared] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [mine, theirs] = await Promise.all([fetchDocuments(), fetchSharedDocuments()]);
        if (!cancelled) {
          setDocs(Array.isArray(mine) ? mine : []);
          setShared(Array.isArray(theirs) ? theirs : []);
        }
      } catch (e) {
        if (e?.status === 401) handleUnauthorized();
        else if (!cancelled) setError('Could not load documents.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [handleUnauthorized]);

  async function handleNew() {
    setError('');
    try {
      const doc = await createDocument('Untitled');
      setDocs((prev) => [doc, ...prev]);
      onOpen(doc.id);
    } catch (e) {
      if (e?.status === 401) handleUnauthorized();
      else setError('Could not create document.');
    }
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <button className="new-btn" onClick={handleNew}>
          + New Document
        </button>
        <div className="side-section">
          <p className="muted small">Signed in as</p>
          <p className="user-chip">{user?.name ?? user?.email}</p>
          <button className="new-btn" onClick={logout}>
            Log out
          </button>
        </div>
      </aside>

      <main className="editor-area">
        <h2 className="page-title">My documents</h2>
        {error && <p className="error">{error}</p>}
        {loading ? (
          <p className="muted">Loading…</p>
        ) : (
          <>
            {docs.length === 0 ? (
              <div className="empty">
                <p>No documents yet. Click “+ New Document” to start one.</p>
              </div>
            ) : (
              <div className="doc-list">
                {docs.map((doc) => (
                  <button key={doc.id} className="doc-item" onClick={() => onOpen(doc.id)}>
                    {doc.title || 'Untitled'}
                  </button>
                ))}
              </div>
            )}
            <h2 className="page-title shared-heading">Shared with me</h2>
            {shared.length === 0 ? (
              <p className="muted small">Nothing shared with you yet.</p>
            ) : (
              <div className="doc-list">
                {shared.map((doc) => (
                  <button key={doc.id} className="doc-item" onClick={() => onOpen(doc.id)}>
                    {doc.title || 'Untitled'}
                    <span className="muted small">
                      {' '}
                      — {doc.ownerName ?? doc.ownerEmail} · {String(doc.myRole ?? '').toLowerCase()}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
