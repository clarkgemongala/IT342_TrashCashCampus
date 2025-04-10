import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './User.css';
import trashcashLogo from '../assets/trashcash-logo.png';

const User = () => {
  const navigate = useNavigate();
  const [sortBy, setSortBy] = useState('Newest');
  const [currentPage, setCurrentPage] = useState(1);
  const [showCredentialModal, setShowCredentialModal] = useState(false);
  const [showEmailForm, setShowEmailForm] = useState(false);
  const [selectedEmail, setSelectedEmail] = useState('');
  const [emailMessage, setEmailMessage] = useState('');
  
  // Mock data for credential requests
  const credentialRequests = [
    { id: 1, email: 'john.doe@cit.edu', date: '2025-04-08', status: 'Pending' },
    { id: 2, email: 'jane.smith@cit.edu', date: '2025-04-09', status: 'Pending' },
    { id: 3, email: 'robert.johnson@cit.edu', date: '2025-04-10', status: 'Pending' },
    { id: 4, email: 'emily.williams@cit.edu', date: '2025-04-10', status: 'Pending' },
    { id: 5, email: 'michael.brown@cit.edu', date: '2025-04-11', status: 'Pending' },
  ];
  
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

  const openCredentialModal = () => {
    setShowCredentialModal(true);
  };

  const closeCredentialModal = () => {
    setShowCredentialModal(false);
  };

  const openEmailForm = (email) => {
    setSelectedEmail(email);
    setShowEmailForm(true);
  };

  const closeEmailForm = () => {
    setShowEmailForm(false);
    setEmailMessage('');
  };

  const handleEmailMessageChange = (e) => {
    setEmailMessage(e.target.value);
  };

  const sendEmail = () => {
    // Here you would typically integrate with an email API
    // For now, we'll just simulate the sending process
    console.log(`Sending email to ${selectedEmail}: ${emailMessage}`);
    alert(`Credentials sent to ${selectedEmail} successfully!`);
    closeEmailForm();
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
        
        {/* Filters and Credential Request Button */}
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
          
          <div className="credential-request-container">
            <button className="credential-request-button" onClick={openCredentialModal}>
              Credential Requests
            </button>
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

      {/* Credential Requests Modal */}
      {showCredentialModal && (
        <div className="modal-overlay">
          <div className="credential-modal">
            <div className="modal-header">
              <h2>Credential Requests</h2>
              <button className="close-button" onClick={closeCredentialModal}>×</button>
            </div>
            <div className="modal-content">
              <table className="credential-table">
                <thead>
                  <tr>
                    <th>Email</th>
                    <th>Date Requested</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {credentialRequests.map((request) => (
                    <tr key={request.id}>
                      <td>{request.email}</td>
                      <td>{request.date}</td>
                      <td>{request.status}</td>
                      <td>
                        <button 
                          className="send-credential-button"
                          onClick={() => openEmailForm(request.email)}
                        >
                          Send Credentials
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Email Form Modal */}
      {showEmailForm && (
        <div className="modal-overlay">
          <div className="email-form-modal">
          <div className="modal-header">
            <h2>Send Credentials</h2>
          <button className="close-button" onClick={closeEmailForm}>×</button>
      </div>
      <div className="modal-content">
        <div className="email-recipient">
          <strong>To:</strong> {selectedEmail}
        </div>
        <div className="email-form">
          <textarea
            className="email-message"
            placeholder="Enter login credentials or message here...

Email:
Password:"
            value={emailMessage}
            onChange={handleEmailMessageChange}
            rows={8}
          />
          <div className="email-actions">
            <button className="cancel-button" onClick={closeEmailForm}>Cancel</button>
            <button className="send-button" onClick={sendEmail}>Send</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default User;