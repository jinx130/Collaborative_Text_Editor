import { api } from './config.js';

export class AuthError extends Error {
  constructor(status, message) {
    super(message ?? `Request failed: ${status}`);
    this.status = status;
  }
}

// Per-tab login: sessionStorage belongs to ONE tab (new tabs start empty),
// so every tab can be signed in as a different account. Same-tab reloads
// keep the token; closing the tab forgets it.
const TOKEN_KEY = 'ot-jwt';

export function getToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token) {
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token);
    else sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    // storage unavailable (private mode quirks) — login lasts for this page only
  }
}

export function authHeaders(extra = {}) {
  const token = getToken();
  return token ? { ...extra, Authorization: `Bearer ${token}` } : { ...extra };
}

async function handle(res) {
  if (res.status === 401) throw new AuthError(401, 'login required');
  if (!res.ok) {
    let message = `Request failed: ${res.status}`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      // keep default
    }
    throw new AuthError(res.status, message);
  }
  if (res.status === 204) return null;
  return res.json();
}

function storeAuth(payload) {
  // Server returns { user, token }.
  if (payload?.token) setToken(payload.token);
  return payload?.user ?? null;
}

export async function signup({ email, name, password }) {
  const res = await fetch(api('/api/auth/signup'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, name, password }),
  });
  return storeAuth(await handle(res));
}

export async function login({ email, password }) {
  const res = await fetch(api('/api/auth/login'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  return storeAuth(await handle(res));
}

export async function fetchMe() {
  const res = await fetch(api('/api/auth/me'), { headers: authHeaders() });
  return handle(res);
}

export async function logout() {
  try {
    await fetch(api('/api/auth/logout'), { method: 'POST', headers: authHeaders() });
  } catch {
    // ignore server errors on logout
  }
  setToken(null);
  return null;
}
