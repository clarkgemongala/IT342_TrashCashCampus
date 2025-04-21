// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getAnalytics } from "firebase/analytics";

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyBAY9VKwIz817C9Lrko8THQNmdo049lHS4",
  authDomain: "trashcashcampusmobile.firebaseapp.com",
  projectId: "trashcashcampusmobile",
  storageBucket: "trashcashcampusmobile.appspot.com",
  messagingSenderId: "761747053026",
  appId: "1:761747053026:web:a5885b4a5d381610a101bb",
  measurementId: "G-0QS8HDMBRE"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const googleProvider = new GoogleAuthProvider();
const db = getFirestore(app);
const storage = getStorage(app);
const analytics = getAnalytics(app);

export { auth, googleProvider, db, storage, analytics };
export default app; 