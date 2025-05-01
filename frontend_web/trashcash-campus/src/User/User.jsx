import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../firebase';
import { collection, getDocs, doc, updateDoc, deleteDoc, query, orderBy, setDoc } from 'firebase/firestore';
import Navigation from '../components/Navigation';
import './User.css';
import trashcashLogo from '../assets/trashcash-logo.png';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPenToSquare, faUserPlus } from '@fortawesome/free-solid-svg-icons';

const User = () => {
  const { currentUser, isAdmin } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [editingUser, setEditingUser] = useState(null);
  const [sortBy, setSortBy] = useState('Newest');
  const [currentPage, setCurrentPage] = useState(1);
  const [showCredentialModal, setShowCredentialModal] = useState(false);
  const [showEmailForm, setShowEmailForm] = useState(false);
  const [selectedEmail, setSelectedEmail] = useState('');
  const [emailMessage, setEmailMessage] = useState('');
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [newUser, setNewUser] = useState({
    email: '',
    displayName: '',
    role: 'user',
    totalPoints: 0
  });
  const [addUserError, setAddUserError] = useState('');
  
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

  useEffect(() => {
    const fetchUsers = async () => {
      if (!currentUser || !isAdmin) return;
      
      try {
        setLoading(true);
        const usersRef = collection(db, 'users');
        const q = query(usersRef, orderBy('email', 'asc'));
        const querySnapshot = await getDocs(q);
        
        const usersList = querySnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        
        setUsers(usersList);
      } catch (error) {
        console.error('Error fetching users:', error);
      } finally {
        setLoading(false);
      }
    };
    
    fetchUsers();
  }, [currentUser, isAdmin]);

  const handleSearch = (e) => {
    setSearchTerm(e.target.value);
  };

  const filteredUsers = users.filter(user => {
    const lowerSearchTerm = searchTerm.toLowerCase();
    return (
      user.email?.toLowerCase().includes(lowerSearchTerm) ||
      user.displayName?.toLowerCase().includes(lowerSearchTerm) ||
      user.uid?.toLowerCase().includes(lowerSearchTerm)
    );
  });

  const handleEditClick = (user) => {
    setEditingUser({ ...user });
  };

  const handleEditCancel = () => {
    setEditingUser(null);
  };

  const handleEditSave = async () => {
    if (!editingUser) return;
    
    try {
      const userRef = doc(db, 'users', editingUser.id);
      await updateDoc(userRef, {
        role: editingUser.role,
        totalPoints: Number(editingUser.totalPoints) || 0,
        ...(editingUser.displayName ? { displayName: editingUser.displayName } : {})
      });
      
      // Update user in the local state
      setUsers(users.map(user => 
        user.id === editingUser.id ? { ...user, ...editingUser } : user
      ));
      
      setEditingUser(null);
    } catch (error) {
      console.error('Error updating user:', error);
      alert('Error updating user. Please try again.');
    }
  };

  const handleRoleChange = (e) => {
    setEditingUser({ ...editingUser, role: e.target.value });
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setEditingUser({ ...editingUser, [name]: value });
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'Unknown';
    
    try {
      const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    } catch (error) {
      return 'Invalid date';
    }
  };

  const navigateToDashboard = () => {
    // Implement the logic to navigate to the dashboard
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

  const openAddUserModal = () => {
    setShowAddUserModal(true);
  };

  const closeAddUserModal = () => {
    setShowAddUserModal(false);
    setNewUser({
      email: '',
      displayName: '',
      role: 'user',
      totalPoints: 0
    });
    setAddUserError('');
  };

  const handleAddUser = async () => {
    if (!newUser.email || !newUser.displayName) {
      setAddUserError('Email and Display Name are required');
      return;
    }
    
    try {
      // Generate a unique ID based on timestamp
      const userId = `user_${Date.now()}`;
      
      // Create a new user document
      const userRef = doc(db, 'users', userId);
      await setDoc(userRef, {
        id: userId,
        email: newUser.email,
        displayName: newUser.displayName,
        role: newUser.role,
        totalPoints: Number(newUser.totalPoints) || 0,
        createdAt: new Date()
      });
      
      // Add user to local state
      const newUserWithId = {
        ...newUser,
        id: userId,
        createdAt: new Date()
      };
      
      setUsers([...users, newUserWithId]);
      
      // Close modal and reset form
      closeAddUserModal();
    } catch (error) {
      console.error('Error adding user:', error);
      setAddUserError('Error adding user: ' + error.message);
    }
  };

  return (
    <div className="user-management-container">
      <Navigation />
      
      <main className="user-management-content">
        <h1 className="user-title">User Management</h1>
        
        {loading ? (
          <div className="loading">Loading users...</div>
        ) : (
          <>
            <div className="user-controls">
              <div className="search-container">
                <input
                  type="text"
                  placeholder="Search users..."
                  value={searchTerm}
                  onChange={handleSearch}
                  className="search-input"
                />
              </div>
              
              <div className="user-stats">
                <div className="stat">
                  <span className="stat-label">Total Users:</span>
                  <span className="stat-value">{users.length}</span>
                </div>
                <div className="stat">
                  <span className="stat-label">Admins:</span>
                  <span className="stat-value">{users.filter(user => user.role === 'admin').length}</span>
                </div>
                <div className="stat">
                  <span className="stat-label">Regular Users:</span>
                  <span className="stat-value">{users.filter(user => user.role !== 'admin').length}</span>
                </div>
              </div>
              
              <button 
                className="add-user-button" 
                onClick={openAddUserModal}
                title="Add New User"
              >
                <FontAwesomeIcon icon={faUserPlus} /> Add User
              </button>
            </div>
            
            <div className="users-table-container">
              <table className="users-table">
                <thead>
                  <tr>
                    <th>Email</th>
                    <th>Name</th>
                    <th>Role</th>
                    <th>Points</th>
                    <th>Created</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.length === 0 ? (
                    <tr>
                      <td colSpan="6" className="no-users">No users found</td>
                    </tr>
                  ) : (
                    filteredUsers.map(user => (
                      <tr key={user.id}>
                        <td>{user.email}</td>
                        <td>{user.displayName || '-'}</td>
                        <td>
                          <span className={`role-badge ${user.role || 'user'}`}>
                            {user.role || 'user'}
                          </span>
                        </td>
                        <td>{user.totalPoints || 0}</td>
                        <td>{formatDate(user.createdAt)}</td>
                        <td>
                          <button
                            className="edit-button"
                            onClick={() => handleEditClick(user)}
                            title="Edit User"
                          >
                            <FontAwesomeIcon icon={faPenToSquare} />
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            
            {/* Edit User Modal */}
            {editingUser && (
              <div className="modal-overlay">
                <div className="edit-modal">
                  <h2>Edit User</h2>
                  
                  <div className="form-group">
                    <label>Email:</label>
                    <input
                      type="text"
                      value={editingUser.email}
                      disabled
                      className="form-input"
                    />
                  </div>
                  
                  <div className="form-group">
                    <label>Display Name:</label>
                    <input
                      type="text"
                      name="displayName"
                      value={editingUser.displayName || ''}
                      onChange={handleInputChange}
                      className="form-input"
                      placeholder="Display Name"
                    />
                  </div>
                  
                  <div className="form-group">
                    <label>Role:</label>
                    <select
                      value={editingUser.role || 'user'}
                      onChange={handleRoleChange}
                      className="form-select"
                    >
                      <option value="user">User</option>
                      <option value="admin">Admin</option>
                    </select>
                  </div>
                  
                  <div className="form-group">
                    <label>Points:</label>
                    <input
                      type="number"
                      name="totalPoints"
                      value={editingUser.totalPoints || 0}
                      onChange={handleInputChange}
                      className="form-input"
                      min="0"
                    />
                  </div>
                  
                  <div className="modal-actions">
                    <div className="button-wrapper">
                      <button
                        onClick={handleEditCancel}
                        type="button"
                        style={{
                          width: '100%',
                          height: '50px',
                          backgroundColor: '#e8e8e8',
                          color: '#333',
                          border: 'none',
                          borderRadius: '4px',
                          fontSize: '1rem',
                          fontWeight: '500',
                          cursor: 'pointer'
                        }}
                      >
                        Cancel
                      </button>
                    </div>
                    <div className="button-wrapper">
                      <button
                        onClick={handleEditSave}
                        type="button"
                        style={{
                          width: '100%',
                          height: '50px',
                          backgroundColor: '#1a5336',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          fontSize: '1rem',
                          fontWeight: '500',
                          cursor: 'pointer'
                        }}
                      >
                        Save
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </>
        )}
        
        {/* Add User Modal */}
        {showAddUserModal && (
          <div className="modal-overlay">
            <div className="edit-modal add-user-modal">
              <h2>Add New User</h2>
              
              {addUserError && (
                <div className="error-message">{addUserError}</div>
              )}
              
              <div className="form-group">
                <label>Email:</label>
                <input
                  type="email"
                  value={newUser.email}
                  onChange={(e) => setNewUser({...newUser, email: e.target.value})}
                  className="form-input"
                  placeholder="user@example.com"
                />
              </div>
              
              <div className="form-group">
                <label>Display Name:</label>
                <input
                  type="text"
                  value={newUser.displayName}
                  onChange={(e) => setNewUser({...newUser, displayName: e.target.value})}
                  className="form-input"
                  placeholder="John Doe"
                />
              </div>
              
              <div className="form-group">
                <label>Role:</label>
                <select
                  value={newUser.role}
                  onChange={(e) => setNewUser({...newUser, role: e.target.value})}
                  className="form-select"
                >
                  <option value="user">User</option>
                  <option value="admin">Admin</option>
                </select>
              </div>
              
              <div className="form-group">
                <label>Points:</label>
                <input
                  type="number"
                  value={newUser.totalPoints}
                  onChange={(e) => setNewUser({...newUser, totalPoints: e.target.value})}
                  className="form-input"
                  min="0"
                />
              </div>
              
              <div className="modal-actions">
                <div className="button-wrapper">
                  <button
                    onClick={closeAddUserModal}
                    type="button"
                    className="cancel-button"
                  >
                    Cancel
                  </button>
                </div>
                <div className="button-wrapper">
                  <button
                    onClick={handleAddUser}
                    type="button"
                    className="save-button"
                  >
                    Add User
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default User;