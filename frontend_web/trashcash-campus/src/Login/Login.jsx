import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';
import trashCashLogo from '../assets/trashcash-logo.png';
import recyclingVideo from '../assets/recycling-video.mp4';
import { auth } from '../firebase';
import { signOut } from 'firebase/auth';
import { useAuth } from '../contexts/AuthContext';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '../firebase';
import { useSpring, animated, config } from '@react-spring/web';

function Login() {
  const navigate = useNavigate();
  const { isBackendOnline, checkBackendStatus, currentUser, signOut: contextSignOut, login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [loading, setLoading] = useState(false);
  const [backendError, setBackendError] = useState('');
  
  // Modal states
  const [showModal, setShowModal] = useState(false);
  const [requestEmail, setRequestEmail] = useState('');
  const [requestEmailError, setRequestEmailError] = useState('');
  const [showConfirmation, setShowConfirmation] = useState(false);
  
  // Check for existing session and provide a way to sign out
  const [existingSession, setExistingSession] = useState(false);
  
  // Animation states
  const [formFocus, setFormFocus] = useState(false);
  
  // Spring animations
  const formAnimation = useSpring({
    from: { opacity: 0, transform: 'translateY(30px)' },
    to: { opacity: 1, transform: 'translateY(0)' },
    config: config.gentle,
    delay: 300
  });
  
  const titleAnimation = useSpring({
    from: { opacity: 0, transform: 'translateY(-20px)' },
    to: { opacity: 1, transform: 'translateY(0)' },
    config: config.gentle
  });
  
  const fadeIn = useSpring({
    from: { opacity: 0 },
    to: { opacity: 1 },
    config: { duration: 1000 }
  });
  
  const overlayAnimation = useSpring({
    from: { opacity: 0, transform: 'scale(0.9)' },
    to: { opacity: 1, transform: 'scale(1)' },
    config: config.gentle,
    delay: 500
  });
  
  const modalAnimation = useSpring({
    opacity: showModal ? 1 : 0,
    transform: showModal ? 'translateY(0)' : 'translateY(-40px)',
    config: config.gentle
  });
  
  const inputFocusProps = useSpring({
    boxShadow: formFocus ? '0 0 0 3px rgba(26, 83, 54, 0.3)' : '0 0 0 0px rgba(26, 83, 54, 0)',
    config: config.gentle
  });
  
  // Redirect if already logged in
  useEffect(() => {
    if (currentUser) {
      // Instead of immediately redirecting, set a flag
      setExistingSession(true);
    } else {
      setExistingSession(false);
    }
  }, [currentUser]);
  
  // Function to handle signing out from existing session
  const handleSignOut = async () => {
    setLoading(true);
    try {
      await contextSignOut();
      setExistingSession(false);
    } catch (error) {
      console.error("Error signing out:", error);
    } finally {
      setLoading(false);
    }
  };

  // Check backend status when component mounts
  useEffect(() => {
    const checkBackend = async () => {
      await checkBackendStatus();
    };
    
    checkBackend();
  }, [checkBackendStatus]);
  
  // Set backend error when status changes
  useEffect(() => {
    if (isBackendOnline === false) {
      setBackendError('Backend service is unavailable. Please try again later.');
    } else {
      setBackendError('');
    }
  }, [isBackendOnline]);

  const handleEmailChange = (e) => {
    const value = e.target.value;
    setEmail(value);
    
    // Clear error when user is typing
    if (emailError) setEmailError('');
  };

  const handlePasswordChange = (e) => {
    setPassword(e.target.value);
    if (passwordError) setPasswordError('');
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  const validateEmail = (email) => {
    // Check if email is empty
    if (!email.trim()) {
      return 'Email is required';
    }
    
    // Check if email ends with @cit.edu
    if (!email.toLowerCase().endsWith('@cit.edu')) {
      return 'Only @cit.edu email addresses are allowed';
    }
    
    // Check for valid email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return 'Please enter a valid email address';
    }
    
    return '';
  };

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    
    // Reset errors
    setEmailError('');
    setPasswordError('');
    setBackendError('');
    
    // Validate email before submission
    const errorMsg = validateEmail(email);
    if (errorMsg) {
      setEmailError(errorMsg);
      return;
    }
    
    // Validate password
    if (!password.trim()) {
      setPasswordError('Password is required');
      return;
    }
    
    setLoading(true);
    
    try {
      // Check if backend is online
      if (!isBackendOnline) {
        setBackendError('Backend service is unavailable. Please try again later.');
        setLoading(false);
        return;
      }
      
      // Use the login method from AuthContext which handles both backend and Firebase auth
      const loginResponse = await login(email, password);
      
      // If we're still executing code here, login was successful
      console.log('Login successful');
      
      // Now manually navigate to dashboard since authentication was successful
      // This is needed because Firebase Auth state change might not trigger due to our workaround
      navigate('/dashboard');
    } catch (error) {
      console.error('Login error:', error);
      
      if (error.message) {
        if (error.message.includes('Invalid credentials')) {
          setPasswordError('Invalid email or password');
        } else if (error.message.includes('Only administrators')) {
          setPasswordError('Only administrators can log in to this application');
        } else if (error.message.includes('User profile not found')) {
          setPasswordError('User profile not found. Please contact an administrator.');
        } else {
          setPasswordError(error.message);
        }
      } else if (error.code === 'auth/too-many-requests') {
        setPasswordError('Too many failed attempts. Please try again later.');
      } else if (error.code === 'auth/user-not-found' || error.code === 'auth/wrong-password') {
        setPasswordError('Invalid email or password');
      } else {
        setPasswordError('Authentication failed. Please check your credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRequestEmailChange = (e) => {
    setRequestEmail(e.target.value);
    if (requestEmailError) setRequestEmailError('');
  };

  const openRequestModal = (e) => {
    e.preventDefault();
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setRequestEmail('');
    setRequestEmailError('');
    
    // If we're showing confirmation, reset after closing
    if (showConfirmation) {
      setShowConfirmation(false);
    }
  };

  const handleRequestSubmit = (e) => {
    e.preventDefault();
    
    // Validate the request email
    const errorMsg = validateEmail(requestEmail);
    if (errorMsg) {
      setRequestEmailError(errorMsg);
      return;
    }
    
    // Show confirmation message
    setShowConfirmation(true);
    
    // Here you would typically send the request to your backend
    console.log('Credential request submitted for:', requestEmail);
    
    // Clear the form
    setRequestEmail('');
  };

  return (
    <div className="login-container">
      <div className="login-image-section">
        <video className="background-video" autoPlay loop muted>
          <source src={recyclingVideo} type="video/mp4" />
          Your browser does not support the video tag.
        </video>
        <animated.div style={fadeIn} className="video-overlay">
          <animated.div style={overlayAnimation} className="overlay-content">
            <h2 className="overlay-title">TrashCash Campus</h2>
            <p className="overlay-tagline">Let's change our earth together</p>
            <div className="overlay-description">
              <p>A sustainable recycling initiative to help our campus reduce waste and promote environmental awareness.</p>
            </div>
          </animated.div>
        </animated.div>
      </div>
      <animated.div style={formAnimation} className="login-form-section">
        <div className="login-form-container">
          <animated.img style={titleAnimation} src={trashCashLogo} alt="TrashCash Logo" className="login-logo" />
          <animated.h2 style={titleAnimation} className="login-title">Admin Login</animated.h2>
          
          {/* Display session notice if already logged in */}
          {existingSession && (
            <animated.div style={fadeIn} className="session-notice">
              <p>You are already logged in.</p>
              <div className="session-actions">
                <animated.button 
                  className="continue-button glow-button" 
                  onClick={() => navigate('/dashboard')}
                  disabled={loading}
                  whileHover={{ scale: 1.05 }}
                >
                  Continue to Dashboard
                </animated.button>
                <animated.button 
                  className="signout-button pulse-button" 
                  onClick={handleSignOut}
                  disabled={loading}
                >
                  Sign Out
                </animated.button>
              </div>
            </animated.div>
          )}
          
          {!existingSession && (
            <>
              {backendError && <animated.div style={fadeIn} className="auth-error">{backendError}</animated.div>}
              
              <animated.form onSubmit={handleLoginSubmit} className="login-form">
                <div className="form-group">
                  <label htmlFor="email">Email</label>
                  <animated.div 
                    className="input-wrapper"
                    style={inputFocusProps}
                  >
                    <input
                      type="email"
                      id="email"
                      value={email}
                      onChange={handleEmailChange}
                      placeholder="Enter your email"
                      className={emailError ? 'input-error' : ''}
                      onFocus={() => setFormFocus(true)}
                      onBlur={() => setFormFocus(false)}
                    />
                  </animated.div>
                  {emailError && 
                    <animated.div 
                      className="error-message"
                      style={useSpring({
                        from: { opacity: 0, height: 0 },
                        to: { opacity: 1, height: 20 },
                        config: config.gentle
                      })}
                    >
                      {emailError}
                    </animated.div>
                  }
                </div>
                
                <div className="form-group">
                  <label htmlFor="password">Password</label>
                  <animated.div 
                    className="password-input-wrapper"
                    style={inputFocusProps}
                  >
                    <input
                      type={showPassword ? 'text' : 'password'}
                      id="password"
                      value={password}
                      onChange={handlePasswordChange}
                      placeholder="Enter your password"
                      className={passwordError ? 'input-error' : ''}
                      onFocus={() => setFormFocus(true)}
                      onBlur={() => setFormFocus(false)}
                    />
                    <button
                      type="button"
                      className="toggle-password"
                      onClick={togglePasswordVisibility}
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                    >
                      {showPassword ? (
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#666666" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                          <line x1="1" y1="1" x2="23" y2="23" />
                        </svg>
                      ) : (
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#666666" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                          <circle cx="12" cy="12" r="3" />
                        </svg>
                      )}
                    </button>
                  </animated.div>
                  {passwordError && 
                    <animated.div 
                      className="error-message"
                      style={useSpring({
                        from: { opacity: 0, height: 0 },
                        to: { opacity: 1, height: 20 },
                        config: config.gentle
                      })}
                    >
                      {passwordError}
                    </animated.div>
                  }
                </div>
                
                <animated.button
                  type="submit"
                  className="login-button"
                  disabled={loading}
                  style={useSpring({
                    scale: loading ? 0.95 : 1,
                    config: config.gentle
                  })}
                >
                  {loading ? (
                    <div className="spinner-container">
                      <div className="spinner"></div>
                      <span>Logging in...</span>
                    </div>
                  ) : 'Login'}
                </animated.button>
              </animated.form>
              
              <animated.div 
                className="credential-request"
                style={useSpring({
                  from: { opacity: 0, transform: 'translateY(20px)' },
                  to: { opacity: 1, transform: 'translateY(0)' },
                  delay: 600,
                  config: config.gentle
                })}
              >
                <p>Don't have credentials yet?</p>
                <button 
                  className="request-button shine-button"
                  onClick={openRequestModal}
                >
                  Request Access
                </button>
              </animated.div>
            </>
          )}
        </div>
      </animated.div>

      {/* Request Credentials Modal */}
      {showModal && (
        <div className="modal-overlay">
          <animated.div 
            style={modalAnimation} 
            className="modal-content"
          >
            <div className="modal-header">
              <h2>{showConfirmation ? "Request Submitted" : "Request Credentials"}</h2>
              <button className="close-button" onClick={closeModal}>×</button>
            </div>
            
            {!showConfirmation ? (
              <div className="modal-body">
                <p className="modal-instruction">Please enter your @cit.edu email address to request access credentials:</p>
                <form onSubmit={handleRequestSubmit}>
                  <div className="form-group">
                    <div className="input-container">
                      <input
                        type="email"
                        value={requestEmail}
                        onChange={handleRequestEmailChange}
                        placeholder="yourname@cit.edu"
                        required
                        className={requestEmailError ? "input-error" : ""}
                      />
                      <div className="input-icon">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#1a5336" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                          <polyline points="22,6 12,13 2,6"></polyline>
                        </svg>
                      </div>
                    </div>
                    {requestEmailError && 
                      <animated.div 
                        className="error-message"
                        style={useSpring({
                          from: { opacity: 0, height: 0 },
                          to: { opacity: 1, height: 20 },
                          config: config.gentle
                        })}
                      >
                        <i className="error-icon">⚠️</i> {requestEmailError}
                      </animated.div>
                    }
                  </div>
                  <animated.button 
                    type="submit" 
                    className="submit-button"
                    style={useSpring({
                      scale: loading ? 0.95 : 1,
                      config: { tension: 300, friction: 10 }
                    })}
                  >
                    Submit Request
                  </animated.button>
                </form>
              </div>
            ) : (
              <animated.div 
                className="modal-body confirmation"
                style={useSpring({
                  from: { opacity: 0, transform: 'scale(0.8)' },
                  to: { opacity: 1, transform: 'scale(1)' },
                  config: { mass: 1, tension: 180, friction: 12 }
                })}
              >
                <div className="confirmation-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="64" height="64">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="#4caf50"/>
                  </svg>
                </div>
                <p className="confirmation-message">
                  Your request has been submitted. Please wait for confirmation from an administrator.
                  You will receive an email with your credentials once approved.
                </p>
                <animated.button 
                  className="close-button-centered"
                  onClick={closeModal}
                  style={useSpring({
                    scale: loading ? 0.95 : 1,
                    config: { tension: 300, friction: 10 }
                  })}
                >
                  Close
                </animated.button>
              </animated.div>
            )}
          </animated.div>
        </div>
      )}
    </div>
  );
}

export default Login;