import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';
import trashCashLogo from '../assets/trashcash-logo.png';
import recyclingVideo from '../assets/recycling-video.mp4';
import { auth } from '../firebase';
import { signOut, signInWithEmailAndPassword } from 'firebase/auth';
import { useAuth } from '../contexts/AuthContext';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '../firebase';

const Login = () => {
  const navigate = useNavigate();
  const { isBackendOnline, checkBackendStatus, currentUser, signOut: contextSignOut, login, resetPassword } = useAuth();
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await signInWithEmailAndPassword(auth, email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
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
      await resetPassword(email);
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
              
              <form onSubmit={handleSubmit} className="login-form">
                {error && (
                  <div className="alert alert-error">
                    {error}
                  </div>
                )}

                <div className="form-group">
                  <label className="form-label">Email</label>
                  <input
                    type="email"
                    className="input"
                    value={email}
                    onChange={handleEmailChange}
                    required
                    placeholder="Enter your email"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Password</label>
                  <input
                    type="password"
                    className="input"
                    value={password}
                    onChange={handlePasswordChange}
                    required
                    placeholder="Enter your password"
                  />
                </div>

                <button
                  type="submit"
                  className="button button-primary"
                  disabled={loading}
                >
                  {loading ? (
                    <span className="loading-spinner" />
                  ) : (
                    'Sign In'
                  )}
                </button>
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
};

export default Login;