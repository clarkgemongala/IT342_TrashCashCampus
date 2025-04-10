import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './User.css';
import trashcashLogo from '../assets/trashcash-logo.png';

const User = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState('Newest');
  const [currentPage, setCurrentPage] = useState(1);
  
  // Mock data for the users table
  const userData = [
    { id: 1, name: 'Jane Cooper', course: 'BSIT', phone: '(225) 555-0118', email: 'jane@microsoft.com', country: 'United States', status: 'Active' },
    { id: 2, name: 'Floyd Miles', course: 'CCS', phone: '(205) 555-0100', email: 'floyd@yahoo.com', country: 'Kiribati', status: 'Inactive' },
    { id: 3, name: 'Ronald Richards', course: 'Adobe', phone: '(302) 555-0107', email: 'ronald@adobe.com', country: 'Israel', status: 'Inactive' },
    { id: 4, name: 'Marvin McKinney', course: 'Tesla', phone: '(252) 555-0126', email: 'marvin@tesla.com', country: 'Iran', status: 'Active' },
    { id: 5, name: 'Jerome Bell', course: 'Google', phone: '(629) 555-0129', email: 'jerome@google.com', country: 'Réunion', status: 'Active' },
    { id: 6, name: 'Kathryn Murphy', course: 'Microsoft', phone: '(406) 555-0120', email: 'kathryn@microsoft.com', country: 'Curaçao', status: 'Active' },
    { id: 7, name: 'Jacob Jones', course: 'Yahoo', phone: '(208) 555-0112', email: 'jacob@yahoo.com', country: 'Brazil', status: 'Active' },
    { id: 8, name: 'Kristin Watson', course: 'Facebook', phone: '(704) 555-0127', email: 'kristin@facebook.com', country: 'Åland Islands', status: 'Inactive' },
  ];

  const totalEntries = 256000;
  const totalPages = 40;

  const navigateToDashboard = () => {
    navigate('/dashboard');
  };

  const navigateToPage = (page) => {
    setCurrentPage(page);
  };

  return (
    <div className="user-container">
      {/* Left Sidebar */}
      <div className="sidebar">
        <div className="logo-container">
          <img src={trashcashLogo} alt="TrashCash Campus Logo" className="logo" />
        </div>
        
        <nav className="sidebar-nav">
          <div className="nav-item" onClick={navigateToDashboard}>
            <span className="nav-icon">🏠</span>
            <span className="nav-text">Dashboard</span>
          </div>
          
          <div className="nav-item active">
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
        <h1 className="page-title">Users</h1>
        
        {/* Filters and Search */}
        <div className="filters-container">
          <div className="filters">
            <div className="filter-dropdown">
              <button className="filter-button">
                Action <span className="dropdown-arrow">▼</span>
              </button>
            </div>
            
            <div className="filter-dropdown">
              <button className="filter-button">
                Date <span className="dropdown-arrow">▼</span>
              </button>
            </div>
          </div>
          
          <div className="search-container">
            <input 
              type="text" 
              placeholder="Search" 
              className="search-input"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <span className="search-icon">🔍</span>
            <button className="filter-toggle">≡</button>
          </div>
        </div>
        
        {/* Users Table */}
        <div className="users-table-container">
          <div className="table-header">
            <div className="header-tabs">
              <h2 className="tab active">All Members</h2>
              <h2 className="tab active-tab">Active Members</h2>
            </div>
            
            <div className="table-controls">
              <div className="table-search">
                <span className="search-icon-small">🔍</span>
                <input 
                  type="text" 
                  placeholder="Search" 
                  className="table-search-input"
                />
              </div>
              
              <div className="sort-dropdown">
                Sort by: {sortBy} <span className="dropdown-arrow">▼</span>
              </div>
            </div>
          </div>
          
          <table className="users-table">
            <thead>
              <tr>
                <th>Customer Name</th>
                <th>Course</th>
                <th>Phone Number</th>
                <th>Email</th>
                <th>Country</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {userData.map((user) => (
                <tr key={user.id}>
                  <td>{user.name}</td>
                  <td>{user.course}</td>
                  <td>{user.phone}</td>
                  <td>{user.email}</td>
                  <td>{user.country}</td>
                  <td>
                    <span className={`status-badge ${user.status.toLowerCase()}`}>
                      {user.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          
          {/* Pagination */}
          <div className="pagination">
            <div className="pagination-info">
              Showing data 1 to 8 of {totalEntries} entries
            </div>
            
            <div className="pagination-controls">
              <button className="pagination-arrow" onClick={() => navigateToPage(Math.max(1, currentPage - 1))}>
                ◄
              </button>
              
              <button className={`pagination-number ${currentPage === 1 ? 'active' : ''}`} onClick={() => navigateToPage(1)}>
                1
              </button>
              
              <button className={`pagination-number ${currentPage === 2 ? 'active' : ''}`} onClick={() => navigateToPage(2)}>
                2
              </button>
              
              <button className={`pagination-number ${currentPage === 3 ? 'active' : ''}`} onClick={() => navigateToPage(3)}>
                3
              </button>
              
              <button className={`pagination-number ${currentPage === 4 ? 'active' : ''}`} onClick={() => navigateToPage(4)}>
                4
              </button>
              
              <span className="pagination-ellipsis">...</span>
              
              <button className={`pagination-number ${currentPage === totalPages ? 'active' : ''}`} onClick={() => navigateToPage(totalPages)}>
                {totalPages}
              </button>
              
              <button className="pagination-arrow" onClick={() => navigateToPage(Math.min(totalPages, currentPage + 1))}>
                ►
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default User;