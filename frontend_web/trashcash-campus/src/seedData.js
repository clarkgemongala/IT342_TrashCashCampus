import { db } from './firebase';
import { collection, addDoc, setDoc, doc, getDocs, query, where } from 'firebase/firestore';

// Seed data for recycling bins
const binsData = [
  {
    name: 'Engineering Building Bin',
    location: 'Ground Floor, Engineering Building',
    acceptedWaste: ['paper', 'plastic', 'metal'],
    tips: [
      'Please flatten cardboard boxes before recycling',
      'Remove caps from plastic bottles',
      'Rinse containers before disposal'
    ],
    coordinates: {
      lat: 10.2953,
      lng: 123.8854
    }
  },
  {
    name: 'Library Recycling Station',
    location: 'Main Library, 1st Floor',
    acceptedWaste: ['paper', 'plastic'],
    tips: [
      'Only clean paper is accepted here',
      'No food-contaminated items please',
      'Staples in paper are OK'
    ],
    coordinates: {
      lat: 10.2943,
      lng: 123.8864
    }
  },
  {
    name: 'Computer Science Dept Bin',
    location: 'CS Building, 2nd Floor Hallway',
    acceptedWaste: ['paper', 'plastic', 'metal', 'glass'],
    tips: [
      'E-waste collection also available nearby',
      'Please separate different materials',
      'No food waste in these bins'
    ],
    coordinates: {
      lat: 10.2933,
      lng: 123.8874
    }
  },
  {
    name: 'Student Center Bin',
    location: 'Main Student Center, Food Court',
    acceptedWaste: ['plastic', 'paper', 'organic'],
    tips: [
      'Food containers must be rinsed',
      'Organic waste for composting only',
      'No plastic straws or utensils in organic waste'
    ],
    coordinates: {
      lat: 10.2963,
      lng: 123.8844
    }
  },
  {
    name: 'Sports Complex Bin',
    location: 'Near Gymnasium Entrance',
    acceptedWaste: ['plastic', 'metal'],
    tips: [
      'Sports drinks bottles welcome',
      'Please empty liquids before disposal',
      'Remove caps from bottles'
    ],
    coordinates: {
      lat: 10.2973,
      lng: 123.8834
    }
  }
];

// Seed data for rewards
const rewardsData = [
  {
    title: 'Campus Bookstore 10% Discount',
    description: 'Get 10% off your next purchase at the campus bookstore',
    pointsCost: 100,
    category: 'campus',
    icon: '📚',
    backgroundColor: '#E3F2FD',
    available: true,
    expiryDate: new Date(2023, 11, 31)
  },
  {
    title: 'Free Coffee at Campus Cafe',
    description: 'Redeem for a free coffee of your choice at any campus cafe',
    pointsCost: 75,
    category: 'food',
    icon: '☕',
    backgroundColor: '#FFF3E0',
    available: true,
    expiryDate: new Date(2023, 11, 31)
  },
  {
    title: 'TrashCash Eco Bottle',
    description: 'Stylish reusable water bottle with the TrashCash logo',
    pointsCost: 200,
    category: 'merchandise',
    icon: '🍶',
    backgroundColor: '#E8F5E9',
    available: true,
    expiryDate: null
  },
  {
    title: 'Campus Printing Credit',
    description: '100 pages of free printing at any campus computer lab',
    pointsCost: 120,
    category: 'campus',
    icon: '🖨️',
    backgroundColor: '#F3E5F5',
    available: true,
    expiryDate: new Date(2023, 11, 31)
  },
  {
    title: 'Sustainable Campus Tour',
    description: 'Join an exclusive tour of the sustainable facilities on campus',
    pointsCost: 150,
    category: 'experiences',
    icon: '🌱',
    backgroundColor: '#E0F7FA',
    available: true,
    expiryDate: new Date(2023, 10, 30)
  },
  {
    title: 'Eco-Friendly Stationery Set',
    description: 'Notebook, pen, and pencil made from recycled materials',
    pointsCost: 180,
    category: 'merchandise',
    icon: '✏️',
    backgroundColor: '#FFFDE7',
    available: true,
    expiryDate: null
  },
  {
    title: 'Free Lunch at Campus Cafeteria',
    description: 'Enjoy a free lunch of your choice at the main campus cafeteria',
    pointsCost: 150,
    category: 'food',
    icon: '🥪',
    backgroundColor: '#FBE9E7',
    available: true,
    expiryDate: new Date(2023, 11, 15)
  },
  {
    title: 'Priority Registration',
    description: 'Get priority registration for one course next semester',
    pointsCost: 500,
    category: 'campus',
    icon: '🎓',
    backgroundColor: '#F1F8E9',
    available: true,
    expiryDate: new Date(2023, 12, 15)
  }
];

// Seed function for bins
export const seedBins = async () => {
  try {
    // Check if bins already exist
    const binsSnapshot = await getDocs(collection(db, 'bins'));
    
    if (binsSnapshot.size > 0) {
      console.log('Bins data already exists, skipping seed');
      return;
    }
    
    // Add each bin to Firestore
    const binPromises = binsData.map(bin => addDoc(collection(db, 'bins'), bin));
    await Promise.all(binPromises);
    
    console.log('Bins data seeded successfully');
  } catch (error) {
    console.error('Error seeding bins data:', error);
  }
};

// Seed function for rewards
export const seedRewards = async () => {
  try {
    // Check if rewards already exist
    const rewardsSnapshot = await getDocs(collection(db, 'rewards'));
    
    if (rewardsSnapshot.size > 0) {
      console.log('Rewards data already exists, skipping seed');
      return;
    }
    
    // Add each reward to Firestore
    const rewardPromises = rewardsData.map(reward => addDoc(collection(db, 'rewards'), reward));
    await Promise.all(rewardPromises);
    
    console.log('Rewards data seeded successfully');
  } catch (error) {
    console.error('Error seeding rewards data:', error);
  }
};

// Function to ensure a new user has a document in the users collection
export const ensureUserDocument = async (user) => {
  if (!user) return;
  
  try {
    // Check if user document already exists
    const userRef = doc(db, 'users', user.uid);
    const userQuery = query(collection(db, 'users'), where('uid', '==', user.uid));
    const userSnapshot = await getDocs(userQuery);
    
    if (userSnapshot.size === 0) {
      // Create new user document
      await setDoc(userRef, {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName || '',
        photoURL: user.photoURL || '',
        totalPoints: 100, // Start with some points
        totalRecycled: 0,
        isEmailVerified: user.emailVerified,
        createdAt: new Date(),
        role: 'user' // Default role
      });
      
      console.log('User document created successfully');
    }
  } catch (error) {
    console.error('Error ensuring user document:', error);
  }
};

// Main seed function to call all seed functions
export const seedDatabase = async () => {
  await seedBins();
  await seedRewards();
  console.log('Database seeding completed');
}; 