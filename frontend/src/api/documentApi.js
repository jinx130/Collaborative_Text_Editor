const BASE = '/api/documents';

async function handle(res) {
  if (res.status === 204) return null;
  if (!res.ok) throw new Error(`Request failed: ${res.status}`);
  return res.json();
}

export async function fetchDocuments() {
  const res = await fetch(BASE);
  return handle(res);
}

export async function fetchDocument(id) {
  const res = await fetch(`${BASE}/${id}`);
  return handle(res);
}

export async function createDocument(title = 'Untitled') {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title }),
  });
  return handle(res);
}

export async function updateDocument(id, data) {
  const res = await fetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handle(res);
}

export async function deleteDocument(id) {
  const res = await fetch(`${BASE}/${id}`, { method: 'DELETE' });
  if (!res.ok && res.status !== 204) throw new Error(`Request failed: ${res.status}`);
}
