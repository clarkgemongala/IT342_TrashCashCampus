import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';
import trashCashLogo from '../assets/trashcash-logo.png';
import recyclingVideo from '../assets/recycling-video.mp4';
import { auth } from '../firebase';
import { signInWithEmailAndPassword, sendPasswordResetEmail } from 'firebase/auth';
import { login as apiLogin } from '../services/api';
import { useAuth } from '../contexts/AuthContext';

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
      setBackendError('Backend is offline. Please turn on the backend.');
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
          <p className="tagline">Let's change our earth 🌍</p>
        </div>
      </div>
      <div className="login-form-section">
        <div className="login-form-container">
          <div className="form-header">
            <img src={trashCashLogo} alt="TrashCash Campus Logo" className="form-logo" />
            <h1 className="form-title">Admin Portal</h1>
          </div>
          {backendError && (
            <div className="backend-error-message">
              {backendError}
            </div>
          )}
          <form className="login-form" onSubmit={handleLoginSubmit}>
            <div className="form-group">
              <div className="input-container">
                <input
                  type="email"
                  id="email"
                  value={email}
                  onChange={handleEmailChange}
                  onBlur={() => setEmailError(validateEmail(email))}
                  placeholder="Email"
                  required
                  disabled={loading}
                />
                <span className="input-icon">✉️</span>
              </div>
              {emailError && <div className="error-message">{emailError}</div>}
            </div>
            <div className="form-group">
              <div className="input-container">
                <input
                  type={showPassword ? "text" : "password"}
                  id="password"
                  value={password}
                  onChange={handlePasswordChange}
                  placeholder="Password"
                  required
                  disabled={loading}
                />
                <span className="input-icon">🔒</span>
                <button
                  type="button"
                  className="password-toggle"
                  onClick={togglePasswordVisibility}
                  disabled={loading}
                >
                  👁️
                </button>
              </div>
              {passwordError && <div className="error-message">{passwordError}</div>}
            </div>
            <div className="forgot-password">
              <a href="#" onClick={(e) => { e.preventDefault(); handleForgotPassword(); }}>Forgot Password?</a>
            </div>
            <button type="submit" className="login-button" disabled={loading}>
              {loading ? 'Logging in...' : 'Login'}
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
                        placeholder="Your CIT Email"
                        required
                      />
                    </div>
                    {requestEmailError && <div className="error-message">{requestEmailError}</div>}
                  </div>
                  <button type="submit" className="submit-button">Submit Request</button>
                </form>
              </div>
            ) : (
              <div className="modal-body confirmation">
                <div className="confirmation-icon">✅</div>
                <p className="confirmation-message">
                  Your request has been submitted. Please wait for confirmation from an administrator.
                  You will receive an email with your credentials once approved.
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default Login;