import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../firebase';
import { collection, query, where, getDocs, limit, orderBy, doc, getDoc } from 'firebase/firestore';
import Navigation from '../components/Navigation';
import BackendStatus from '../components/BackendStatus';
import './Dashboard.css';

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
            trees: (userData.totalRecycled || 0) * 0.1, // Approx 10 items = 1 tree
            water: (userData.totalRecycled || 0) * 100, // Liters of water
            co2: (userData.totalRecycled || 0) * 2.5 // kg of CO2
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

        // Fetch recent recycling activities
        try {
          const activitiesRef = collection(db, 'recyclingActivities');
          
          // Query to get all recent recycling activities from all bins
          const activitiesQuery = query(
            activitiesRef,
            orderBy('timestamp', 'desc'),
            limit(5)
          );
          
          const activitiesSnapshot = await getDocs(activitiesQuery);
          let activities = activitiesSnapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
          }));
          
          // For each activity, if we don't have the user email, try to fetch it
          const activitiesWithUser = await Promise.all(
            activities.map(async (activity) => {
              // If we already have the userEmail, just return the activity
              if (activity.userEmail) {
                return activity;
              }
              
              // If we have the userId but no userEmail, try to fetch the user data
              if (activity.userId) {
                try {
                  const userRef = doc(db, 'users', activity.userId);
                  const userSnap = await getDoc(userRef);
                  
                  if (userSnap.exists()) {
                    // Add the user email to the activity
                    return {
                      ...activity,
                      userEmail: userSnap.data().email || 'Unknown User'
                    };
                  }
                } catch (err) {
                  console.error("Error fetching user for activity:", err);
                }
              }
              
              // If we couldn't get the user email, return activity with default
              return {
                ...activity,
                userEmail: 'Unknown User'
              };
            })
          );
          
          setRecentActivities(activitiesWithUser);
        } catch (activityError) {
          console.error('Error fetching activities:', activityError);
          setRecentActivities([]); // Set empty array on error
        }
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, [currentUser]);

  // Format timestamp to readable date
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

  return (
    <div className="dashboard-container">
      <Navigation />
      
      <main className="dashboard-content">
        <div className="dashboard-header">
          <h1 className="dashboard-title">Dashboard</h1>
          <BackendStatus />
        </div>
        
        {loading ? (
          <div className="loading">Loading your recycling data...</div>
        ) : (
          <>
            {/* User stats section */}
            <section className="stats-section">
              <div className="stat-card">
                <h3>Total Points</h3>
                <div className="stat-value">{stats.totalPoints}</div>
              </div>
              
              <div className="stat-card">
                <h3>Items Recycled</h3>
                <div className="stat-value">{stats.totalRecycled}</div>
              </div>
              
              <div className="stat-card">
                <h3>Your Rank</h3>
                <div className="stat-value">{stats.rank}</div>
              </div>
            </section>
            
            {/* Environmental impact section */}
            <section className="impact-section">
              <h2>Your Environmental Impact</h2>
              <div className="impact-cards">
                <div className="impact-card">
                  <div className="impact-icon">🌳</div>
                  <div className="impact-value">{stats.impactSaved.trees.toFixed(1)}</div>
                  <div className="impact-label">Trees Saved</div>
              </div>
              
                <div className="impact-card">
                  <div className="impact-icon">💧</div>
                  <div className="impact-value">{stats.impactSaved.water.toFixed(0)}</div>
                  <div className="impact-label">Liters of Water Conserved</div>
              </div>
              
                <div className="impact-card">
                  <div className="impact-icon">☁️</div>
                  <div className="impact-value">{stats.impactSaved.co2.toFixed(1)}</div>
                  <div className="impact-label">kg of CO2 Emissions Reduced</div>
                </div>
              </div>
            </section>
            
            {/* Recent activities section */}
            <section className="activities-section">
              <h2>Recent Recycling Activities</h2>
              
              {recentActivities.length === 0 ? (
                <div className="no-activities">
                  <p>No recent recycling activities found!</p>
                  <p>Start recycling to earn points and see activities here.</p>
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
                        <div className="activity-title">
                          {activity.userEmail || 'Unknown User'} recycled {activity.wasteType} at {activity.binLocation || 'Unknown Location'}
                        </div>
                        <div className="activity-subtitle">
                          Bin: {activity.binName || activity.binId || 'Unknown Bin'}
                        </div>
                        <div className="activity-date">{formatDate(activity.timestamp)}</div>
                      </div>
                      <div className="activity-points">
                        <div className="bin-info">
                          {(() => {
                            const binType = activity.binType || 
                              (activity.wasteType === 'organic' ? 'organic' :
                               activity.wasteType === 'plastic' || activity.wasteType === 'paper' || 
                               activity.wasteType === 'glass' || activity.wasteType === 'metal' ? 'recyclable' : 'unknown');
                            
                            const binLabel = binType === 'organic' ? 'Biodegradable' : 
                                             binType === 'non-recyclable' ? 'Non-Biodegradable' : 
                                             binType === 'recyclable' ? 'Recyclable' : 
                                             activity.binName || 'Bin';
                            
                            return <span className={`bin-type ${binType}`}>{binLabel}</span>;
                          })()}
                        </div>
                        <div className="points-earned">+{activity.pointsEarned || 0} pts</div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
};

export default Dashboard;