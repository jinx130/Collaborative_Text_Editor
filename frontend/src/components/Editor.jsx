// textarea + diff->operation: emits Wave ops on every keystroke for the OT state machine.
import { useEffect, useRef } from 'react';
import { diffToOperation } from '../ot/operations.js';

export default function Editor({ value, onLocalEdit, disabled }) {
  const prevRef = useRef(value ?? '');
  const areaRef = useRef(null);

  // Keep prev in sync when remote ops change value from outside.
  useEffect(() => {
    prevRef.current = value ?? '';
  }, [value]);

  function handleChange(e) {
    const next = e.target.value;
    const prev = prevRef.current ?? '';
    const op = diffToOperation(prev, next);
    prevRef.current = next;
    if (onLocalEdit) onLocalEdit(next, op, e.target.selectionStart ?? next.length);
  }

  return (
    <textarea
      ref={areaRef}
      className="content-input"
      value={value ?? ''}
      onChange={handleChange}
      placeholder="Start typing… (live-collaborative)"
      disabled={disabled}
    />
  );
}
