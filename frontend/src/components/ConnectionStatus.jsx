export default function ConnectionStatus({ status, otState }) {
  const label =
    status === 'connected' ? (otState === 'synced' ? 'Live · synced' : `Live · ${otState}`) : status ?? 'offline';
  return (
    <span className={`conn conn-${status ?? 'offline'}`} title={`OT state: ${otState ?? 'unknown'}`}>
      {label}
    </span>
  );
}
