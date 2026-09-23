import { useState } from 'react';
import { AuthError } from '../api/authApi.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function Login({ onSwitch }) {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login({ email, password });
    } catch (err) {
      if (err instanceof AuthError) setError(`${err.message} (server ${err.status})`);
      else if (err instanceof TypeError) setError('Cannot reach server. Is the backend running?');
      else setError('Could not log in.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h2 className="page-title">Log in</h2>
        {error && <p className="error">{error}</p>}
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
          Password
          <input
            className="title-input"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            autoComplete="current-password"
            required
          />
        </label>
        <button className="new-btn auth-btn" type="submit" disabled={busy}>
          {busy ? 'Logging in…' : 'Log in'}
        </button>
        <p className="muted small">
          No account?{' '}
          <button type="button" className="link-btn" onClick={onSwitch}>
            Sign up
          </button>
        </p>
      </form>
    </div>
  );
}
