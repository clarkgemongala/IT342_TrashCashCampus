import { BrowserRouter as Router, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { NotificationProvider } from './components/Notification';
import { motion, AnimatePresence } from 'framer-motion';
import Login from './Login/Login';
import Dashboard from './Dashboard/Dashboard';
import User from './User/User';
import Bins from './Bins/Bins';
import Rewards from './Rewards/Rewards';
import AdminManagement from './AdminManagement/AdminManagement';
import './App.css';

const pageTransition = {
  initial: { opacity: 0, x: -20 },
  animate: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: 20 }
};

// Layout wrapper component
const Layout = ({ children }) => {
  return (
    <motion.div
      initial="initial"
      animate="animate"
      exit="exit"
      variants={pageTransition}
      className="min-h-screen bg-gray-50"
    >
      {children}
    </motion.div>
  );
};

// Protected route component
const ProtectedRoute = ({ adminOnly = false }) => {
  const { currentUser, isAdmin, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-green-500"></div>
      </div>
    );
  }

  if (!currentUser) {
    return <Navigate to="/login" replace />;
  }

  if (adminOnly && !isAdmin) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <Layout>
      <Outlet />
    </Layout>
  );
};

function AppRoutes() {
  return (
    <AnimatePresence mode="wait">
      <Routes>
        <Route path="/login" element={
          <Layout>
            <Login />
          </Layout>
        } />
        
        {/* Protected routes */}
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/bins" element={<Bins />} />
          <Route path="/rewards" element={<Rewards />} />
        </Route>
        
        {/* Admin-only routes */}
        <Route element={<ProtectedRoute adminOnly={true} />}>
          <Route path="/users" element={<User />} />
          <Route path="/admin-management" element={<AdminManagement />} />
        </Route>
        
        {/* Redirect to login by default */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </AnimatePresence>
  );
}

function App() {
  return (
    <AuthProvider>
      <Router>
        <NotificationProvider />
        <AppRoutes />
      </Router>
    </AuthProvider>
  );
}

export default App;
