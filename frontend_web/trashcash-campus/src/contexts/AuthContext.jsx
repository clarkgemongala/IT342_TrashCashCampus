import React, { createContext, useContext, useState, useEffect } from 'react';
import { auth, db } from '../firebase';
import { 
  onAuthStateChanged, 
  signOut as firebaseSignOut 
} from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { ensureUserDocument, seedDatabase } from '../seedData';

// Create context
const AuthContext = createContext();

// Context provider component
export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [userRole, setUserRole] = useState(null);
  const [loading, setLoading] = useState(true);

  // Sign out function
  const signOut = () => {
    return firebaseSignOut(auth);
  };

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
    isAuthenticated: !!currentUser
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