import { useEffect, useState } from 'react';
import { fetchShares, shareDocument, unshareDocument } from '../api/documentApi.js';

// Owner-managed sharing, Google-Docs style: invite by email, Viewer/Editor.
export default function ShareDialog({ docId, isOwner, onClose }) {
  const [shares, setShares] = useState([]);
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('EDITOR');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const list = await fetchShares(docId);
        if (!cancelled) setShares(Array.isArray(list) ? list : []);
      } catch (e) {
        if (!cancelled) setError(e?.status === 403 ? 'Only the owner can manage sharing.' : 'Could not load sharing list.');
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [docId]);

  async function handleShare(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const entry = await shareDocument(docId, email.trim(), role);
      setShares((prev) => {
        const rest = prev.filter((s) => s.userId !== entry.userId);
        return [...rest, entry];
      });
      setEmail('');
    } catch (err) {
      if (err?.status === 404) setError('No account with that email.');
      else if (err?.status === 403) setError('Only the owner can share.');
      else if (err?.status === 400) setError('Enter an email and pick Viewer or Editor.');
      else setError('Could not share.');
    } finally {
      setBusy(false);
    }
  }

  async function handleUnshare(userId) {
    setError('');
    try {
      await unshareDocument(docId, userId);
      setShares((prev) => prev.filter((s) => s.userId !== userId));
    } catch {
      setError('Could not remove access.');
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3 className="page-title">Share this document</h3>
        {error && <p className="error">{error}</p>}
        {isOwner ? (
          <form onSubmit={handleShare} className="share-form">
            <input
              className="title-input"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="teammate@example.com"
              required
            />
            <select className="role-select" value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="EDITOR">Editor</option>
              <option value="VIEWER">Viewer</option>
            </select>
            <button className="new-btn" type="submit" disabled={busy}>
              {busy ? 'Sharing…' : 'Share'}
            </button>
          </form>
        ) : (
          <p className="muted small">Only the owner can share this document.</p>
        )}
        <div className="share-list">
          {shares.length === 0 ? (
            <p className="muted small">Only you have access.</p>
          ) : (
            shares.map((s) => (
              <div key={s.userId} className="share-row">
                <span>
                  {s.name} <span className="muted small">{s.email}</span>
                </span>
                <span className="muted small">{s.role}</span>
                {isOwner && (
                  <button className="delete-btn small-btn" onClick={() => handleUnshare(s.userId)}>
                    Remove
                  </button>
                )}
              </div>
            ))
          )}
        </div>
        <button className="new-btn" onClick={onClose}>
          Done
        </button>
      </div>
    </div>
  );
}
