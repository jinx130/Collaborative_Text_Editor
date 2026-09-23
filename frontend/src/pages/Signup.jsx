import { useState } from 'react';
import { AuthError } from '../api/authApi.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function Signup({ onSwitch }) {
  const { signup } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await signup({ email, name, password });
    } catch (err) {
      if (err instanceof AuthError) setError(`${err.message} (server ${err.status})`);
      else if (err instanceof TypeError) setError('Cannot reach server. Is the backend running?');
      else setError('Could not sign up.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h2 className="page-title">Sign up</h2>
        {error && <p className="error">{error}</p>}
        <label className="auth-label">
          Display name
          <input
            className="title-input"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Ada Lovelace"
            autoComplete="name"
            required
          />
        </label>
        <label className="auth-label">
          Email
          <input
            className="title-input"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            autoComplete="email"
            required
          />
        </label>
        <label className="auth-label">
          Password (min 6 characters)
          <input
            className="title-input"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            autoComplete="new-password"
            required
          />
        </label>
        <button className="new-btn auth-btn" type="submit" disabled={busy}>
          {busy ? 'Creating account…' : 'Sign up'}
        </button>
        <p className="muted small">
          Have an account?{' '}
          <button type="button" className="link-btn" onClick={onSwitch}>
            Log in
          </button>
        </p>
      </form>
    </div>
  );
}
