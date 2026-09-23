export default function PresenceList({ users, selfId }) {
  if (!users || users.length === 0) return <span className="muted small">Only you here</span>;
  return (
    <div className="presence-list">
      {users.map((u) => (
        <span key={u.clientId} className="presence-chip" title={`cursor ${u.cursor ?? 0}`}>
          <span className="presence-dot" style={{ background: u.color ?? '#888' }} />
          {u.name ?? 'Anonymous'}
          {u.clientId === selfId ? ' (you)' : ''}
        </span>
      ))}
    </div>
  );
}
