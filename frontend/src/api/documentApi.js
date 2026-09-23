import { authHeaders } from './authApi.js';
import { api } from './config.js';

const BASE = api('/api/documents');

async function handle(res) {
  if (res.status === 204) return null;
  if (res.status === 401) {
    const err = new Error('login required');
    err.status = 401;
    throw err;
  }
  if (!res.ok) {
    const err = new Error(`Request failed: ${res.status}`);
    err.status = res.status;
    throw err;
  }
  return res.json();
}

export async function fetchDocuments() {
  const res = await fetch(BASE, { headers: authHeaders() });
  return handle(res);
}

export async function fetchDocument(id) {
  const res = await fetch(`${BASE}/${id}`, { headers: authHeaders() });
  return handle(res);
}

export async function createDocument(title = 'Untitled') {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ title }),
  });
  return handle(res);
}

export async function updateDocument(id, data) {
  const res = await fetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(data),
  });
  return handle(res);
}

export async function fetchSharedDocuments() {
  const res = await fetch(`${BASE}/shared`, { headers: authHeaders() });
  return handle(res);
}

export async function fetchShares(id) {
  const res = await fetch(`${BASE}/${id}/shares`, { headers: authHeaders() });
  return handle(res);
}

export async function shareDocument(id, email, role) {
  const res = await fetch(`${BASE}/${id}/shares`, {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ email, role }),
  });
  return handle(res);
}

export async function unshareDocument(id, userId) {
  const res = await fetch(`${BASE}/${id}/shares/${userId}`, { method: 'DELETE', headers: authHeaders() });
  if (res.status === 204) return null;
  return handle(res);
}

export async function deleteDocument(id) {
  const res = await fetch(`${BASE}/${id}`, { method: 'DELETE', headers: authHeaders() });
  if (res.status === 401) {
    const err = new Error('login required');
    err.status = 401;
    throw err;
  }
  if (!res.ok && res.status !== 204) {
    const err = new Error(`Request failed: ${res.status}`);
    err.status = res.status;
    throw err;
  }
}
