import { useEffect, useState } from 'react';
import { createDocument, fetchDocuments } from '../api/documentApi.js';

// Page 1: list all documents + create a new one (Google Docs home).
export default function Dashboard({ onOpen }) {
  const [docs, setDocs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const list = await fetchDocuments();
        if (!cancelled) setDocs(Array.isArray(list) ? list : []);
      } catch {
        if (!cancelled) setError('Could not load documents.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleNew() {
    setError('');
    try {
      const doc = await createDocument('Untitled');
      setDocs((prev) => [doc, ...prev]);
      onOpen(doc.id);
    } catch {
      setError('Could not create document.');
    }
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <button className="new-btn" onClick={handleNew}>
          + New Document
        </button>
      </aside>

      <main className="editor-area">
        <h2 className="page-title">Your documents</h2>
        {error && <p className="error">{error}</p>}
        {loading ? (
          <p className="muted">Loading…</p>
        ) : docs.length === 0 ? (
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
      </main>
    </div>
  );
}
