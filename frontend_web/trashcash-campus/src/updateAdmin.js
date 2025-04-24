import { db } from './firebase';
import { collection, query, where, getDocs, doc, updateDoc } from 'firebase/firestore';

/**
 * Script to update a specific user to admin role
 * Run this script once to make the change
 */
const updateUserToAdmin = async () => {
  try {
    // Find the user with the given email
    const userEmail = 'drewadrein.odilao@cit.edu';
    const usersRef = collection(db, 'users');
    const q = query(usersRef, where('email', '==', userEmail));
    const querySnapshot = await getDocs(q);
    
    if (querySnapshot.empty) {
      console.error(`User with email ${userEmail} not found`);
      return;
    }
    
    // Get the first matching document
    const userDoc = querySnapshot.docs[0];
    const userId = userDoc.id;
    
    // Update the user's role to admin
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, {
      role: 'admin'
    });
    
    console.log(`Successfully updated user ${userEmail} to admin role`);
  } catch (error) {
    console.error('Error updating user role:', error);
  }
};

// Run the function
updateUserToAdmin().then(() => console.log('Admin update complete'));

export default updateUserToAdmin; 