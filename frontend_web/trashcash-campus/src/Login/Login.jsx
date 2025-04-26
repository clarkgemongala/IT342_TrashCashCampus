import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';
import trashCashLogo from '../assets/trashcash-logo.png';
import recyclingVideo from '../assets/recycling-video.mp4';
import { auth } from '../firebase';
import { signInWithEmailAndPassword, sendPasswordResetEmail, signOut } from 'firebase/auth';
import { login as apiLogin } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '../firebase';

function Login() {
  const navigate = useNavigate();
  const { isBackendOnline, checkBackendStatus, currentUser, signOut: contextSignOut } = useAuth();
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
      
      // Use Firebase authentication
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      console.log('Firebase auth successful:', userCredential.user);
      
      // Get user data from Firestore to check role
      const userDocRef = doc(db, "users", userCredential.user.uid);
      const userSnap = await getDoc(userDocRef);
      
      // Check if user is admin
      if (userSnap.exists()) {
        const userData = userSnap.data();
        if (userData.role !== 'admin') {
          // Show notification for non-admin users and sign them out
          alert('Only administrators can log in to this application');
          await signOut(auth);
          setLoading(false);
          return;
        }
      } else {
        // If user document doesn't exist in Firestore, sign them out
        alert('User profile not found. Please contact an administrator.');
        await signOut(auth);
        setLoading(false);
        return;
      }
      
      // API login (no need to wait for it)
      apiLogin(email, password)
        .then(userData => {
          console.log('API login successful:', userData);
        })
        .catch(error => {
          console.warn('API login failed:', error);
        });
      
      // navigation will happen through the useEffect hook that watches currentUser
    } catch (error) {
      console.error('Login error:', error);
      
      if (error.code === 'auth/user-not-found' || error.code === 'auth/wrong-password') {
        setPasswordError('Invalid email or password');
      } else if (error.code === 'auth/too-many-requests') {
        setPasswordError('Too many failed attempts. Please try again later.');
      } else {
        setPasswordError('Authentication failed. Please check your credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    // Validate email
    const errorMsg = validateEmail(email);
    if (errorMsg) {
      setEmailError(errorMsg);
      return;
    }
    
    try {
      setLoading(true);
      await sendPasswordResetEmail(auth, email);
      alert('Password reset email sent. Check your inbox.');
    } catch (error) {
      console.error('Password reset error:', error);
      setEmailError('Error sending password reset email. Please try again.');
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
        <div className="video-overlay">
          <div className="overlay-content">
            <h2 className="overlay-title">TrashCash Campus</h2>
            <p className="overlay-tagline">Let's change our earth together</p>
            <div className="overlay-description">
              <p>A sustainable recycling initiative to help our campus reduce waste and promote environmental awareness.</p>
            </div>
          </div>
        </div>
      </div>
      <div className="login-form-section">
        <div className="login-form-container">
          <img src={trashCashLogo} alt="TrashCash Logo" className="login-logo" />
          <h2 className="login-title">Admin Login</h2>
          
          {/* Display session notice if already logged in */}
          {existingSession && (
            <div className="session-notice">
              <p>You are already logged in.</p>
              <div className="session-actions">
                <button 
                  className="continue-button" 
                  onClick={() => navigate('/dashboard')}
                  disabled={loading}
                >
                  Continue to Dashboard
                </button>
                <button 
                  className="signout-button" 
                  onClick={handleSignOut}
                  disabled={loading}
                >
                  Sign Out
                </button>
              </div>
            </div>
          )}
          
          {!existingSession && (
            <>
              {backendError && <div className="auth-error">{backendError}</div>}
              
              <form onSubmit={handleLoginSubmit} className="login-form">
                <div className="form-group">
                  <label htmlFor="email">Email</label>
                  <input
                    type="email"
                    id="email"
                    value={email}
                    onChange={handleEmailChange}
                    placeholder="Enter your email"
                    className={emailError ? 'input-error' : ''}
                  />
                  {emailError && <div className="error-message">{emailError}</div>}
                </div>
                
                <div className="form-group">
                  <label htmlFor="password">Password</label>
                  <div className="password-input-wrapper">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      id="password"
                      value={password}
                      onChange={handlePasswordChange}
                      placeholder="Enter your password"
                      className={passwordError ? 'input-error' : ''}
                    />
                    <button
                      type="button"
                      className="toggle-password"
                      onClick={togglePasswordVisibility}
                    >
                      {showPassword ? '🙈' : '👁️'}
                    </button>
                  </div>
                  {passwordError && <div className="error-message">{passwordError}</div>}
                </div>
                
                <button
                  type="submit"
                  className="login-button"
                  disabled={loading}
                >
                  {loading ? 'Logging in...' : 'Login'}
                </button>
                
                <div className="additional-options">
                  <button
                    type="button"
                    className="forgot-password"
                    onClick={handleForgotPassword}
                    disabled={loading}
                  >
                    Forgot Password?
                  </button>
                </div>
              </form>
              
              <div className="credential-request">
                <p>Don't have credentials yet?</p>
                <button 
                  className="request-button"
                  onClick={openRequestModal}
                >
                  Request Access
                </button>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Request Credentials Modal */}
      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content">
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
                      <svg className="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                        <path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-.4 4.25l-7.07 4.42c-.32.2-.74.2-1.06 0L4.4 8.25c-.25-.16-.4-.43-.4-.72 0-.67.73-1.07 1.3-.72L12 11l6.7-4.19c.57-.35 1.3.05 1.3.72 0 .29-.15.56-.4.72z" fill="#666"/>
                      </svg>
                    </div>
                    {requestEmailError && <div className="error-message"><i className="error-icon">⚠️</i> {requestEmailError}</div>}
                  </div>
                  <button type="submit" className="submit-button">Submit Request</button>
                </form>
              </div>
            ) : (
              <div className="modal-body confirmation">
                <div className="confirmation-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="64" height="64">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="#4caf50"/>
                  </svg>
                </div>
                <p className="confirmation-message">
                  Your request has been submitted. Please wait for confirmation from an administrator.
                  You will receive an email with your credentials once approved.
                </p>
                <button className="close-button-centered" onClick={closeModal}>Close</button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default Login;