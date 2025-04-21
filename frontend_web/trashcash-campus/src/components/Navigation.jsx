import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Navigation.css';
import trashCashLogo from '../assets/trashcash-logo.png';

const Navigation = () => {
  const { currentUser, signOut, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const handleSignOut = async () => {
    try {
      await signOut();
      navigate('/login');
    } catch (error) {
      console.error('Error signing out:', error);
    }
  };

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
  };

  // Define navigation links
  const navLinks = [
    { path: '/dashboard', label: 'Dashboard', icon: '📊', access: 'all' },
    { path: '/rewards', label: 'Rewards', icon: '🏆', access: 'all' },
    { path: '/bins', label: 'Find Bins', icon: '🗑️', access: 'all' },
    { path: '/users', label: 'Users', icon: '👥', access: 'admin' },
    { path: '/analytics', label: 'Analytics', icon: '📈', access: 'admin' },
  ];

  // Filter links based on user role
  const filteredLinks = navLinks.filter(link => 
    link.access === 'all' || (link.access === 'admin' && isAdmin)
  );

  return (
    <nav className="navigation">
      <div className="nav-container">
        <div className="nav-brand">
          <img src={trashCashLogo} alt="TrashCash Logo" />
          <h1>TrashCash Campus</h1>
        </div>

        {/* Mobile menu button */}
        <button className="mobile-menu-button" onClick={toggleMobileMenu}>
          {isMobileMenuOpen ? '✕' : '☰'}
        </button>

        {/* Desktop navigation */}
        <ul className="nav-links desktop-nav">
          {filteredLinks.map((link) => (
            <li key={link.path} className={location.pathname === link.path ? 'active' : ''}>
              <Link to={link.path}>
                <span className="nav-icon">{link.icon}</span>
                <span className="nav-label">{link.label}</span>
              </Link>
            </li>
          ))}
        </ul>

        {/* User section */}
        <div className="user-section">
          {currentUser && (
            <div className="user-info">
              <span className="user-name">
                {currentUser.displayName || currentUser.email}
              </span>
              <button className="sign-out-button" onClick={handleSignOut}>
                Sign Out
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Mobile navigation */}
      {isMobileMenuOpen && (
        <ul className="nav-links mobile-nav">
          {filteredLinks.map((link) => (
            <li 
              key={link.path} 
              className={location.pathname === link.path ? 'active' : ''}
              onClick={() => setIsMobileMenuOpen(false)}
            >
              <Link to={link.path}>
                <span className="nav-icon">{link.icon}</span>
                <span className="nav-label">{link.label}</span>
              </Link>
            </li>
          ))}
          <li className="mobile-user-section">
            {currentUser && (
              <>
                <div className="user-name">
                  {currentUser.displayName || currentUser.email}
                </div>
                <button className="sign-out-button" onClick={handleSignOut}>
                  Sign Out
                </button>
              </>
            )}
          </li>
        </ul>
      )}
    </nav>
  );
};

export default Navigation; 