import React, { createContext, useContext, useState, useEffect } from 'react';
import { auth, db } from '../firebase';
import { 
  onAuthStateChanged, 
  signOut as firebaseSignOut,
  signInWithEmailAndPassword,
  sendPasswordResetEmail
} from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { ensureUserDocument, seedDatabase } from '../seedData';
import { pingBackend, login as apiLogin } from '../services/api';

// Create context
const AuthContext = createContext();

// Context provider component
export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [userRole, setUserRole] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isBackendOnline, setIsBackendOnline] = useState(null);

  // Login function that uses the backend API
  const login = async (email, password) => {
    try {
      // First authenticate with backend
      const apiResponse = await apiLogin(email, password);
      console.log("API login successful:", apiResponse);
      
      // If backend authentication is successful
      if (apiResponse && apiResponse.userId) {
        // Check if the user is an admin before proceeding
        const userDocRef = doc(db, "users", apiResponse.userId);
        const userSnap = await getDoc(userDocRef);
        
        if (userSnap.exists()) {
          const userData = userSnap.data();
          if (userData.role !== 'admin') {
            // Not an admin - throw an error
            throw new Error('Only administrators can log in to this application');
          }
          
          try {
            // Try to sign in with Firebase but don't block if it fails
            await signInWithEmailAndPassword(auth, email, password);
          } catch (firebaseError) {
            console.error("Firebase auth error:", firebaseError);
            console.log("Setting user manually since Firebase auth failed but backend auth succeeded");
            
            // Manually set the current user since backend authentication was successful
            // We'll simulate what onAuthStateChanged would normally do
            setCurrentUser({
              uid: apiResponse.userId,
              email: apiResponse.email,
              displayName: userData.name || email,
              ...userData
            });
            setUserRole(userData.role || 'user');
          }
          
          // Return the backend response regardless of Firebase auth result
          return apiResponse;
        } else {
          // User document doesn't exist
          throw new Error('User profile not found. Please contact an administrator.');
        }
      }
      
      // If we got here without a valid response, throw an error
      throw new Error('Authentication failed with the backend');
    } catch (error) {
      console.error("Login error in AuthContext:", error);
      throw error;
    }
  };

  // Sign out function
  const signOut = () => {
    return firebaseSignOut(auth);
  };

  // Check backend connectivity
  const checkBackendStatus = async () => {
    try {
      const status = await pingBackend();
      // Only update state if it changed to avoid unnecessary rerenders
      if (status !== isBackendOnline) {
        setIsBackendOnline(status);
      }
      return status;
    } catch (error) {
      console.warn("Error checking backend status:", error);
      // Only update state if it changed
      if (isBackendOnline !== false) {
        setIsBackendOnline(false);
      }
      return false;
    }
  };

  // Check backend status periodically
  useEffect(() => {
    let intervalId;
    
    const checkStatus = async () => {
      await checkBackendStatus();
    };
    
    // Check immediately on mount
    checkStatus();
    
    // Then check every 60 seconds instead of 30
    // to reduce the frequency of error messages in the console
    intervalId = setInterval(checkStatus, 60000);
    
    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, []);

  // Fetch user data from Firestore
  const fetchUserData = async (user) => {
    if (!user) return null;
    
    try {
      const userRef = doc(db, "users", user.uid);
      const userSnap = await getDoc(userRef);
      
      if (userSnap.exists()) {
        return userSnap.data();
      } else {
        console.log("No user data found in Firestore");
        return null;
      }
    } catch (error) {
      console.error("Error fetching user data:", error);
      return null;
    }
  };

  // Effect to handle auth state changes
  useEffect(() => {
    let isMounted = true;
    setLoading(true);

    // Only seed the database in production or with explicit flag
    // Comment out or modify this section during development to prevent auto-login
    /* 
    seedDatabase().catch(error => {
      console.error("Error seeding database:", error);
    });
    */

    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      try {
        if (user) {
          // Ensure the user has a document in Firestore
          // Comment the next line to prevent auto-creation of user documents
          // await ensureUserDocument(user);
          
          const userData = await fetchUserData(user);
          
          if (isMounted) {
            setCurrentUser({
              uid: user.uid,
              email: user.email,
              displayName: user.displayName,
              photoURL: user.photoURL,
              emailVerified: user.emailVerified,
              ...userData
            });
            setUserRole(userData?.role || 'user');
          }
        } else {
          if (isMounted) {
            setCurrentUser(null);
            setUserRole(null);
          }
        }
      } catch (error) {
        console.error("Auth state change error:", error);
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    });

    // Cleanup subscription and mounted flag
    return () => {
      isMounted = false;
      unsubscribe();
    };
  }, []);

  // Password reset function
  const resetPassword = (email) => {
    return sendPasswordResetEmail(auth, email);
  };

  const value = {
    currentUser,
    userRole,
    loading,
    login,
    signOut,
    resetPassword,
    isAdmin: userRole === 'admin',
    isAuthenticated: !!currentUser,
    isBackendOnline,
    checkBackendStatus
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
}

// Custom hook to use the auth context
export function useAuth() {
  return useContext(AuthContext);
}

export default AuthContext; 