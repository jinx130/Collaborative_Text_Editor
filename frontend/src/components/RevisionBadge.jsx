export default function RevisionBadge({ revision }) {
  return <span className="muted small">rev {revision ?? 0}</span>;
}
