import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../firebase';
import { collection, getDocs, doc, updateDoc, getDoc, addDoc, serverTimestamp } from 'firebase/firestore';
import Navigation from '../components/Navigation';
import './Rewards.css';

const Rewards = () => {
  const { currentUser } = useAuth();
  const [rewards, setRewards] = useState([]);
  const [userPoints, setUserPoints] = useState(0);
  const [redeemHistory, setRedeemHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState('all');
  const [showRedeemModal, setShowRedeemModal] = useState(false);
  const [selectedReward, setSelectedReward] = useState(null);
  const [redeeming, setRedeeming] = useState(false);

  // Categories
  const categories = [
    { id: 'all', name: 'All Rewards' },
    { id: 'campus', name: 'Campus Services' },
    { id: 'food', name: 'Food & Beverages' },
    { id: 'merchandise', name: 'Merchandise' },
    { id: 'experiences', name: 'Experiences' }
  ];

  useEffect(() => {
    const fetchData = async () => {
      try {
        if (!currentUser) return;

        // Fetch user points
        const userRef = doc(db, 'users', currentUser.uid);
        const userSnap = await getDoc(userRef);
        
        if (userSnap.exists()) {
          setUserPoints(userSnap.data().totalPoints || 0);
        }

        // Fetch rewards
        const rewardsRef = collection(db, 'rewards');
        const rewardsSnap = await getDocs(rewardsRef);
        
        const rewardsList = rewardsSnap.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        
        setRewards(rewardsList);

        // Fetch redemption history
        const historyRef = collection(db, 'redemptions');
        const historyQuery = getDocs(collection(db, 'redemptions'));
        const historySnap = await historyQuery;
        
        const historyList = historySnap.docs
          .filter(doc => doc.data().userId === currentUser.uid)
          .map(doc => ({
            id: doc.id,
            ...doc.data()
          }));
        
        setRedeemHistory(historyList);
      } catch (error) {
        console.error('Error fetching rewards data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [currentUser]);

  const handleCategoryChange = (categoryId) => {
    setActiveCategory(categoryId);
  };

  const handleRedeemClick = (reward) => {
    setSelectedReward(reward);
    setShowRedeemModal(true);
  };

  const closeRedeemModal = () => {
    setShowRedeemModal(false);
    setSelectedReward(null);
  };

  const confirmRedeem = async () => {
    if (!selectedReward || !currentUser) return;
    
    setRedeeming(true);
    
    try {
      // Check if user has enough points
      if (userPoints < selectedReward.pointsCost) {
        alert("You don't have enough points to redeem this reward.");
        setRedeeming(false);
        return;
      }
      
      // Update user points
      const userRef = doc(db, 'users', currentUser.uid);
      await updateDoc(userRef, {
        totalPoints: userPoints - selectedReward.pointsCost
      });
      
      // Add to redemption history
      await addDoc(collection(db, 'redemptions'), {
        userId: currentUser.uid,
        rewardId: selectedReward.id,
        rewardName: selectedReward.title,
        pointsCost: selectedReward.pointsCost,
        timestamp: serverTimestamp(),
        status: 'pending' // pending, completed, cancelled
      });
      
      // Update local user points
      setUserPoints(userPoints - selectedReward.pointsCost);
      
      // Show success message
      alert(`You have successfully redeemed ${selectedReward.title}!`);
      
      // Close modal
      closeRedeemModal();
      
      // Fetch updated redemption history
      const historyQuery = getDocs(collection(db, 'redemptions'));
      const historySnap = await historyQuery;
      
      const historyList = historySnap.docs
        .filter(doc => doc.data().userId === currentUser.uid)
        .map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
      
      setRedeemHistory(historyList);
      
    } catch (error) {
      console.error('Error redeeming reward:', error);
      alert('An error occurred while redeeming the reward. Please try again.');
    } finally {
      setRedeeming(false);
    }
  };

  // Format timestamp to readable date
  const formatDate = (timestamp) => {
    if (!timestamp) return 'Processing';
    
    const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  // Filter rewards by category
  const filteredRewards = activeCategory === 'all'
    ? rewards
    : rewards.filter(reward => reward.category === activeCategory);

  return (
    <div className="rewards-container">
      <Navigation />
      
      <main className="rewards-content">
        <h1 className="rewards-title">Rewards</h1>
        
        {loading ? (
          <div className="loading">Loading rewards...</div>
        ) : (
          <>
            {/* User points display */}
            <div className="user-points-display">
              <div className="points-value">{userPoints}</div>
              <div className="points-label">Available Points</div>
            </div>
            
            {/* Categories */}
            <div className="categories-tabs">
              {categories.map(category => (
                <button
                  key={category.id}
                  className={`category-tab ${activeCategory === category.id ? 'active' : ''}`}
                  onClick={() => handleCategoryChange(category.id)}
                >
                  {category.name}
                </button>
              ))}
            </div>
            
            {/* Rewards grid */}
            <div className="rewards-grid">
              {filteredRewards.length === 0 ? (
                <div className="no-rewards">
                  <p>No rewards available in this category yet.</p>
                </div>
              ) : (
                filteredRewards.map(reward => (
                  <div key={reward.id} className="reward-card">
                    <div 
                      className="reward-image" 
                      style={{ backgroundColor: reward.backgroundColor || '#f0f0f0' }}
                    >
                      <span className="reward-icon">{reward.icon || '🎁'}</span>
                    </div>
                    <div className="reward-content">
                      <h3 className="reward-title">{reward.title}</h3>
                      <p className="reward-description">{reward.description}</p>
                      <div className="reward-footer">
                        <span className="reward-points">{reward.pointsCost} pts</span>
                        <button 
                          className="redeem-button"
                          onClick={() => handleRedeemClick(reward)}
                          disabled={userPoints < reward.pointsCost}
                        >
                          {userPoints < reward.pointsCost ? 'Not Enough Points' : 'Redeem'}
                        </button>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
            
            {/* Redemption History */}
            <div className="redemption-history">
              <h2>Redemption History</h2>
              
              {redeemHistory.length === 0 ? (
                <div className="no-history">
                  <p>You haven't redeemed any rewards yet.</p>
                </div>
              ) : (
                <table className="history-table">
                  <thead>
                    <tr>
                      <th>Reward</th>
                      <th>Points</th>
                      <th>Date</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {redeemHistory.map(item => (
                      <tr key={item.id}>
                        <td>{item.rewardName}</td>
                        <td>{item.pointsCost}</td>
                        <td>{formatDate(item.timestamp)}</td>
                        <td>
                          <span className={`status-badge ${item.status}`}>
                            {item.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </main>
      
      {/* Redeem Confirmation Modal */}
      {showRedeemModal && selectedReward && (
        <div className="modal-overlay">
          <div className="redeem-modal">
            <h2>Confirm Redemption</h2>
            <p>Are you sure you want to redeem the following reward?</p>
            
            <div className="reward-details">
              <span className="modal-reward-icon">{selectedReward.icon || '🎁'}</span>
              <h3>{selectedReward.title}</h3>
              <p>{selectedReward.description}</p>
              <div className="cost-display">
                <span className="cost-label">Cost:</span>
                <span className="cost-value">{selectedReward.pointsCost} points</span>
              </div>
              <div className="points-after">
                <span>Your points after redemption:</span>
                <span className="points-value">{userPoints - selectedReward.pointsCost}</span>
              </div>
            </div>
            
            <div className="modal-actions">
              <button 
                className="cancel-button"
                onClick={closeRedeemModal}
                disabled={redeeming}
              >
                Cancel
              </button>
              <button 
                className="confirm-button"
                onClick={confirmRedeem}
                disabled={redeeming}
              >
                {redeeming ? 'Processing...' : 'Confirm Redemption'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Rewards; 