import { useState } from 'react';
import Dashboard from './pages/Dashboard.jsx';
import DocumentPage from './pages/DocumentPage.jsx';
import './styles.css';

// 2 pages only: home (new + list) and editor (edit one document).
export default function App() {
  const [selectedId, setSelectedId] = useState(null);

  if (selectedId) {
    return <DocumentPage id={selectedId} onBack={() => setSelectedId(null)} />;
  }

  return <Dashboard onOpen={(id) => setSelectedId(id)} />;
}
