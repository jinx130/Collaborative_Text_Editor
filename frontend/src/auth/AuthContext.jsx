import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import {
  AuthError,
  fetchMe,
  getToken,
  login as apiLogin,
  logout as apiLogout,
  signup as apiSignup,
} from '../api/authApi.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    if (!getToken()) {
      setUser(null);
      return null;
    }
    try {
      const me = await fetchMe();
      setUser(me);
      return me;
    } catch (e) {
      if (e instanceof AuthError && e.status === 401) setUser(null);
      else setUser(null);
      return null;
    }
  }, []);

  useEffect(() => {
    (async () => {
      setLoading(true);
      await refresh();
      setLoading(false);
    })();
  }, [refresh]);

  const signup = useCallback(async (data) => {
    const me = await apiSignup(data);
    setUser(me);
    return me;
  }, []);

  const login = useCallback(async (data) => {
    const me = await apiLogin(data);
    setUser(me);
    return me;
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiLogout();
    } catch {
      // ignore server errors on logout
    }
    setUser(null);
  }, []);

  /** Drop local session when an API call reports 401. */
  const handleUnauthorized = useCallback(() => setUser(null), []);

  return (
    <AuthContext.Provider value={{ user, loading, signup, login, logout, refresh, handleUnauthorized }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
