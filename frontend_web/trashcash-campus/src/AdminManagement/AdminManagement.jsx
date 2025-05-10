import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../firebase';
import { collection, getDocs, doc, updateDoc, query, orderBy, where } from 'firebase/firestore';
import Navigation from '../components/Navigation';
import './AdminManagement.css';

const AdminManagement = () => {
  const { currentUser, isAdmin } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // Fetch all users
  useEffect(() => {
    const fetchUsers = async () => {
      try {
        setLoading(true);
        const usersQuery = query(
          collection(db, 'users'),
          orderBy('email')
        );
        const snapshot = await getDocs(usersQuery);
        
        const usersData = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        
        setUsers(usersData);
      } catch (error) {
        console.error('Error fetching users:', error);
        setErrorMessage('Failed to load users. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchUsers();
  }, []);

  // Handle search input change
  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  // Filter users based on search term
  const filteredUsers = users.filter(user => {
    const email = user.email?.toLowerCase() || '';
    const displayName = user.displayName?.toLowerCase() || '';
    const search = searchTerm.toLowerCase();
    
    return email.includes(search) || displayName.includes(search);
  });

  // Make a user admin
  const makeAdmin = async (userId, email) => {
    try {
      const userRef = doc(db, 'users', userId);
      await updateDoc(userRef, {
        role: 'admin'
      });
      
      // Update local state
      setUsers(users.map(user => 
        user.id === userId ? { ...user, role: 'admin' } : user
      ));
      
      setSuccessMessage(`${email} is now an admin`);
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (error) {
      console.error('Error making user admin:', error);
      setErrorMessage(`Failed to make ${email} an admin. Please try again.`);
      setTimeout(() => setErrorMessage(''), 3000);
    }
  };

  // Remove admin privileges
  const removeAdmin = async (userId, email) => {
    // Don't allow removing admin from default admin account
    if (email === 'drewadrein.odilao@cit.edu') {
      setErrorMessage('Cannot remove admin rights from the default admin account');
      setTimeout(() => setErrorMessage(''), 3000);
      return;
    }
    
    // Don't allow removing admin from current user
    if (userId === currentUser.uid) {
      setErrorMessage('You cannot remove your own admin rights');
      setTimeout(() => setErrorMessage(''), 3000);
      return;
    }
    
    try {
      const userRef = doc(db, 'users', userId);
      await updateDoc(userRef, {
        role: 'user'
      });
      
      // Update local state
      setUsers(users.map(user => 
        user.id === userId ? { ...user, role: 'user' } : user
      ));
      
      setSuccessMessage(`Admin rights removed from ${email}`);
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (error) {
      console.error('Error removing admin rights:', error);
      setErrorMessage(`Failed to remove admin rights from ${email}. Please try again.`);
      setTimeout(() => setErrorMessage(''), 3000);
    }
  };

  return (
    <div className="admin-management-container">
      <Navigation />
      
      <main className="admin-management-content">
        <h1 className="admin-title">Admin Management</h1>
        
        {!isAdmin && (
          <div className="admin-error">
            You don't have permission to access this page.
          </div>
        )}
        
        {isAdmin && (
          <>
            {successMessage && (
              <div className="success-message">
                {successMessage}
              </div>
            )}
            
            {errorMessage && (
              <div className="error-message">
                {errorMessage}
              </div>
            )}
            
            <div className="admin-controls">
              <div className="search-container">
                <input
                  type="text"
                  placeholder="Search users..."
                  value={searchTerm}
                  onChange={handleSearchChange}
                  className="search-input"
                />
              </div>
            </div>
            
            {loading ? (
              <div className="loading">Loading users...</div>
            ) : (
              <div className="users-table-container">
                <table className="users-table">
                  <thead>
                    <tr>
                      <th>Email</th>
                      <th>Name</th>
                      <th>Role</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.length === 0 ? (
                      <tr>
                        <td colSpan="4" className="no-users">No users found</td>
                      </tr>
                    ) : (
                      filteredUsers.map(user => (
                        <tr key={user.id} className={user.email === 'drewadrein.odilao@cit.edu' ? 'default-admin' : ''}>
                          <td>{user.email}</td>
                          <td>{user.displayName || '-'}</td>
                          <td>
                            <span className={`role-badge ${user.role || 'user'}`}>
                              {user.role || 'user'}
                              {user.email === 'drewadrein.odilao@cit.edu' && ' (default)'}
                            </span>
                          </td>
                          <td>
                            {user.role === 'admin' ? (
                              <button
                                className="remove-admin-button"
                                onClick={() => removeAdmin(user.id, user.email)}
                                disabled={user.email === 'drewadrein.odilao@cit.edu'}
                                title={user.email === 'drewadrein.odilao@cit.edu' ? 'Cannot remove admin rights from default admin' : ''}
                              >
                                Remove Admin
                              </button>
                            ) : (
                              <button
                                className="make-admin-button"
                                onClick={() => makeAdmin(user.id, user.email)}
                              >
                                Make Admin
                              </button>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
};

export default AdminManagement; 