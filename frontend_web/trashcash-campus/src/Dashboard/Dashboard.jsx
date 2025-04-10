import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Dashboard.css';
import trashcashLogo from '../assets/trashcash-logo.png';

const Dashboard = () => {
  const [timeRange, setTimeRange] = useState('Today');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const navigate = useNavigate();
  
  const toggleDropdown = () => {
    setDropdownOpen(!dropdownOpen);
  };

  const navigateToUsers = () => {
    navigate('/users');
  };

  return (
    <div className="dashboard-container">
      {/* Left Sidebar */}
      <div className="sidebar">
        <div className="logo-container">
          <img src={trashcashLogo} alt="TrashCash Campus Logo" className="logo" />
        </div>
        
        <nav className="sidebar-nav">
          <div className="nav-item active">
            <span className="nav-icon">🏠</span>
            <span className="nav-text">Dashboard</span>
          </div>
          
          <div className="nav-item" onClick={navigateToUsers}>
            <span className="nav-icon">👤</span>
            <span className="nav-text">Users</span>
          </div>
          
          <div className="nav-item">
            <span className="nav-icon">🎁</span>
            <span className="nav-text">Rewards</span>
          </div>
          
          <div className="nav-item">
            <span className="nav-icon">📊</span>
            <span className="nav-text">Reports</span>
          </div>
        </nav>
        
        <div className="user-profile">
          <div className="avatar">H</div>
          <div className="user-info">
            <p className="welcome-text">Welcome</p>
            <p className="username">Himanshu</p>
          </div>
        </div>
      </div>
      
      {/* Main Content */}
      <div className="main-content">
        {/* Header */}
        <div className="dashboard-header">
          <h1>Dashboard</h1>
          
          <div className="timeframe-selector">
            <button className="timeframe-button" onClick={toggleDropdown}>
              {timeRange} <span className="dropdown-arrow">▼</span>
            </button>
            
            <div className="profile-icon">👤</div>
          </div>
        </div>
        
        {/* User Stats */}
        <div className="stats-card users-card">
          <div className="stats-icon">👥</div>
          <div className="stats-info">
            <p className="stats-label">Total Users</p>
            <h2 className="stats-value">5,423</h2>
          </div>
        </div>
        
        {/* Time Range Pills */}
        <div className="time-range-pills">
          <button className={`pill ${timeRange === 'Today' ? 'active' : ''}`} onClick={() => setTimeRange('Today')}>Today</button>
          <button className={`pill ${timeRange === 'This week' ? 'active' : ''}`} onClick={() => setTimeRange('This week')}>This week</button>
          <button className={`pill ${timeRange === 'Last Week' ? 'active' : ''}`} onClick={() => setTimeRange('Last Week')}>Last Week</button>
          <button className={`pill ${timeRange === 'This Month' ? 'active' : ''}`} onClick={() => setTimeRange('This Month')}>This Month</button>
          <button className={`pill ${timeRange === 'Last Month' ? 'active' : ''}`} onClick={() => setTimeRange('Last Month')}>Last Month</button>
          <button className={`pill ${timeRange === 'This Year' ? 'active' : ''}`} onClick={() => setTimeRange('This Year')}>This Year</button>
          <button className={`pill ${timeRange === 'Last Year' ? 'active' : ''}`} onClick={() => setTimeRange('Last Year')}>Last Year</button>
          <button className={`pill ${timeRange === 'Custom' ? 'active' : ''}`} onClick={() => setTimeRange('Custom')}>Custom</button>
        </div>
        
        {/* Garbage Chart */}
        <div className="chart-container">
          <div className="chart-header">
            <div className="chart-title">
              <p>Total Garbage Disposed</p>
              <h2>4,50,000</h2>
            </div>
            
            <div className="chart-filter">
              <button className="filter-button">
                Income <span className="dropdown-arrow">▼</span>
              </button>
            </div>
          </div>
          
          <div className="chart">
            <div className="y-axis">
              <div className="y-label">₹5L</div>
              <div className="y-label">₹4L</div>
              <div className="y-label">₹3L</div>
              <div className="y-label">₹2L</div>
              <div className="y-label">₹1L</div>
              <div className="y-label">₹0</div>
            </div>
            
            <div className="bars-container">
              <div className="bar-group">
                <div className="bar" style={{ height: '15%' }}></div>
                <div className="bar-label">Monday</div>
              </div>
              
              <div className="bar-group">
                <div className="bar" style={{ height: '50%' }}></div>
                <div className="bar-label">Tuesday</div>
              </div>
              
              <div className="bar-group">
                <div className="bar" style={{ height: '30%' }}></div>
                <div className="bar-label">Wednesday</div>
              </div>
              
              <div className="bar-group">
                <div className="bar active" style={{ height: '75%' }}></div>
                <div className="bar-label">Thursday</div>
              </div>
              
              <div className="bar-group">
                <div className="bar" style={{ height: '35%' }}></div>
                <div className="bar-label">Friday</div>
              </div>
              
              <div className="bar-group">
                <div className="bar" style={{ height: '55%' }}></div>
                <div className="bar-label">Saturday</div>
              </div>
              
              <div className="bar-group">
                <div className="bar" style={{ height: '33%' }}></div>
                <div className="bar-label">Sunday</div>
              </div>
            </div>
          </div>
        </div>
        
        {/* Statistics Cards */}
        <div className="stats-grid">
          <div className="stats-card small">
            <p className="stats-label">Total Garbage</p>
            <h2 className="stats-value">4.5L</h2>
          </div>
          
          <div className="stats-card small">
            <p className="stats-label">Biodegradable</p>
            <h2 className="stats-value">132</h2>
          </div>
          
          <div className="stats-card small">
            <p className="stats-label">Non-Biodegradable</p>
            <h2 className="stats-value">156</h2>
          </div>
          
          <div className="stats-card small">
            <p className="stats-label">Recyclable</p>
            <h2 className="stats-value">64</h2>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;