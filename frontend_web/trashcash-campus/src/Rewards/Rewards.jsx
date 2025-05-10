import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../firebase';
import { collection, getDocs, doc, updateDoc, getDoc, addDoc, serverTimestamp, deleteDoc } from 'firebase/firestore';
import Navigation from '../components/Navigation';
import './Rewards.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faEdit, faTrash, faSearch, faCoffee, faPizzaSlice, faBook, faTshirt, faUtensils, faGift } from '@fortawesome/free-solid-svg-icons';

const Rewards = () => {
  const { currentUser } = useAuth();
  const [rewards, setRewards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [editingReward, setEditingReward] = useState(null);
  const [showAddRewardModal, setShowAddRewardModal] = useState(false);
  const [newReward, setNewReward] = useState({
    title: '',
    description: '',
    pointsCost: 0,
    category: 'campus',
    icon: 'gift',
    backgroundColor: '#f0f0f0'
  });

  useEffect(() => {
    const fetchRewards = async () => {
      try {
        // Fetch all rewards
        const rewardsRef = collection(db, 'rewards');
        const rewardsSnap = await getDocs(rewardsRef);
        
        const rewardsList = rewardsSnap.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        
        setRewards(rewardsList);
      } catch (error) {
        console.error('Error fetching rewards:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchRewards();
  }, []);

  // Helper function to get the correct icon component
  const getIconComponent = (iconName) => {
    switch (iconName) {
      case 'coffee':
        return faCoffee;
      case 'pizza':
        return faPizzaSlice;
      case 'book':
        return faBook;
      case 'tshirt':
        return faTshirt;
      case 'utensils':
        return faUtensils;
      case 'gift':
      default:
        return faGift;
    }
  };

  const handleDeleteReward = async (rewardId) => {
    if (window.confirm('Are you sure you want to delete this reward?')) {
      try {
        await deleteDoc(doc(db, 'rewards', rewardId));
        
        // Update local state
        setRewards(rewards.filter(reward => reward.id !== rewardId));
        
        alert('Reward deleted successfully');
      } catch (error) {
        console.error('Error deleting reward:', error);
        alert('Failed to delete reward');
      }
    }
  };

  const handleEditReward = (reward) => {
    setEditingReward({...reward});
  };

  const saveEditedReward = async () => {
    if (!editingReward) return;
    
    try {
      await updateDoc(doc(db, 'rewards', editingReward.id), {
        title: editingReward.title,
        description: editingReward.description,
        pointsCost: Number(editingReward.pointsCost),
        category: editingReward.category,
        icon: editingReward.icon,
        backgroundColor: editingReward.backgroundColor
      });
      
      // Update local state
      setRewards(rewards.map(reward => 
        reward.id === editingReward.id ? editingReward : reward
      ));
      
      // Close modal
      setEditingReward(null);
      
      alert('Reward updated successfully');
    } catch (error) {
      console.error('Error updating reward:', error);
      alert('Failed to update reward');
    }
  };

  const handleAddReward = async () => {
    try {
      // Validate inputs
      if (!newReward.title || !newReward.description || newReward.pointsCost <= 0) {
        alert('Please fill in all required fields');
        return;
      }
      
      // Add new reward to Firestore
      const docRef = await addDoc(collection(db, 'rewards'), {
        title: newReward.title,
        description: newReward.description,
        pointsCost: Number(newReward.pointsCost),
        category: newReward.category,
        icon: newReward.icon,
        backgroundColor: newReward.backgroundColor,
        createdAt: serverTimestamp()
      });
      
      // Update local state
      const newRewardWithId = {
        id: docRef.id,
        ...newReward
      };
      
      setRewards([...rewards, newRewardWithId]);
      
      // Reset form and close modal
      setNewReward({
        title: '',
        description: '',
        pointsCost: 0,
        category: 'campus',
        icon: 'gift',
        backgroundColor: '#f0f0f0'
      });
      
      setShowAddRewardModal(false);
      
      alert('Reward added successfully');
    } catch (error) {
      console.error('Error adding reward:', error);
      alert('Failed to add reward');
    }
  };

  // Filter rewards by search query
  const filteredRewards = rewards.filter(reward => 
    reward.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
    reward.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
    reward.category.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Count rewards by category
  const categoryCounts = {
    campus: rewards.filter(r => r.category === 'campus').length,
    food: rewards.filter(r => r.category === 'food').length,
    merchandise: rewards.filter(r => r.category === 'merchandise').length,
    experiences: rewards.filter(r => r.category === 'experiences').length
  };

  return (
    <div className="rewards-container">
      <Navigation />
      
      <main className="rewards-content">
        <div className="rewards-header">
          <h1 className="rewards-title">Rewards Management</h1>
          <button 
            className="add-reward-button"
            onClick={() => setShowAddRewardModal(true)}
          >
            <FontAwesomeIcon icon={faPlus} /> Add Reward
          </button>
        </div>

        <div className="search-container">
          <input
            type="text"
            placeholder="Search rewards..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
        </div>

        <div className="rewards-stats">
          <div className="stat-box">
            <span className="stat-value">{rewards.length}</span>
            <span className="stat-label">Total Rewards</span>
          </div>
          <div className="stat-box">
            <span className="stat-value">{categoryCounts.campus}</span>
            <span className="stat-label">Campus</span>
          </div>
          <div className="stat-box">
            <span className="stat-value">{categoryCounts.food}</span>
            <span className="stat-label">Food</span>
          </div>
          <div className="stat-box">
            <span className="stat-value">{categoryCounts.merchandise}</span>
            <span className="stat-label">Merchandise</span>
          </div>
        </div>
        
        {loading ? (
          <div className="loading">Loading rewards...</div>
        ) : (
          <div className="rewards-table-container">
            <table className="rewards-table">
              <thead>
                <tr>
                  <th>Icon</th>
                  <th>Title</th>
                  <th>Description</th>
                  <th>Points</th>
                  <th>Category</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
              {filteredRewards.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="no-rewards">
                      {searchQuery ? 'No rewards found matching your search.' : 'No rewards available. Add a new reward to get started.'}
                    </td>
                  </tr>
              ) : (
                filteredRewards.map(reward => (
                    <tr key={reward.id}>
                      <td>
                    <div 
                          className="reward-icon-cell" 
                      style={{ backgroundColor: reward.backgroundColor || '#f0f0f0' }}
                    >
                          <FontAwesomeIcon icon={getIconComponent(reward.icon)} />
                    </div>
                      </td>
                      <td>{reward.title}</td>
                      <td className="description-cell">{reward.description}</td>
                      <td>{reward.pointsCost}</td>
                      <td>{reward.category}</td>
                      <td className="actions-cell">
                        <button 
                          className="action-button edit-button"
                          onClick={() => handleEditReward(reward)}
                        >
                          <FontAwesomeIcon icon={faEdit} />
                        </button>
                        <button 
                          className="action-button delete-button"
                          onClick={() => handleDeleteReward(reward.id)}
                        >
                          <FontAwesomeIcon icon={faTrash} />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
                  </tbody>
                </table>
            </div>
        )}
      </main>
      
      {/* Edit Reward Modal */}
      {editingReward && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Edit Reward</h2>
            
            <div className="form-group">
              <label>Title</label>
              <input 
                type="text" 
                value={editingReward.title} 
                onChange={(e) => setEditingReward({...editingReward, title: e.target.value})}
                placeholder="Enter reward title"
              />
            </div>
            
            <div className="form-group">
              <label>Description</label>
              <textarea 
                value={editingReward.description} 
                onChange={(e) => setEditingReward({...editingReward, description: e.target.value})}
                placeholder="Enter reward description"
                rows={4}
              />
            </div>
            
            <div className="form-group">
              <label>Points Cost</label>
              <input 
                type="number" 
                value={editingReward.pointsCost} 
                onChange={(e) => setEditingReward({...editingReward, pointsCost: e.target.value})}
                min="0"
                placeholder="0"
              />
            </div>
            
            <div className="form-group">
              <label>Category</label>
              <select 
                value={editingReward.category} 
                onChange={(e) => setEditingReward({...editingReward, category: e.target.value})}
              >
                <option value="campus">Campus Services</option>
                <option value="food">Food & Beverages</option>
                <option value="merchandise">Merchandise</option>
                <option value="experiences">Experiences</option>
              </select>
              </div>
            
            <div className="form-group">
              <label>Icon</label>
              <select 
                value={editingReward.icon} 
                onChange={(e) => setEditingReward({...editingReward, icon: e.target.value})}
              >
                <option value="gift">Gift</option>
                <option value="coffee">Coffee</option>
                <option value="pizza">Pizza</option>
                <option value="book">Book</option>
                <option value="tshirt">T-shirt</option>
                <option value="utensils">Food</option>
              </select>
              </div>
            
            <div className="form-group">
              <label>Background Color</label>
              <input 
                type="color" 
                value={editingReward.backgroundColor} 
                onChange={(e) => setEditingReward({...editingReward, backgroundColor: e.target.value})}
              />
            </div>
            
            <div className="modal-actions">
              <button onClick={() => setEditingReward(null)}>Cancel</button>
              <button onClick={saveEditedReward} className="save-button">Save Changes</button>
            </div>
          </div>
        </div>
      )}
      
      {/* Add Reward Modal */}
      {showAddRewardModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Add New Reward</h2>
            
            <div className="form-group">
              <label>Title</label>
              <input 
                type="text" 
                value={newReward.title} 
                onChange={(e) => setNewReward({...newReward, title: e.target.value})}
                placeholder="Enter reward title"
              />
            </div>
            
            <div className="form-group">
              <label>Description</label>
              <textarea 
                value={newReward.description} 
                onChange={(e) => setNewReward({...newReward, description: e.target.value})}
                placeholder="Enter reward description"
                rows={4}
              />
            </div>
            
            <div className="form-group">
              <label>Points Cost</label>
              <input 
                type="number" 
                value={newReward.pointsCost} 
                onChange={(e) => setNewReward({...newReward, pointsCost: e.target.value})}
                min="0"
                placeholder="0"
              />
            </div>
            
            <div className="form-group">
              <label>Category</label>
              <select 
                value={newReward.category} 
                onChange={(e) => setNewReward({...newReward, category: e.target.value})}
              >
                <option value="campus">Campus Services</option>
                <option value="food">Food & Beverages</option>
                <option value="merchandise">Merchandise</option>
                <option value="experiences">Experiences</option>
              </select>
            </div>
            
            <div className="form-group">
              <label>Icon</label>
              <select 
                value={newReward.icon} 
                onChange={(e) => setNewReward({...newReward, icon: e.target.value})}
              >
                <option value="gift">Gift</option>
                <option value="coffee">Coffee</option>
                <option value="pizza">Pizza</option>
                <option value="book">Book</option>
                <option value="tshirt">T-shirt</option>
                <option value="utensils">Food</option>
              </select>
            </div>
            
            <div className="form-group">
              <label>Background Color</label>
              <input 
                type="color" 
                value={newReward.backgroundColor} 
                onChange={(e) => setNewReward({...newReward, backgroundColor: e.target.value})}
              />
            </div>
            
            <div className="modal-actions">
              <button onClick={() => setShowAddRewardModal(false)}>Cancel</button>
              <button onClick={handleAddReward} className="save-button">Add Reward</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Rewards; 