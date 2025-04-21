import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../firebase';
import { collection, query, where, getDocs, orderBy } from 'firebase/firestore';
import Navigation from '../components/Navigation';
import './Analytics.css';

const Analytics = () => {
  const { currentUser } = useAuth();
  const [loading, setLoading] = useState(true);
  const [totalRecycled, setTotalRecycled] = useState(0);
  const [wasteByType, setWasteByType] = useState({});
  const [weeklyTrends, setWeeklyTrends] = useState([]);
  const [impactStats, setImpactStats] = useState({
    trees: 0,
    water: 0,
    co2: 0
  });
  const [timeRange, setTimeRange] = useState('month'); // week, month, year, all

  useEffect(() => {
    const fetchAnalyticsData = async () => {
      if (!currentUser) return;
      
      try {
        setLoading(true);
        
        // Get date range based on selected time range
        const currentDate = new Date();
        let startDate = new Date();
        
        switch (timeRange) {
          case 'week':
            startDate.setDate(currentDate.getDate() - 7);
            break;
          case 'month':
            startDate.setMonth(currentDate.getMonth() - 1);
            break;
          case 'year':
            startDate.setFullYear(currentDate.getFullYear() - 1);
            break;
          case 'all':
            startDate = new Date(0); // Beginning of time
            break;
          default:
            startDate.setMonth(currentDate.getMonth() - 1);
        }
        
        // Fetch user's recycling activities
        const activitiesRef = collection(db, 'recyclingActivities');
        let activitiesQuery;
        
        if (timeRange !== 'all') {
          activitiesQuery = query(
            activitiesRef,
            where('userId', '==', currentUser.uid),
            where('timestamp', '>=', startDate),
            orderBy('timestamp', 'asc')
          );
        } else {
          activitiesQuery = query(
            activitiesRef,
            where('userId', '==', currentUser.uid),
            orderBy('timestamp', 'asc')
          );
        }
        
        const activitiesSnapshot = await getDocs(activitiesQuery);
        const activities = activitiesSnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        
        // Calculate total recycled items
        setTotalRecycled(activities.length);
        
        // Calculate waste by type
        const wasteTypes = {};
        activities.forEach(activity => {
          const type = activity.wasteType || 'unknown';
          wasteTypes[type] = (wasteTypes[type] || 0) + 1;
        });
        setWasteByType(wasteTypes);
        
        // Calculate weekly trends
        const weeksData = calculateWeeklyTrends(activities);
        setWeeklyTrends(weeksData);
        
        // Calculate environmental impact
        const impact = {
          trees: activities.length * 0.1, // Approx 10 items = 1 tree
          water: activities.length * 100, // Liters of water
          co2: activities.length * 2.5 // kg of CO2
        };
        setImpactStats(impact);
        
      } catch (error) {
        console.error('Error fetching analytics data:', error);
      } finally {
        setLoading(false);
      }
    };
    
    fetchAnalyticsData();
  }, [currentUser, timeRange]);

  // Calculate weekly trends from activities
  const calculateWeeklyTrends = (activities) => {
    const weeks = {};
    
    activities.forEach(activity => {
      if (!activity.timestamp) return;
      
      const date = activity.timestamp.toDate ? activity.timestamp.toDate() : new Date(activity.timestamp);
      
      // Get ISO week number
      const weekNumber = getWeekNumber(date);
      const weekKey = `${date.getFullYear()}-W${weekNumber}`;
      
      if (!weeks[weekKey]) {
        weeks[weekKey] = {
          weekKey,
          weekLabel: `Week ${weekNumber}`,
          count: 0
        };
      }
      
      weeks[weekKey].count += 1;
    });
    
    // Convert to array and sort by week
    return Object.values(weeks).sort((a, b) => {
      return a.weekKey.localeCompare(b.weekKey);
    });
  };

  // Helper function to get ISO week number
  const getWeekNumber = (date) => {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay() || 7));
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
  };

  // Get the maximum value for the chart scale
  const getMaxChartValue = () => {
    if (weeklyTrends.length === 0) return 10;
    return Math.max(...weeklyTrends.map(week => week.count)) + 2;
  };

  // Handle time range change
  const handleTimeRangeChange = (range) => {
    setTimeRange(range);
  };

  return (
    <div className="analytics-container">
      <Navigation />
      
      <main className="analytics-content">
        <h1 className="analytics-title">Recycling Analytics</h1>
        
        {loading ? (
          <div className="loading">Loading your analytics data...</div>
        ) : (
          <>
            {/* Time range selector */}
            <div className="time-range-selector">
              <button 
                className={`range-button ${timeRange === 'week' ? 'active' : ''}`}
                onClick={() => handleTimeRangeChange('week')}
              >
                Last Week
              </button>
              <button 
                className={`range-button ${timeRange === 'month' ? 'active' : ''}`}
                onClick={() => handleTimeRangeChange('month')}
              >
                Last Month
              </button>
              <button 
                className={`range-button ${timeRange === 'year' ? 'active' : ''}`}
                onClick={() => handleTimeRangeChange('year')}
              >
                Last Year
              </button>
              <button 
                className={`range-button ${timeRange === 'all' ? 'active' : ''}`}
                onClick={() => handleTimeRangeChange('all')}
              >
                All Time
              </button>
            </div>
            
            {/* Summary stats */}
            <div className="summary-stats">
              <div className="stat-card total">
                <div className="stat-icon">♻️</div>
                <div className="stat-value">{totalRecycled}</div>
                <div className="stat-label">Items Recycled</div>
              </div>
              
              <div className="stat-card trees">
                <div className="stat-icon">🌳</div>
                <div className="stat-value">{impactStats.trees.toFixed(1)}</div>
                <div className="stat-label">Trees Saved</div>
              </div>
              
              <div className="stat-card water">
                <div className="stat-icon">💧</div>
                <div className="stat-value">{impactStats.water.toFixed(0)}</div>
                <div className="stat-label">Liters of Water Saved</div>
              </div>
              
              <div className="stat-card co2">
                <div className="stat-icon">☁️</div>
                <div className="stat-value">{impactStats.co2.toFixed(1)}</div>
                <div className="stat-label">kg CO2 Reduced</div>
              </div>
            </div>
            
            {/* Waste by type */}
            <div className="analytics-section">
              <h2>Waste by Type</h2>
              <div className="waste-types-chart">
                {Object.keys(wasteByType).length === 0 ? (
                  <div className="no-data">No recycling data available</div>
                ) : (
                  <div className="waste-bars">
                    {Object.entries(wasteByType).map(([type, count]) => (
                      <div key={type} className="waste-bar-container">
                        <div className="waste-label">
                          {type === 'paper' ? '📄 Paper' :
                           type === 'plastic' ? '🥤 Plastic' :
                           type === 'glass' ? '🍶 Glass' :
                           type === 'metal' ? '🥫 Metal' :
                           type === 'organic' ? '🍎 Organic' : 
                           `♻️ ${type.charAt(0).toUpperCase() + type.slice(1)}`}
                        </div>
                        <div className="waste-bar-wrapper">
                          <div 
                            className={`waste-bar ${type}`} 
                            style={{width: `${(count / totalRecycled) * 100}%`}}
                          ></div>
                          <span className="waste-count">{count}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
            
            {/* Weekly trends */}
            <div className="analytics-section">
              <h2>Recycling Trends</h2>
              <div className="trends-chart">
                {weeklyTrends.length === 0 ? (
                  <div className="no-data">No trend data available</div>
                ) : (
                  <>
                    <div className="chart-y-axis">
                      {[...Array(5)].map((_, i) => (
                        <div key={i} className="y-label">
                          {Math.round(getMaxChartValue() * (4 - i) / 4)}
                        </div>
                      ))}
                    </div>
                    <div className="chart-bars">
                      {weeklyTrends.map((week) => (
                        <div key={week.weekKey} className="chart-bar-container">
                          <div 
                            className="chart-bar"
                            style={{
                              height: `${(week.count / getMaxChartValue()) * 100}%`
                            }}
                          >
                            <div className="bar-tooltip">{week.count} items</div>
                          </div>
                          <div className="chart-label">{week.weekLabel}</div>
                        </div>
                      ))}
                    </div>
                  </>
                )}
              </div>
            </div>
            
            {/* Educational impact */}
            <div className="analytics-section">
              <h2>Your Environmental Impact</h2>
              <div className="impact-cards">
                <div className="impact-card">
                  <div className="impact-icon">🌳</div>
                  <h3>Tree Equivalent</h3>
                  <p>Your recycling efforts are equivalent to planting {impactStats.trees.toFixed(1)} trees!</p>
                  <p className="impact-fact">Did you know? A mature tree can absorb around 48 pounds of CO2 per year.</p>
                </div>
                
                <div className="impact-card">
                  <div className="impact-icon">💧</div>
                  <h3>Water Saved</h3>
                  <p>You've helped conserve {impactStats.water.toFixed(0)} liters of water through recycling!</p>
                  <p className="impact-fact">Recycling paper reduces water pollution by 35% and water usage by 58%.</p>
                </div>
                
                <div className="impact-card">
                  <div className="impact-icon">⚡</div>
                  <h3>Energy Conserved</h3>
                  <p>Your recycling has saved enough energy to power a home for {Math.round(totalRecycled / 10)} days.</p>
                  <p className="impact-fact">Recycling aluminum cans saves 95% of the energy used to make new ones.</p>
                </div>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
};

export default Analytics; 