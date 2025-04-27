import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Navigation.css';
import trashCashLogo from '../assets/trashcash-logo.png';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { 
  faHome, 
  faAward, 
  faTrashAlt, 
  faUsers, 
  faChartBar, 
  faUserShield,
  faUser,
  faSignOutAlt
} from '@fortawesome/free-solid-svg-icons';

const Navigation = () => {
  const { currentUser, signOut, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isUserDropdownOpen, setIsUserDropdownOpen] = useState(false);
  const userDropdownRef = useRef(null);

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

  const toggleUserDropdown = () => {
    setIsUserDropdownOpen(!isUserDropdownOpen);
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (userDropdownRef.current && !userDropdownRef.current.contains(event.target)) {
        setIsUserDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Define navigation links with Font Awesome icons
  const navLinks = [
    { path: '/dashboard', label: 'Dashboard', icon: <FontAwesomeIcon icon={faHome} />, access: 'all' },
    { path: '/rewards', label: 'Rewards', icon: <FontAwesomeIcon icon={faAward} />, access: 'all' },
    { path: '/bins', label: 'QR Bins', icon: <FontAwesomeIcon icon={faTrashAlt} />, access: 'all' },
    { path: '/users', label: 'Users', icon: <FontAwesomeIcon icon={faUsers} />, access: 'admin' },
    
    { path: '/admin-management', label: 'Admin Management', icon: <FontAwesomeIcon icon={faUserShield} />, access: 'admin' },
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

        <div className="nav-right">
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

          {/* User section with dropdown */}
          <div className="user-section" ref={userDropdownRef}>
            {currentUser && (
              <>
                <button className="user-icon-button" onClick={toggleUserDropdown}>
                  <FontAwesomeIcon icon={faUser} />
                </button>
                {isUserDropdownOpen && (
                  <div className="user-dropdown">
                    <div className="user-email">
                      {currentUser.displayName || currentUser.email}
                    </div>
                    <button className="sign-out-button" onClick={handleSignOut}>
                      <FontAwesomeIcon icon={faSignOutAlt} className="sign-out-icon" />
                      <span>Sign Out</span>
                    </button>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Mobile menu button */}
          <button className="mobile-menu-button" onClick={toggleMobileMenu}>
            {isMobileMenuOpen ? '✕' : '☰'}
          </button>
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
                  <FontAwesomeIcon icon={faSignOutAlt} className="sign-out-icon" />
                  <span>Sign Out</span>
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
