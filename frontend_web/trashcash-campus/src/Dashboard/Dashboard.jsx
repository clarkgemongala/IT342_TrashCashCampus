import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { db } from '../firebase';
import { collection, query, where, getDocs, orderBy, limit } from 'firebase/firestore';
import { useAuth } from '../contexts/AuthContext';
import { FaRecycle, FaLeaf, FaWater, FaTree } from 'react-icons/fa';
import { format } from 'date-fns';
import './Dashboard.css';

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
      <div className="loading">
        <div className="loading-spinner"></div>
        <p>Loading your recycling dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-container">
        <div className="error-message">
          <strong>Error!</strong>
          <span>{error}</span>
        </div>
      </div>
    );
  }

  if (!currentUser) {
    return (
      <div className="error-container">
        <div className="warning-message">
          <strong>Please log in</strong>
          <span>You need to be logged in to view this dashboard.</span>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      variants={containerVariants}
      initial="hidden"
      animate="visible"
      className="dashboard-container"
    >
      <div className="dashboard-content">
        {/* Stats Section */}
        <motion.div variants={itemVariants} className="stats-section">
          <div className="stat-card">
            <div className="stat-content">
              <div>
                <h3>Total Recycled</h3>
                <p className="stat-value">{userData?.totalRecycled || 0} kg</p>
              </div>
              <FaRecycle className="stat-icon" />
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-content">
              <div>
                <h3>Trees Saved</h3>
                <p className="stat-value">{impact.trees}</p>
              </div>
              <FaTree className="stat-icon" />
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-content">
              <div>
                <h3>Water Saved</h3>
                <p className="stat-value">{impact.water}L</p>
              </div>
              <FaWater className="stat-icon" />
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-content">
              <div>
                <h3>CO₂ Reduced</h3>
                <p className="stat-value">{impact.emissions}kg</p>
              </div>
              <FaLeaf className="stat-icon" />
            </div>
          </div>
        </motion.div>

        {/* Recent Activities */}
        <motion.div variants={itemVariants} className="activities-section">
          <h2>Recent Activities</h2>
          <div className="activities-content">
            {recentActivities.length === 0 ? (
              <div className="no-activities">
                <p>No recent activities found.</p>
              </div>
            ) : (
              <div className="activities-list">
                {recentActivities.map((activity) => (
                  <motion.div
                    key={activity.id}
                    variants={itemVariants}
                    className="activity-card"
                  >
                    <div className="activity-details">
                      <p className="activity-title">
                        {activity.userEmail === currentUser.email ? 'You' : activity.userEmail}
                      </p>
                      <p className="activity-subtitle">
                        Recycled {activity.weight || 0}kg at {activity.location || 'Unknown Location'}
                      </p>
                    </div>
                    <p className="activity-date">
                      {activity.timestamp?.toDate ? format(activity.timestamp.toDate(), 'MMM d, yyyy') : 'Unknown Date'}
                    </p>
                  </motion.div>
                ))}
              </div>
            )}
          </div>
        </motion.div>
      </div>
    </motion.div>
  );
}