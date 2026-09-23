// Wave-style OT mirror of the Java backend: retain/insert/delete components.
// Blog mapping: Insert(pos,text) => [retain(pos), insert(text), retain(rest)].

export function retain(count) {
  return { type: 'RETAIN', count };
}

export function insert(text) {
  const t = text ?? '';
  return { type: 'INSERT', count: t.length, text: t };
}

export function del(count) {
  return { type: 'DELETE', count };
}

/** Accept both wire shapes: bare component array or { components: [...] } object. */
export function asComponents(op) {
  if (Array.isArray(op)) return op;
  if (op && Array.isArray(op.components)) return op.components;
  return [];
}

export function normalizeType(c) {
  if (!c) return null;
  const t = String(c.type ?? '').toUpperCase();
  if (t === 'RETAIN' || t === 'INSERT' || t === 'DELETE') return t;
  return null;
}

export function compact(components) {
  const out = [];
  for (const raw of asComponents(components)) {
    const type = normalizeType(raw);
    if (!type) continue;
    if (type === 'INSERT') {
      const text = raw.text ?? '';
      if (!text) continue;
      const last = out[out.length - 1];
      if (last && last.type === 'INSERT') {
        last.text += text;
        last.count = last.text.length;
      } else out.push({ type: 'INSERT', text, count: text.length });
    } else {
      const count = raw.count ?? 0;
      if (count <= 0) continue;
      const last = out[out.length - 1];
      if (last && last.type === type) last.count += count;
      else out.push({ type, count });
    }
  }
  return out;
}

export function baseLength(op) {
  return asComponents(op).reduce((n, c) => {
    const t = normalizeType(c);
    if (t === 'RETAIN' || t === 'DELETE') return n + (c.count ?? 0);
    return n;
  }, 0);
}

export function isNoop(op) {
  return asComponents(op).every((c) => normalizeType(c) === 'RETAIN');
}

export function validate(op, expectedBase) {
  const comps = asComponents(op);
  if (baseLength(comps) !== expectedBase) {
    throw new Error(`baseLength ${baseLength(comps)} != ${expectedBase}`);
  }
  for (const c of comps) {
    const t = normalizeType(c);
    if (t === 'INSERT' && !(c.text ?? '')) throw new Error('insert text must be non-empty');
    if ((t === 'RETAIN' || t === 'DELETE') && (c.count ?? 0) <= 0) throw new Error(`${t} count must be > 0`);
  }
}

export function apply(op, doc) {
  const source = doc ?? '';
  if (baseLength(op) !== source.length) {
    throw new Error(`operation baseLength ${baseLength(op)} does not match doc length ${source.length}`);
  }
  let out = '';
  let i = 0;
  for (const raw of compact(asComponents(op))) {
    const t = normalizeType(raw);
    if (t === 'RETAIN') {
      out += source.slice(i, i + raw.count);
      i += raw.count;
    } else if (t === 'INSERT') {
      out += raw.text;
    } else if (t === 'DELETE') {
      i += raw.count;
    }
  }
  if (i !== source.length) throw new Error('operation did not consume entire document');
  return out;
}

/**
 * Pair transform (a,b) -> [a', b'] with a-priority on insert/insert,
 * same algorithm as OperationTransformer.java. Satisfies TP1.
 */
export function transform(a, b) {
  const qa = asComponents(a).flatMap((c) => {
    const t = normalizeType(c);
    if (t === 'INSERT') return c.text ? [{ type: 'INSERT', text: c.text }] : [];
    if (t && (c.count ?? 0) > 0) return [{ type: t, count: c.count }];
    return [];
  });
  const qb = asComponents(b).flatMap((c) => {
    const t = normalizeType(c);
    if (t === 'INSERT') return c.text ? [{ type: 'INSERT', text: c.text }] : [];
    if (t && (c.count ?? 0) > 0) return [{ type: t, count: c.count }];
    return [];
  });
  const aPrime = [];
  const bPrime = [];
  let ia = 0;
  let ib = 0;
  let ca = qa[0] ? { ...qa[0] } : null;
  let cb = qb[0] ? { ...qb[0] } : null;
  const next = (q, i) => (i + 1 < q.length ? { ...q[i + 1] } : null);

  while (ca || cb) {
    if (ca && ca.type === 'INSERT') {
      aPrime.push({ type: 'INSERT', text: ca.text });
      bPrime.push({ type: 'RETAIN', count: ca.text.length });
      ia += 1;
      ca = next(qa, ia - 1) && ia < qa.length ? { ...qa[ia] } : null;
      continue;
    }
    if (cb && cb.type === 'INSERT') {
      bPrime.push({ type: 'INSERT', text: cb.text });
      aPrime.push({ type: 'RETAIN', count: cb.text.length });
      ib += 1;
      cb = ib < qb.length ? { ...qb[ib] } : null;
      continue;
    }
    if (!ca || !cb) {
      const rest = ca ?? cb;
      const target = ca ? [aPrime, bPrime] : [aPrime, bPrime];
      if (rest.type !== 'RETAIN') throw new Error('operation length mismatch during transform');
      target[0].push({ type: 'RETAIN', count: rest.count });
      target[1].push({ type: 'RETAIN', count: rest.count });
      if (ca) {
        ia += 1;
        ca = ia < qa.length ? { ...qa[ia] } : null;
      } else {
        ib += 1;
        cb = ib < qb.length ? { ...qb[ib] } : null;
      }
      continue;
    }
    const min = Math.min(ca.count, cb.count);
    const aRet = ca.type === 'RETAIN';
    const bRet = cb.type === 'RETAIN';
    if (aRet && bRet) {
      aPrime.push({ type: 'RETAIN', count: min });
      bPrime.push({ type: 'RETAIN', count: min });
    } else if (!aRet && !bRet) {
      // delete vs delete: drop
    } else if (aRet) {
      bPrime.push({ type: 'DELETE', count: min });
    } else {
      aPrime.push({ type: 'DELETE', count: min });
    }
    ca.count -= min;
    cb.count -= min;
    if (ca.count === 0) {
      ia += 1;
      ca = ia < qa.length ? { ...qa[ia] } : null;
    }
    if (cb.count === 0) {
      ib += 1;
      cb = ib < qb.length ? { ...qb[ib] } : null;
    }
  }
  return [compact(aPrime), compact(bPrime)];
}

export function transformAgainst(prior, incoming) {
  return transform(prior, incoming)[1];
}

/** Diff old->next into a Wave op (common prefix/suffix). */
export function diffToOperation(oldText, nextText) {
  const a = oldText ?? '';
  const b = nextText ?? '';
  if (a === b) return [];
  let prefix = 0;
  while (prefix < a.length && prefix < b.length && a[prefix] === b[prefix]) prefix += 1;
  let suffix = 0;
  while (
    suffix < a.length - prefix &&
    suffix < b.length - prefix &&
    a[a.length - 1 - suffix] === b[b.length - 1 - suffix]
  ) {
    suffix += 1;
  }
  const op = [];
  if (prefix > 0) op.push({ type: 'RETAIN', count: prefix });
  const delLen = a.length - prefix - suffix;
  const insText = b.slice(prefix, b.length - suffix);
  if (delLen > 0) op.push({ type: 'DELETE', count: delLen });
  if (insText) op.push({ type: 'INSERT', text: insText });
  if (suffix > 0) op.push({ type: 'RETAIN', count: suffix });
  return compact(op);
}

/** Blog's transformCursor(). */
export function transformCursor(cursor, op) {
  let pos = cursor;
  let index = 0;
  for (const raw of asComponents(op)) {
    const t = normalizeType(raw);
    if (t === 'RETAIN') index += raw.count;
    else if (t === 'INSERT') {
      if (index <= pos) pos += (raw.text ?? '').length;
      index += 0;
    } else if (t === 'DELETE') {
      if (index < pos) {
        const end = index + raw.count;
        if (end <= pos) pos -= raw.count;
        else pos = index;
      }
      index += raw.count;
    }
  }
  return Math.max(0, pos);
}

/** Blog's invertOperation() for undo (caller must supply deleted text). */
export function invertOperation(op, baseDoc) {
  const inv = [];
  let index = 0;
  for (const raw of asComponents(op)) {
    const t = normalizeType(raw);
    if (t === 'RETAIN') {
      inv.push({ type: 'RETAIN', count: raw.count });
      index += raw.count;
    } else if (t === 'INSERT') {
      inv.push({ type: 'DELETE', count: (raw.text ?? '').length });
    } else if (t === 'DELETE') {
      inv.push({ type: 'INSERT', text: (baseDoc ?? '').slice(index, index + raw.count) });
      index += raw.count;
    }
  }
  return compact(inv);
}

export function colorFor(clientId) {
  let hash = 0;
  for (const ch of clientId ?? '') hash = ((hash << 5) - hash + ch.charCodeAt(0)) | 0;
  return `hsl(${Math.abs(hash) % 360}, 70%, 50%)`;
}
