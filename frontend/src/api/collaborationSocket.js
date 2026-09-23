// WebSocket client + protocol (JOIN_DOCUMENT, OPERATION, LEAVE_DOCUMENT / ACK, REMOTE_OPERATION).
// The per-tab login token rides on every message, so each tab authenticates
// as its own account even in the same browser.
export function createCollaborationSocket({ url, documentId, clientId, name, token, onMessage, onStatus }) {
  let ws = null;
  let closed = false;
  let retries = 0;
  const handlers = { joined: [], ack: [], remote: [], presence: [], error: [] };

  function emit(kind, payload) {
    for (const fn of handlers[kind] ?? []) {
      try {
        fn(payload);
      } catch {
        // ignore handler errors
      }
    }
    if (onMessage) {
      try {
        onMessage(kind, payload);
      } catch {
        // ignore
      }
    }
  }

  function setStatus(status) {
    if (onStatus) {
      try {
        onStatus(status);
      } catch {
        // ignore
      }
    }
  }

  function send(obj) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(token ? { ...obj, token } : obj));
      return true;
    }
    return false;
  }

  function connect() {
    if (closed) return;
    setStatus('connecting');
    ws = new WebSocket(url);
    ws.onopen = () => {
      retries = 0;
      setStatus('connected');
      send({ type: 'JOIN_DOCUMENT', documentId, clientId, name, cursor: 0 });
    };
    ws.onmessage = (event) => {
      let msg;
      try {
        msg = JSON.parse(event.data);
      } catch {
        return;
      }
      switch (msg.type) {
        case 'JOINED':
          emit('joined', msg);
          break;
        case 'ACK':
          emit('ack', msg);
          break;
        case 'REMOTE_OPERATION':
          emit('remote', msg);
          break;
        case 'PRESENCE':
          emit('presence', msg);
          break;
        case 'ERROR':
          emit('error', msg);
          break;
        default:
          break;
      }
    };
    ws.onclose = () => {
      setStatus('disconnected');
      if (!closed) {
        retries += 1;
        const delay = Math.min(1000 * retries, 5000);
        setTimeout(connect, delay);
      }
    };
    ws.onerror = () => {
      try {
        ws.close();
      } catch {
        // ignore
      }
    };
  }

  connect();

  return {
    on(kind, fn) {
      if (handlers[kind]) handlers[kind].push(fn);
      return () => {
        handlers[kind] = (handlers[kind] ?? []).filter((f) => f !== fn);
      };
    },
    sendOperation(operation, baseRevision) {
      // Wire protocol carries operations as { components: [...] },
      // matching the backend's Operation object.
      const components = Array.isArray(operation) ? operation : (operation?.components ?? []);
      return send({ type: 'OPERATION', documentId, clientId, operation: { components }, baseRevision });
    },
    sendPresence(cursor, selectionStart, selectionEnd) {
      return send({ type: 'PRESENCE', documentId, clientId, name, cursor, selectionStart, selectionEnd });
    },
    leave() {
      try {
        send({ type: 'LEAVE_DOCUMENT', documentId, clientId });
      } catch {
        // ignore
      }
    },
    close() {
      closed = true;
      try {
        this.leave();
      } catch {
        // ignore
      }
      try {
        if (ws) ws.close();
      } catch {
        // ignore
      }
    },
  };
}

export function wsUrl(docIdHint) {
  void docIdHint;
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
  // Same-origin so Vite proxy handles it in dev; in prod serve WS from same host.
  return `${proto}://${window.location.host}/ws/collab`;
}
