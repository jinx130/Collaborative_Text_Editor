// Centralizes the backend location for split deploy (Vercel + Render).
// Dev: leave VITE_API_URL unset -> relative '/api' goes through the Vite proxy.
// Prod (Vercel): set VITE_API_URL=https://<your-backend>.onrender.com
//   and optionally VITE_WS_URL=wss://<your-backend>.onrender.com/ws/collab
export const API_BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');
export const WS_URL = import.meta.env.VITE_WS_URL ?? '';

export function api(path) {
  const suffix = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE}${suffix}`;
}
