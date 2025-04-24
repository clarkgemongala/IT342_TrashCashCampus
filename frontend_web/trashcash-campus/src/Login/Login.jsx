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
  const { isBackendOnline, checkBackendStatus, currentUser } = useAuth();
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
  
  // Redirect if already logged in
  useEffect(() => {
    if (currentUser) {
      navigate('/dashboard');
    }
  }, [currentUser, navigate]);

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
          <div className="form-header">
            <img src={trashCashLogo} alt="TrashCash Campus Logo" className="form-logo" />
            <h1 className="form-title">TrashCash Admin Portal</h1>
            <p className="form-subtitle">Login for administrators only</p>
          </div>
          {backendError && (
            <div className="backend-error-message">
              <i className="error-icon">⚠️</i>
              <span>{backendError}</span>
            </div>
          )}
          <form className="login-form" onSubmit={handleLoginSubmit}>
            <div className="form-group">
              <label htmlFor="email" className="input-label">Email Address</label>
              <div className="input-container">
                <input
                  type="email"
                  id="email"
                  value={email}
                  onChange={handleEmailChange}
                  onBlur={() => setEmailError(validateEmail(email))}
                  placeholder="yourname@cit.edu"
                  required
                  disabled={loading}
                  className={emailError ? "input-error" : ""}
                />
                <svg className="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                  <path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-.4 4.25l-7.07 4.42c-.32.2-.74.2-1.06 0L4.4 8.25c-.25-.16-.4-.43-.4-.72 0-.67.73-1.07 1.3-.72L12 11l6.7-4.19c.57-.35 1.3.05 1.3.72 0 .29-.15.56-.4.72z" fill="#666"/>
                </svg>
              </div>
              {emailError && <div className="error-message"><i className="error-icon">⚠️</i> {emailError}</div>}
            </div>
            <div className="form-group">
              <label htmlFor="password" className="input-label">Password</label>
              <div className="input-container">
                <input
                  type={showPassword ? "text" : "password"}
                  id="password"
                  value={password}
                  onChange={handlePasswordChange}
                  placeholder="Enter your password"
                  required
                  disabled={loading}
                  className={passwordError ? "input-error" : ""}
                />
                <svg className="input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                  <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM9 6c0-1.66 1.34-3 3-3s3 1.34 3 3v2H9V6zm9 14H6V10h12v10zm-6-3c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z" fill="#666"/>
                </svg>
                <button
                  type="button"
                  className="password-toggle"
                  onClick={togglePasswordVisibility}
                  disabled={loading}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                    {showPassword ? (
                      <path d="M12 6c3.79 0 7.17 2.13 8.82 5.5-.59 1.22-1.42 2.27-2.41 3.12l1.41 1.41c1.39-1.23 2.49-2.77 3.18-4.53-1.73-4.39-6-7.5-11-7.5-1.27 0-2.49.2-3.64.57l1.65 1.65C10.66 6.09 11.32 6 12 6zm-1.07 1.14L13 9.21c.57.25 1.03.71 1.28 1.28l2.07 2.07c.08-.34.14-.7.14-1.07C16.5 9.01 14.48 7 12 7c-.37 0-.72.05-1.07.14zM2.01 3.87l2.68 2.68C3.06 7.83 1.77 9.53 1 11.5 2.73 15.89 7 19 12 19c1.52 0 2.98-.29 4.32-.82l3.42 3.42 1.41-1.41L3.42 2.45 2.01 3.87zm7.5 7.5l2.61 2.61c-.04.01-.08.02-.12.02-1.38 0-2.5-1.12-2.5-2.5 0-.05.01-.08.01-.13zm-3.4-3.4l1.75 1.75c-.23.55-.36 1.15-.36 1.78 0 2.48 2.02 4.5 4.5 4.5.63 0 1.23-.13 1.77-.36l.98.98c-.88.24-1.8.38-2.75.38-3.79 0-7.17-2.13-8.82-5.5.7-1.43 1.72-2.61 2.93-3.53z" fill="#666"/>
                    ) : (
                      <path d="M12 6c-5 0-9.27 3.11-11 7.5 1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zm0 12.5c-3.79 0-7.17-2.13-8.82-5.5 1.65-3.37 5.03-5.5 8.82-5.5s7.17 2.13 8.82 5.5c-1.65 3.37-5.03 5.5-8.82 5.5zm0-9c-1.93 0-3.5 1.57-3.5 3.5s1.57 3.5 3.5 3.5 3.5-1.57 3.5-3.5-1.57-3.5-3.5-3.5zm0 5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z" fill="#666"/>
                    )}
                  </svg>
                </button>
              </div>
              {passwordError && <div className="error-message"><i className="error-icon">⚠️</i> {passwordError}</div>}
            </div>
            <div className="forgot-password">
              <a href="#" onClick={(e) => { e.preventDefault(); handleForgotPassword(); }}>Forgot Password?</a>
            </div>
            <button type="submit" className="login-button" disabled={loading}>
              {loading ? (
                <span className="loading-spinner">
                  <svg className="spinner" viewBox="0 0 50 50">
                    <circle className="path" cx="25" cy="25" r="20" fill="none" strokeWidth="5"></circle>
                  </svg>
                  <span className="loading-text">Logging in...</span>
                </span>
              ) : (
                'Login'
              )}
            </button>
            <div className="signup-link">
              <span>Not a user yet? </span>
              <a href="#" className="request-link" onClick={openRequestModal}>Request Credentials</a>
            </div>
          </form>
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