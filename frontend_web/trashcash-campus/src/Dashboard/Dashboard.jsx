import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { db } from '../firebase';
import { collection, query, where, getDocs, orderBy, limit } from 'firebase/firestore';
import { useAuth } from '../contexts/AuthContext';
import './Dashboard.css';

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      duration: 0.3,
      when: "beforeChildren",
      staggerChildren: 0.1
    }
  }
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.3
    }
  }
};

const Dashboard = () => {
  const { currentUser } = useAuth();
  const [stats, setStats] = useState({
    totalPoints: 0,
    totalRecycled: 0,
    rank: 'Beginner',
    impactSaved: {
      trees: 0,
      water: 0,
      co2: 0
    }
  });
  const [recentActivities, setRecentActivities] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        if (!currentUser) return;

        // Fetch user stats
        const userRef = collection(db, 'users');
        const userQuery = query(userRef, where('uid', '==', currentUser.uid));
        const userSnapshot = await getDocs(userQuery);
        
        if (!userSnapshot.empty) {
          const userData = userSnapshot.docs[0].data();
          
          // Calculate environmental impact
          const impactSaved = {
            trees: (userData.totalRecycled || 0) * 0.1,
            water: (userData.totalRecycled || 0) * 100,
            co2: (userData.totalRecycled || 0) * 2.5
          };
          
          // Determine rank based on points
          let rank = 'Beginner';
          const points = userData.totalPoints || 0;
          
          if (points >= 1000) rank = 'Recycling Master';
          else if (points >= 500) rank = 'Eco Warrior';
          else if (points >= 200) rank = 'Green Champion';
          else if (points >= 100) rank = 'Recycling Enthusiast';
          
          setStats({
            totalPoints: userData.totalPoints || 0,
            totalRecycled: userData.totalRecycled || 0,
            rank,
            impactSaved
          });
        }

        // Fetch recent activities
        const activitiesRef = collection(db, 'recyclingActivities');
        const activitiesQuery = query(
          activitiesRef,
          orderBy('timestamp', 'desc'),
          limit(5)
        );
        
        const activitiesSnapshot = await getDocs(activitiesQuery);
        const activities = await Promise.all(
          activitiesSnapshot.docs.map(async (doc) => {
            const data = doc.data();
            if (data.userId && !data.userEmail) {
              try {
                const userRef = doc(db, 'users', data.userId);
                const userSnap = await getDocs(userRef);
                if (userSnap.empty) {
                  console.error("User data not found for activity:", data.userId);
                  return null;
                }
                const userData = userSnap.docs[0].data();
                return {
                  id: doc.id,
                  ...data,
                  userEmail: userData.email || 'Unknown User'
                };
              } catch (err) {
                console.error("Error fetching user for activity:", err);
                return null;
              }
            }
            return {
              id: doc.id,
              ...data,
              userEmail: data.userEmail || 'Unknown User'
            };
          })
        );
        
        setRecentActivities(activities.filter(Boolean));
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, [currentUser]);

  const formatDate = (timestamp) => {
    if (!timestamp) return 'Unknown';
    const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <div className="loading"></div>
          <p className="text-text-light">Loading your recycling data...</p>
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
      <main className="dashboard-content">
        <motion.div variants={itemVariants} className="dashboard-header">
          <h1 className="dashboard-title">Dashboard</h1>
          <p className="dashboard-subtitle">Track your recycling impact and activities</p>
        </motion.div>

        {/* Stats Section */}
        <motion.section variants={itemVariants} className="stats-section">
          <div className="stat-card">
            <h3>Total Points</h3>
            <p className="stat-value">{stats.totalPoints}</p>
          </div>
          
          <div className="stat-card">
            <h3>Items Recycled</h3>
            <p className="stat-value">{stats.totalRecycled}</p>
          </div>
          
          <div className="stat-card">
            <h3>Your Rank</h3>
            <p className="stat-value">{stats.rank}</p>
          </div>
        </motion.section>

        {/* Environmental Impact */}
        <motion.section variants={itemVariants} className="impact-section">
          <h2>Your Environmental Impact</h2>
          <div className="impact-cards">
            <div className="impact-card">
              <div className="impact-icon">🌳</div>
              <p className="impact-value">{stats.impactSaved.trees.toFixed(1)}</p>
              <p className="impact-label">Trees Saved</p>
            </div>
            
            <div className="impact-card">
              <div className="impact-icon">💧</div>
              <p className="impact-value">{stats.impactSaved.water.toFixed(0)}</p>
              <p className="impact-label">Liters of Water Conserved</p>
            </div>
            
            <div className="impact-card">
              <div className="impact-icon">☁️</div>
              <p className="impact-value">{stats.impactSaved.co2.toFixed(1)}</p>
              <p className="impact-label">kg of CO2 Emissions Reduced</p>
            </div>
          </div>
        </motion.section>

        {/* Recent Activities */}
        <motion.section variants={itemVariants} className="activities-section">
          <h2>Recent Activities</h2>
          {recentActivities.length === 0 ? (
            <div className="no-activities">
              <p>No recent recycling activities found!</p>
              <p>Start recycling to earn points and see your activities here.</p>
            </div>
          ) : (
            <div className="activities-list">
              {recentActivities.map((activity) => (
                <div key={activity.id} className="activity-card">
                  <div className="activity-icon">
                    {activity.wasteType === 'paper' ? '📄' :
                     activity.wasteType === 'plastic' ? '🥤' :
                     activity.wasteType === 'glass' ? '🍶' :
                     activity.wasteType === 'metal' ? '🥫' : '♻️'}
                  </div>
                  <div className="activity-details">
                    <p className="activity-title">
                      {activity.userEmail} recycled {activity.wasteType} at {activity.binLocation || 'Unknown Location'}
                    </p>
                    <p className="activity-subtitle">
                      {formatDate(activity.timestamp)}
                    </p>
                  </div>
                  <div className="activity-points">
                    <p className="bin-info">{activity.binName || activity.binId || 'Unknown Bin'}</p>
                    <p className="points-earned">+{activity.pointsEarned || 0} pts</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </motion.section>
      </main>
    </motion.div>
  );
};

export default Dashboard;