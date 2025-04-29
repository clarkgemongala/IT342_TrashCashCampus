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
  const mobileMenuRef = useRef(null);

  const handleSignOut = async () => {
    try {
      // Add animation effect before signout
      const button = document.querySelector('.sign-out-button');
      button.classList.add('signing-out');
      
      // Delay signout for animation
      setTimeout(async () => {
        await signOut();
        
        // Reset any app-wide styling that might be causing issues
        document.body.style.backgroundColor = '';
        document.body.style.overflow = '';
        
        // Force a reload instead of navigate to ensure clean state
        window.location.href = '/login';
      }, 300);
    } catch (error) {
      console.error('Error signing out:', error);
    }
  };

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
    // Close user dropdown when opening mobile menu
    if (!isMobileMenuOpen) {
      setIsUserDropdownOpen(false);
    }
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
      
      if (mobileMenuRef.current && !mobileMenuRef.current.contains(event.target) && 
          !event.target.classList.contains('mobile-menu-button')) {
        setIsMobileMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Add scroll effect for nav bar
  useEffect(() => {
    const handleScroll = () => {
      const navigation = document.querySelector('.navigation');
      if (window.scrollY > 10) {
        navigation.classList.add('scrolled');
      } else {
        navigation.classList.remove('scrolled');
      }
    };

    window.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('scroll', handleScroll);
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
          <Link to="/dashboard" className="logo-link">
            <img 
              src={trashCashLogo} 
              alt="TrashCash Logo" 
              className="logo-image"
            />
          </Link>
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
                <button 
                  className="user-icon-button" 
                  onClick={toggleUserDropdown}
                  aria-label="User menu"
                >
                  <FontAwesomeIcon icon={faUser} className="user-icon" />
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
          <button 
            className="mobile-menu-button" 
            onClick={toggleMobileMenu}
            aria-label={isMobileMenuOpen ? 'Close menu' : 'Open menu'}
          >
            <span className={`menu-icon ${isMobileMenuOpen ? 'open' : ''}`}>
              <span className="bar bar1"></span>
              <span className="bar bar2"></span>
              <span className="bar bar3"></span>
            </span>
          </button>
        </div>
      </div>

      {/* Mobile navigation */}
      {isMobileMenuOpen && (
        <div ref={mobileMenuRef}>
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
        </div>
      )}
    </nav>
  );
};

export default Navigation; 
