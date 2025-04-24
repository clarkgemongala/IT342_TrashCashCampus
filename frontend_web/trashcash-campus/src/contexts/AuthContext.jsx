import React, { createContext, useContext, useState, useEffect } from 'react';
import { auth, db } from '../firebase';
import { 
  onAuthStateChanged, 
  signOut as firebaseSignOut 
} from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { ensureUserDocument, seedDatabase } from '../seedData';
import { pingBackend } from '../services/api';

// Create context
const AuthContext = createContext();

// Context provider component
export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [userRole, setUserRole] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isBackendOnline, setIsBackendOnline] = useState(null);

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

    // Seed the database
    seedDatabase().catch(error => {
      console.error("Error seeding database:", error);
    });

    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      try {
        if (user) {
          // Ensure the user has a document in Firestore
          await ensureUserDocument(user);
          
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

  const value = {
    currentUser,
    userRole,
    loading,
    signOut,
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