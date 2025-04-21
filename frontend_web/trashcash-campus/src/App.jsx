import { BrowserRouter as Router, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Login from './Login/Login';
import Dashboard from './Dashboard/Dashboard';
import User from './User/User';
import Bins from './Bins/Bins';
import Rewards from './Rewards/Rewards';
import Analytics from './Analytics/Analytics';
import './App.css';

// Protected route component
const ProtectedRoute = ({ adminOnly = false }) => {
  const { currentUser, isAdmin, loading } = useAuth();

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!currentUser) {
    return <Navigate to="/login" replace />;
  }

  if (adminOnly && !isAdmin) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
};

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      
      {/* Protected routes */}
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/bins" element={<Bins />} />
        <Route path="/rewards" element={<Rewards />} />
        <Route path="/analytics" element={<Analytics />} />
      </Route>
      
      {/* Admin-only routes */}
      <Route element={<ProtectedRoute adminOnly={true} />}>
        <Route path="/users" element={<User />} />
      </Route>
      
      {/* Redirect to login by default */}
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

function App() {
  return (
    <AuthProvider>
      <Router>
        <AppRoutes />
      </Router>
    </AuthProvider>
  );
}

export default App;