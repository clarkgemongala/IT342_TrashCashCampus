const { initializeApp } = require('firebase/app');
const { getFirestore, collection, query, where, getDocs, doc, updateDoc } = require('firebase/firestore');

// Firebase configuration from project settings
const firebaseConfig = {
  apiKey: "AIzaSyD4KSCSLvJvE6-PZp5EoPH7nGfDpftKTck",
  authDomain: "trashcashcampusmobile.firebaseapp.com",
  projectId: "trashcashcampusmobile",
  storageBucket: "trashcashcampusmobile.appspot.com",
  messagingSenderId: "761747053026",
  appId: "1:761747053026:web:a5885b4a5d381610a101bb"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

/**
 * Function to update a specific user to admin role
 */
async function makeUserAdmin() {
  try {
    // Find the user with the given email
    const userEmail = 'drewadrein.odilao@cit.edu';
    console.log(`Looking for user with email: ${userEmail}`);
    
    const usersRef = collection(db, 'users');
    const q = query(usersRef, where('email', '==', userEmail));
    const querySnapshot = await getDocs(q);
    
    if (querySnapshot.empty) {
      console.error(`User with email ${userEmail} not found in Firestore`);
      return;
    }
    
    // Get the first matching document
    const userDoc = querySnapshot.docs[0];
    const userId = userDoc.id;
    const userData = userDoc.data();
    
    console.log(`Found user: ${userId}`);
    console.log(`Current role: ${userData.role || 'user'}`);
    
    // Update the user's role to admin
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, {
      role: 'admin'
    });
    
    console.log(`Successfully updated user ${userEmail} to admin role`);
  } catch (error) {
    console.error('Error updating user role:', error);
  }
}

// Run the function
makeUserAdmin()
  .then(() => {
    console.log('Admin update operation complete');
    process.exit(0);
  })
  .catch(err => {
    console.error('Error in admin update operation:', err);
    process.exit(1);
  }); 