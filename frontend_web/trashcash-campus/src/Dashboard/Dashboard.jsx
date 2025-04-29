import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { db } from '../firebase';
import { collection, query, where, getDocs, orderBy, limit } from 'firebase/firestore';
import { useAuth } from '../contexts/AuthContext';
import { FaRecycle, FaLeaf, FaWater, FaTree } from 'react-icons/fa';
import { format } from 'date-fns';

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1
    }
  }
};

const itemVariants = {
  hidden: { y: 20, opacity: 0 },
  visible: {
    y: 0,
    opacity: 1,
    transition: {
      type: "spring",
      stiffness: 100
    }
  }
};

export default function Dashboard() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [userData, setUserData] = useState(null);
  const [recentActivities, setRecentActivities] = useState([]);
  const { currentUser } = useAuth();

  useEffect(() => {
    async function fetchData() {
      if (!currentUser) {
        setError('No user found. Please log in.');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError('');

        // Fetch user data
        const userQuery = query(
          collection(db, 'users'),
          where('email', '==', currentUser.email)
        );
        const userSnapshot = await getDocs(userQuery);
        
        if (!userSnapshot.empty) {
          const userData = userSnapshot.docs[0].data();
          setUserData(userData);
        } else {
          console.warn('No user data found for:', currentUser.email);
        }

        // Fetch recent activities
        const activitiesQuery = query(
          collection(db, 'recycling_activities'),
          where('userId', '==', currentUser.uid),
          orderBy('timestamp', 'desc'),
          limit(5)
        );
        const activitiesSnapshot = await getDocs(activitiesQuery);
        
        const activities = activitiesSnapshot.docs.map(doc => ({
          ...doc.data(),
          id: doc.id,
          userEmail: currentUser.email
        }));
        
        setRecentActivities(activities);
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        setError('Failed to load dashboard data. Please try again later.');
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, [currentUser]);

  // Calculate environmental impact
  const calculateImpact = () => {
    if (!userData) return { trees: 0, water: 0, emissions: 0 };
    const totalRecycled = userData.totalRecycled || 0;
    return {
      trees: Math.round(totalRecycled * 0.017), // 17 trees saved per ton
      water: Math.round(totalRecycled * 7000), // 7000 gallons per ton
      emissions: Math.round(totalRecycled * 2.5) // 2.5 metric tons of CO2 per ton
    };
  };

  const impact = calculateImpact();

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-background p-4">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary mb-4"></div>
        <p className="text-text-light text-lg">Loading your recycling dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-background p-4">
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative" role="alert">
          <strong className="font-bold">Error!</strong>
          <span className="block sm:inline"> {error}</span>
        </div>
      </div>
    );
  }

  if (!currentUser) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-background p-4">
        <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded relative" role="alert">
          <strong className="font-bold">Please log in</strong>
          <span className="block sm:inline"> You need to be logged in to view this dashboard.</span>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      variants={containerVariants}
      initial="hidden"
      animate="visible"
      className="min-h-screen bg-background p-4 sm:p-6 lg:p-8"
    >
      {/* Stats Grid */}
      <motion.div variants={itemVariants} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <div className="bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow duration-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-text-light text-sm">Total Recycled</p>
              <p className="text-2xl font-bold text-text">{userData?.totalRecycled || 0} kg</p>
            </div>
            <FaRecycle className="text-3xl text-primary" />
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow duration-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-text-light text-sm">Trees Saved</p>
              <p className="text-2xl font-bold text-text">{impact.trees}</p>
            </div>
            <FaTree className="text-3xl text-secondary" />
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow duration-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-text-light text-sm">Water Saved</p>
              <p className="text-2xl font-bold text-text">{impact.water}L</p>
            </div>
            <FaWater className="text-3xl text-accent" />
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow duration-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-text-light text-sm">CO₂ Reduced</p>
              <p className="text-2xl font-bold text-text">{impact.emissions}kg</p>
            </div>
            <FaLeaf className="text-3xl text-primary" />
          </div>
        </div>
      </motion.div>

      {/* Recent Activities */}
      <motion.div variants={itemVariants} className="bg-white rounded-xl shadow-sm">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-text">Recent Activities</h2>
        </div>
        <div className="p-6">
          {recentActivities.length === 0 ? (
            <p className="text-text-light text-center py-4">No recent activities found.</p>
          ) : (
            <div className="space-y-4">
              {recentActivities.map((activity) => (
                <motion.div
                  key={activity.id}
                  variants={itemVariants}
                  className="flex items-center justify-between p-4 bg-background rounded-lg hover:bg-gray-50 transition-colors duration-200"
                >
                  <div>
                    <p className="text-sm font-medium text-text">
                      {activity.userEmail === currentUser.email ? 'You' : activity.userEmail}
                    </p>
                    <p className="text-sm text-text-light">
                      Recycled {activity.weight || 0}kg at {activity.location || 'Unknown Location'}
                    </p>
                  </div>
                  <p className="text-sm text-text-light">
                    {activity.timestamp?.toDate ? format(activity.timestamp.toDate(), 'MMM d, yyyy') : 'Unknown Date'}
                  </p>
                </motion.div>
              ))}
            </div>
          )}
        </div>
      </motion.div>
    </motion.div>
  );
}