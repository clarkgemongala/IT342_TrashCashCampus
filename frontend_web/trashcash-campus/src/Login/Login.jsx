import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';
import trashCashLogo from '../assets/trashcash-logo.png';
import recyclingVideo from '../assets/recycling-video.mp4';
import { auth, googleProvider } from '../firebase';
import { signInWithEmailAndPassword, signInWithPopup, sendPasswordResetEmail, sendEmailVerification } from 'firebase/auth';
import { login as apiLogin } from '../services/api';
import { useAuth } from '../contexts/AuthContext';

function Login() {
  const navigate = useNavigate();
  const { isBackendOnline, checkBackendStatus } = useAuth();
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
    
    try {
      setLoading(true);
      
      // Try to authenticate through our backend API
      try {
        const userData = await apiLogin(email, password);
        console.log('Login successful via API:', userData);
        
        // If the API returns a token, proceed to dashboard
        if (userData.token) {
          // Use setTimeout to ensure state updates complete before navigation
          setTimeout(() => {
            navigate('/dashboard');
          }, 100);
          return;
        }
        
        // If userData is returned but no token, it's a fallback indicator
        // Fall through to Firebase authentication
        console.log('No token received from API, falling back to Firebase');
      } catch (apiError) {
        // If it's an API error but not a backend offline error,
        // show the error message
        console.warn('API login failed:', apiError);
        if (apiError.message && !apiError.message.includes('fetch')) {
          setPasswordError(apiError.message);
          setLoading(false);
          return;
        }
      }
      
      // Fallback to direct Firebase authentication
      try {
        const userCredential = await signInWithEmailAndPassword(auth, email, password);
        const user = userCredential.user;
        
        // Check if email is verified
        if (!user.emailVerified) {
          setEmailError('Please verify your email before logging in. Check your inbox.');
          // Send verification email again
          await sendEmailVerification(user);
          setLoading(false);
          return;
        }
        
        console.log('Login successful via Firebase:', user);
        // Use setTimeout to ensure state updates complete before navigation
        setTimeout(() => {
          navigate('/dashboard');
        }, 100);
      } catch (firebaseError) {
        console.error('Firebase login error:', firebaseError);
        if (firebaseError.code === 'auth/user-not-found' || firebaseError.code === 'auth/wrong-password') {
          setPasswordError('Invalid email or password');
        } else if (firebaseError.code === 'auth/too-many-requests') {
          setPasswordError('Too many failed attempts. Please try again later.');
        } else {
          setPasswordError('An error occurred. Please try again.');
        }
        setLoading(false);
      }
    } catch (error) {
      console.error('Login error:', error);
      setPasswordError('An unexpected error occurred. Please try again.');
      setLoading(false);
    } finally {
      // Don't call setLoading(false) here as we navigate away
      // or have already set it to false in catch blocks
    }
  };

  const handleGoogleSignIn = async () => {
    try {
      setLoading(true);
      const result = await signInWithPopup(auth, googleProvider);
      
      // Check if the Google account has a cit.edu email
      const userEmail = result.user.email;
      if (!userEmail.toLowerCase().endsWith('@cit.edu')) {
        // Sign out if not a cit.edu email
        await auth.signOut();
        setEmailError('Only @cit.edu email addresses are allowed');
        setLoading(false);
        return;
      }
      
      navigate('/dashboard');
    } catch (error) {
      console.error('Google sign-in error:', error);
      setEmailError('Error signing in with Google. Please try again.');
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
            <div className="or-divider">
              <span>OR</span>
            </div>
            <button 
              type="button" 
              className="google-login-button"
              onClick={handleGoogleSignIn}
              disabled={loading}
            >
              Sign in with Google
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