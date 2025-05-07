import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../firebase';
import { collection, query, where, getDocs, limit, orderBy, doc, getDoc, Timestamp } from 'firebase/firestore';
import Navigation from '../components/Navigation';
import BackendStatus from '../components/BackendStatus';
import './Dashboard.css';

const Dashboard = () => {
  const { currentUser } = useAuth();
  const [adminStats, setAdminStats] = useState({
    totalUsers: 0,
    totalBins: 0,
    totalActivities: 0,
    systemImpact: {
      trees: 0,
      water: 0,
      co2: 0
    }
  });
  const [recentActivities, setRecentActivities] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAdminData = async () => {
      try {
        // Fetch total users
        const usersRef = collection(db, 'users');
        const usersSnapshot = await getDocs(usersRef);
        const totalUsers = usersSnapshot.size;

        // Count total bins
        const binsRef = collection(db, 'bins');
        const binsSnapshot = await getDocs(binsRef);
        const totalBins = binsSnapshot.size;

        // Check if binLogs collection exists and use it preferentially
        let targetCollection = 'binLogs';
        const testBinLogsRef = collection(db, 'binLogs');
        const testBinLogs = await getDocs(query(testBinLogsRef, limit(1)));
        
        if (testBinLogs.empty) {
          targetCollection = 'recyclingActivities';
        }

        // Count total activities
        const activitiesRef = collection(db, targetCollection);
        const activitiesSnapshot = await getDocs(activitiesRef);
        const totalActivities = activitiesSnapshot.size;

        // Calculate system-wide environmental impact
        let totalItemsRecycled = 0;
        
        activitiesSnapshot.docs.forEach(doc => {
          const data = doc.data();
          // Count each logged activity as at least 1 item recycled
          totalItemsRecycled++;
        });

        // Calculate impact metrics
        const systemImpact = {
          trees: totalItemsRecycled * 0.1,
          water: totalItemsRecycled * 100,
          co2: totalItemsRecycled * 2.5
        };

        setAdminStats({
          totalUsers,
          totalBins,
          totalActivities,
          systemImpact
        });

        // Fetch recent recycling activities
        try {
          const activitiesQuery = query(
            collection(db, targetCollection),
            orderBy('timestamp', 'desc'),
            limit(10)
          );
          
          const activitiesSnapshot = await getDocs(activitiesQuery);
          let activities = activitiesSnapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
          }));
          
          // For each activity, if we don't have the user email, try to fetch it
          const activitiesWithUser = await Promise.all(
            activities.map(async (activity) => {
              if (activity.userEmail) {
                return activity;
              }
              
              if (activity.userId) {
                try {
                  const userRef = doc(db, 'users', activity.userId);
                  const userSnap = await getDoc(userRef);
                  
                  if (userSnap.exists()) {
                    return {
                      ...activity,
                      userEmail: userSnap.data().email || 'Unknown User'
                    };
                  }
                } catch (err) {
                  console.error("Error fetching user for activity:", err);
                }
              }
              
              return {
                ...activity,
                userEmail: 'Unknown User'
              };
            })
          );
          
          setRecentActivities(activitiesWithUser);
        } catch (activityError) {
          console.error('Error fetching activities:', activityError);
          setRecentActivities([]);
        }
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchAdminData();
  }, []);

  // Format timestamp to readable date
  const formatDate = (timestamp) => {
    if (!timestamp) return 'Unknown';
    
    const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  };

  // Get appropriate icon based on waste type
  const getWasteIcon = (wasteType) => {
    if (!wasteType) return '♻️';
    
    const type = wasteType.toLowerCase();
    if (type.includes('paper')) return '📄';
    if (type.includes('plastic')) return '🥤';
    if (type.includes('glass')) return '🍶';
    if (type.includes('metal')) return '🥫';
    if (type.includes('organic') || type.includes('food')) return '🌱';
    return '♻️';
  };

  // Get bin type from waste type
  const getBinType = (wasteType) => {
    if (!wasteType) return 'recyclable';
    
    const type = wasteType.toLowerCase();
    if (type.includes('organic') || type.includes('food')) return 'biodegradable';
    if (type.includes('paper') || type.includes('plastic') || 
        type.includes('glass') || type.includes('metal')) return 'recyclable';
    return 'recyclable';
  };

  // Get points based on waste type
  const getPoints = (wasteType, pointsEarned) => {
    if (pointsEarned !== undefined && pointsEarned !== null) {
      return pointsEarned;
    }
    
    if (!wasteType) return 5;
    
    const type = wasteType.toLowerCase();
    if (type.includes('organic') || type.includes('food')) return 5;
    if (type.includes('paper')) return 10;
    if (type.includes('plastic')) return 10;
    if (type.includes('glass')) return 10;
    if (type.includes('metal')) return 25;
    return 5;
  };

  return (
    <div className="dashboard-container">
      <Navigation />
      
      <main className="dashboard-content">
        <div className="dashboard-header">
          <h1 className="dashboard-title">Admin Dashboard</h1>
          <BackendStatus />
        </div>
        
        {loading ? (
          <div className="loading">Loading system data...</div>
        ) : (
          <>
            {/* Admin stats section */}
            <section className="stats-section">
              <div className="stat-card">
                <h3>Total Users</h3>
                <div className="stat-value">{adminStats.totalUsers}</div>
              </div>
              
              <div className="stat-card">
                <h3>QR Bins Deployed</h3>
                <div className="stat-value">{adminStats.totalBins}</div>
              </div>
              
              <div className="stat-card">
                <h3>Total Activities</h3>
                <div className="stat-value">{adminStats.totalActivities}</div>
              </div>
            </section>
            
            {/* System impact section */}
            <section className="impact-section">
              <h2>System Environmental Impact</h2>
              <div className="impact-cards">
                <div className="impact-card">
                  <div className="impact-icon">🌳</div>
                  <div className="impact-value">{adminStats.systemImpact.trees.toFixed(1)}</div>
                  <div className="impact-label">Trees Saved</div>
                </div>
              
                <div className="impact-card">
                  <div className="impact-icon">💧</div>
                  <div className="impact-value">{adminStats.systemImpact.water.toFixed(0)}</div>
                  <div className="impact-label">Liters of Water Conserved</div>
                </div>
              
                <div className="impact-card">
                  <div className="impact-icon">☁️</div>
                  <div className="impact-value">{adminStats.systemImpact.co2.toFixed(1)}</div>
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
                  <p>Users haven't submitted any recycling activities yet.</p>
                </div>
              ) : (
                <div className="activities-list">
                  {recentActivities.map((activity) => (
                    <div key={activity.id} className="activity-card">
                      <div className="activity-icon">
                        {getWasteIcon(activity.wasteType)}
                      </div>
                      <div className="activity-details">
                        <div className="activity-title">
                          {activity.userEmail} recycled {activity.wasteType || 'waste'} at {activity.binLocation || activity.locationName || 'Unknown Location'}
                        </div>
                        <div className="activity-subtitle">
                          Bin: {activity.binName || activity.binId || 'Unknown Bin'}
                        </div>
                        <div className="activity-date">{formatDate(activity.timestamp)}</div>
                      </div>
                      <div className="activity-points">
                        <div className="bin-info">
                          <span className={`bin-type ${activity.binType || getBinType(activity.wasteType)}`}>
                            {(activity.binType === 'biodegradable' || 
                             activity.binType === 'organic' || 
                             activity.wasteType === 'organic' || 
                             activity.wasteType === 'food') ? 'Biodegradable' : 'Recyclable'}
                          </span>
                        </div>
                        <div className="points-earned">+{getPoints(activity.wasteType, activity.pointsEarned)} pts</div>
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