import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../firebase';
import { collection, getDocs, addDoc, serverTimestamp } from 'firebase/firestore';
import Navigation from '../components/Navigation';
import QRScanner from '../components/QRScanner';
import './Bins.css';

const Bins = () => {
  const { currentUser } = useAuth();
  const [bins, setBins] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedBin, setSelectedBin] = useState(null);
  const [showTips, setShowTips] = useState(false);
  
  // State for camera access
  const [showScanner, setShowScanner] = useState(false);
  const [scanResult, setScanResult] = useState(null);
  const [processingWaste, setProcessingWaste] = useState(false);
  
  // Waste types
  const wasteTypes = [
    { id: 'paper', name: 'Paper', icon: '📄', color: '#4285F4' },
    { id: 'plastic', name: 'Plastic', icon: '🥤', color: '#EA4335' },
    { id: 'glass', name: 'Glass', icon: '🍶', color: '#FBBC05' },
    { id: 'metal', name: 'Metal', icon: '🥫', color: '#34A853' },
    { id: 'organic', name: 'Organic', icon: '🍎', color: '#8D6E63' },
  ];
  
  useEffect(() => {
    const fetchBins = async () => {
      try {
        const binsRef = collection(db, 'bins');
        const snapshot = await getDocs(binsRef);
        
        const binsList = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        
        setBins(binsList);
      } catch (error) {
        console.error('Error fetching bins:', error);
      } finally {
        setLoading(false);
      }
    };
    
    fetchBins();
  }, []);

  const handleBinSelect = (bin) => {
    setSelectedBin(bin);
    setShowTips(true);
    setShowScanner(false);
    setScanResult(null);
  };

  const handleScanClick = () => {
    setShowScanner(true);
    setShowTips(false);
  };

  const handleScanSuccess = async (data) => {
    try {
      setScanResult(data);
      
      // Find the bin from the scan result
      // In a real app, the QR would contain a bin ID or a JSON with bin info
      let scannedBin;
      
      if (typeof data === 'object' && data.binId) {
        // If QR code returns an object with binId
        scannedBin = bins.find(bin => bin.id === data.binId);
      } else if (typeof data === 'string') {
        // If QR code returns just a bin ID string
        scannedBin = bins.find(bin => bin.id === data);
      }
      
      if (scannedBin) {
        setSelectedBin(scannedBin);
        setShowTips(true);
      } else {
        // If bin not found, create a temporary bin object
        const tempBin = {
          id: 'unknown',
          name: 'Unknown Bin',
          location: 'Scanned Location',
          acceptedWaste: ['paper', 'plastic', 'glass', 'metal'],
          tips: ['Please sort your waste properly', 'Make sure items are clean and dry']
        };
        setSelectedBin(tempBin);
        setShowTips(true);
      }
    } catch (error) {
      console.error('Error processing scan:', error);
    }
  };

  const handleScanError = (error) => {
    console.error('Scan error:', error);
  };

  const handleWasteSubmit = async (wasteType) => {
    if (!currentUser || !selectedBin) return;
    
    setProcessingWaste(true);
    
    try {
      // In a real app, you would validate the image here
      // For now, we'll simulate image recognition success
      
      // Calculate points based on waste type
      const pointsMap = {
        paper: 10,
        plastic: 15,
        glass: 20,
        metal: 25,
        organic: 5
      };
      
      const pointsEarned = pointsMap[wasteType] || 10;
      
      // Add the recycling activity to Firestore
      await addDoc(collection(db, 'recyclingActivities'), {
        userId: currentUser.uid,
        binId: selectedBin.id,
        binLocation: selectedBin.location,
        wasteType: wasteType,
        pointsEarned: pointsEarned,
        timestamp: serverTimestamp()
      });
      
      // Show success message
      alert(`Great job! You earned ${pointsEarned} points for recycling ${wasteType}.`);
      
      // Reset the state
      setShowTips(false);
      setSelectedBin(null);
      setScanResult(null);
      
    } catch (error) {
      console.error('Error submitting waste:', error);
      alert('Error submitting your recycling. Please try again.');
    } finally {
      setProcessingWaste(false);
    }
  };

  return (
    <div className="bins-container">
      <Navigation />
      
      <main className="bins-content">
        <h1 className="bins-title">Find Recycling Bins</h1>
        
        {loading ? (
          <div className="loading">Loading bin locations...</div>
        ) : (
          <div className="bins-layout">
            <div className="bins-list-container">
              <h2>Campus Recycling Bins</h2>
              
              <button 
                className="scan-qr-button"
                onClick={handleScanClick}
              >
                📷 Scan Bin QR Code
              </button>
              
              <ul className="bins-list">
                {bins.length === 0 ? (
                  <li className="no-bins">No bins found in the database</li>
                ) : (
                  bins.map(bin => (
                    <li 
                      key={bin.id}
                      className={`bin-item ${selectedBin && selectedBin.id === bin.id ? 'selected' : ''}`}
                      onClick={() => handleBinSelect(bin)}
                    >
                      <div className="bin-icon">🗑️</div>
                      <div className="bin-details">
                        <h3>{bin.name}</h3>
                        <p className="bin-location">{bin.location}</p>
                        <div className="waste-types">
                          {bin.acceptedWaste && bin.acceptedWaste.map(waste => (
                            <span key={waste} className="waste-type">
                              {wasteTypes.find(w => w.id === waste)?.icon || '♻️'}
                            </span>
                          ))}
                        </div>
                      </div>
                    </li>
                  ))
                )}
              </ul>
            </div>
            
            <div className="bin-details-container">
              {showScanner ? (
                <div className="scanner-wrapper">
                  <h2>Scan Bin QR Code</h2>
                  <QRScanner 
                    onSuccess={handleScanSuccess}
                    onError={handleScanError}
                  />
                </div>
              ) : selectedBin && showTips ? (
                <div className="selected-bin-details">
                  <h2>{selectedBin.name}</h2>
                  <p className="bin-location-large">{selectedBin.location}</p>
                  
                  <div className="bin-section">
                    <h3>Accepted Waste Types</h3>
                    <div className="waste-types-grid">
                      {selectedBin.acceptedWaste ? (
                        wasteTypes
                          .filter(waste => selectedBin.acceptedWaste.includes(waste.id))
                          .map(waste => (
                            <button
                              key={waste.id}
                              className="waste-type-card"
                              style={{ backgroundColor: waste.color + '20' }}
                              onClick={() => handleWasteSubmit(waste.id)}
                              disabled={processingWaste}
                            >
                              <div className="waste-icon">{waste.icon}</div>
                              <div className="waste-name">{waste.name}</div>
                            </button>
                          ))
                      ) : (
                        <p>No accepted waste types specified for this bin.</p>
                      )}
                    </div>
                  </div>
                  
                  <div className="bin-section">
                    <h3>Recycling Tips</h3>
                    {selectedBin.tips && selectedBin.tips.length > 0 ? (
                      <ul className="tips-list">
                        {selectedBin.tips.map((tip, index) => (
                          <li key={index} className="tip-item">
                            {tip}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p>No specific tips available for this bin.</p>
                    )}
                  </div>
                  
                  <div className="bin-section">
                    <h3>How to Use</h3>
                    <ol className="instructions-list">
                      <li>Select the type of waste you're recycling</li>
                      <li>Take a photo of your waste for validation</li>
                      <li>Dispose of your waste in this bin</li>
                      <li>Earn points for proper recycling!</li>
                    </ol>
                  </div>
                </div>
              ) : (
                <div className="no-bin-selected">
                  <div className="placeholder-icon">🔍</div>
                  <h2>Select a Bin or Scan QR Code</h2>
                  <p>Choose a recycling bin from the list or scan a bin's QR code to view details and start recycling</p>
                  <button 
                    className="scan-qr-button large"
                    onClick={handleScanClick}
                  >
                    📷 Scan QR Code
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default Bins; 