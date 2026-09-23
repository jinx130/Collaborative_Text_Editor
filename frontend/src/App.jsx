import { useState } from 'react';
import Dashboard from './pages/Dashboard.jsx';
import DocumentPage from './pages/DocumentPage.jsx';
import Login from './pages/Login.jsx';
import Signup from './pages/Signup.jsx';
import { AuthProvider, useAuth } from './auth/AuthContext.jsx';
import './styles.css';

// 4 states: login / signup / home (list) / editor (one document).
function Shell() {
  const { user, loading } = useAuth();
  const [selectedId, setSelectedId] = useState(null);
  const [mode, setMode] = useState('login'); // 'login' | 'signup'

  if (loading) {
    return (
      <div className="layout">
        <main className="editor-area">
          <p className="muted">Loading…</p>
        </main>
      </div>
    );
  }

  if (!user) {
    return mode === 'login' ? (
      <Login onSwitch={() => setMode('signup')} />
    ) : (
      <Signup onSwitch={() => setMode('login')} />
    );
  }

  if (selectedId) {
    return <DocumentPage id={selectedId} onBack={() => setSelectedId(null)} />;
  }

  return <Dashboard onOpen={(id) => setSelectedId(id)} />;
}

export default function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  );
}
