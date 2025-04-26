import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { db, storage } from '../firebase';
import { 
  collection, 
  getDocs, 
  addDoc, 
  serverTimestamp, 
  query, 
  orderBy, 
  where, 
  limit, 
  doc, 
  updateDoc, 
  getDoc,
  deleteDoc,
  Timestamp 
} from 'firebase/firestore';
import { ref, getDownloadURL, listAll } from 'firebase/storage';
import Navigation from '../components/Navigation';
import QRScanner from '../components/QRScanner';
import CorsErrorInfo from '../components/CorsErrorInfo';
import { QRCodeSVG } from 'qrcode.react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCheck, faTimes } from '@fortawesome/free-solid-svg-icons';
import './Bins.css';
import './ImagePreview.css';

// Helper functions for formatting
const formatWasteType = (wasteType) => {
  if (!wasteType) return 'Unknown';
  return wasteType.charAt(0).toUpperCase() + wasteType.slice(1).toLowerCase();
};

const formatTimestamp = (timestamp) => {
  if (!timestamp) return 'N/A';
  // Format timestamp for display
  const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// Safely convert various data formats to a valid data URL
const safeBase64ToDataUrl = (data, mimeType = 'image/jpeg') => {
  try {
    if (!data) return null;
    
    // If it's already a data URL, return it as is
    if (typeof data === 'string' && data.startsWith('data:')) {
      return data;
    }
    
    // Check for truncated base64 data - JPEG data should be much longer than 100 chars
    if (typeof data === 'string' && data.startsWith('/9j/') && data.length < 1000) {
      console.log(`Base64 data appears to be truncated (length: ${data.length}), using placeholder instead`);
      return null; // Return null to trigger fallback to placeholder
    }
    
    // Special handling for base64 data that starts with '/9j/' 
    // (JPEG header in base64 without the data URL prefix)
    if (typeof data === 'string' && data.startsWith('/9j/')) {
      console.log("Found valid JPEG image data starting with /9j/");
      return `data:image/jpeg;base64,${data}`;
    }
    
    // Clean the base64 string - remove any non-base64 characters
    const cleanBase64 = typeof data === 'string' 
      ? data.replace(/[^A-Za-z0-9+/=]/g, '')
      : data;
      
    // Create a proper data URL
    return `data:${mimeType};base64,${cleanBase64}`;
  } catch (e) {
    console.error("Error converting to data URL:", e);
    return null;
  }
};

// Function to check if a string is a valid base64 JPEG data
const isValidBase64JPEG = (str) => {
  try {
    // Check if it's a string that starts with the JPEG header in base64
    return typeof str === 'string' && str.startsWith('/9j/') && str.length > 50;
  } catch (e) {
    return false;
  }
};

// Safely create a data URL for JPEG base64 content
const createJpegDataUrl = (base64Content) => {
  if (!base64Content) return null;
  
  try {
    // If it's already a data URL, return it
    if (base64Content.startsWith('data:')) {
      return base64Content;
    }
    
    // If it's a raw base64 JPEG string, convert it
    if (base64Content.startsWith('/9j/')) {
      return `data:image/jpeg;base64,${base64Content}`;
    }
    
    // Otherwise return null
    return null;
  } catch (e) {
    console.error("Error creating JPEG data URL:", e);
    return null;
  }
};

const isValidBase64 = (str) => {
  try {
    // More thorough validation of base64 strings
    if (!str || typeof str !== 'string') return false;
    
    // Special case for JPEG base64 data that starts with /9j/
    if (isValidBase64JPEG(str)) {
      return true;
    }
    
    // Check if it's already a data URL
    if (str.startsWith('data:')) {
      // Simple validation for data URLs
      return str.includes('base64,') && str.length > 100;
    }
    
    // For raw base64 strings, validate proper structure
    // Base64 strings should only contain these characters
    const base64Regex = /^[A-Za-z0-9+/=]+$/;
    return base64Regex.test(str) && str.length > 50;
  } catch (e) {
    console.error("Error validating base64:", e);
    return false;
  }
};

// Get default waste type image
const getWasteTypeImage = (wasteType) => {
  if (!wasteType) return generateColoredSVG('general');
  
  // Use the same colored SVG approach as getPlaceholderImage for consistency
  return generateColoredSVG(wasteType.toLowerCase());
};

// Helper function to generate colored SVG data URI
const generateColoredSVG = (wasteType) => {
  // Ensure wasteType is a string and lowercase
  const type = String(wasteType || '').toLowerCase();
  
  // Define colors for different waste types
  const colors = {
    plastic: '#EA4335',
    paper: '#4285F4',
    metal: '#34A853',
    glass: '#FBBC05',
    organic: '#8D6E63',
    recyclable: '#4285F4',
    biodegradable: '#34A853',
    'non-biodegradable': '#EA4335',
    general: '#757575'
  };
  
  // Use the mapped color or default to gray
  const color = colors[type] || '#757575';
  
  // Escape the color for the data URI correctly
  const escapedColor = color.replace('#', '%23');
  
  // Create a data URI for a colored square with text - this will always work without file loading
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="60" height="60" viewBox="0 0 60 60">
    <rect width="60" height="60" fill="${color}" />
    <text x="30" y="30" font-family="Arial" font-size="10" fill="white" text-anchor="middle" dominant-baseline="middle">
      ${type}
    </text>
  </svg>`;
  
  // Convert to base64 for maximum compatibility
  const base64 = btoa(svg);
  const dataUri = `data:image/svg+xml;base64,${base64}`;
  
  return dataUri;
};

const Bins = () => {
  const { currentUser, isAdmin } = useAuth();
  const [bins, setBins] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedBin, setSelectedBin] = useState(null);
  const [showTips, setShowTips] = useState(false);
  const [showScanner, setShowScanner] = useState(false);
  const [scanResult, setScanResult] = useState(null);
  const [processingWaste, setProcessingWaste] = useState(false);
  const [showQRModal, setShowQRModal] = useState(false);
  const [selectedQRBin, setSelectedQRBin] = useState(null);
  const [binLogs, setBinLogs] = useState([]);
  const [loadingLogs, setLoadingLogs] = useState(false);
  const [verifyingEntry, setVerifyingEntry] = useState(false);
  const [previewImage, setPreviewImage] = useState(null);
  const [modalError, setModalError] = useState(null);
  const [hasImageLoadErrors, setHasImageLoadErrors] = useState(false);
  const [cleaningLogs, setCleaningLogs] = useState(false);
  
  // Define the three bin types
  const binTypes = [
    { 
      id: 'recyclable', 
      name: 'Recyclable Bin', 
      icon: '♻️', 
      color: '#4285F4',
      acceptedWaste: ['paper', 'plastic', 'metal', 'glass'],
      tips: ['Clean items before recycling', 'Remove caps from bottles', 'Flatten boxes']
    },
    { 
      id: 'biodegradable', 
      name: 'Biodegradable Bin', 
      icon: '🍃', 
      color: '#34A853',
      acceptedWaste: ['organic'],
      tips: ['No plastic bags', 'Food waste welcome', 'Plants and garden waste allowed']
    },
    { 
      id: 'non-biodegradable', 
      name: 'Non-Biodegradable Bin', 
      icon: '🗑️', 
      color: '#EA4335',
      acceptedWaste: ['plastic', 'metal'],
      tips: ['Non-recyclable plastics', 'Contaminated items', 'Separate hazardous waste']
    }
  ];
  
  // Waste types
  const wasteTypes = [
    { id: 'paper', name: 'Paper', icon: '📄', color: '#4285F4' },
    { id: 'plastic', name: 'Plastic', icon: '🥤', color: '#EA4335' },
    { id: 'glass', name: 'Glass', icon: '🍶', color: '#FBBC05' },
    { id: 'metal', name: 'Metal', icon: '🥫', color: '#34A853' },
    { id: 'organic', name: 'Organic', icon: '🍎', color: '#8D6E63' },
  ];
  
  // Define retention period for processed logs (in minutes)
  const RETENTION_PERIOD_MINUTES = 2;
  
  useEffect(() => {
    const fetchBins = async () => {
      try {
        const binsRef = collection(db, 'bins');
        const q = query(binsRef, orderBy('name', 'asc'));
        const querySnapshot = await getDocs(q);
        const fetchedBins = querySnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        setBins(fetchedBins);
      } catch (error) {
        console.error("Error fetching bins: ", error);
      } finally {
        setLoading(false);
      }
    };

    fetchBins();
    
    // Run log cleanup when component mounts if admin
    if (isAdmin) {
      cleanupOldLogs();
      
      // Set up periodic cleanup every 30 seconds
      const cleanupInterval = setInterval(() => {
        cleanupOldLogs();
      }, 30 * 1000); // 30 seconds
      
      // Clean up the interval when component unmounts
      return () => clearInterval(cleanupInterval);
    }
  }, [currentUser, isAdmin]);

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
  
  const showQRCodeModal = async (binType) => {
    if (!isAdmin) return;
    
    setSelectedQRBin(binType);
    setLoadingLogs(true);
    setModalError(null); // Reset any previous errors
    
    try {
      // Simplified query without orderBy to avoid Firestore index requirements
      const logsRef = collection(db, 'binLogs');
      
      // Basic query that doesn't require complex indexes
      const q = query(
        logsRef,
        where('binIdentifier', '==', binType.id)
      );
      
      console.log("Querying binLogs for binIdentifier:", binType.id);
      const snapshot = await getDocs(q);
      
      console.log("Found logs:", snapshot.docs.length);
      
      const logsList = await Promise.all(snapshot.docs.map(async (doc) => {
        const data = doc.data();
        console.log("Processing log data:", doc.id, data);
        
        // Log image-related fields for debugging
        console.log(`Log image data for ${doc.id}:`, {
          hasPhotoRef: !!data.photoRef,
          photoRef: data.photoRef,
          hasImageBase64: !!data.imageBase64,
          imageBase64Length: data.imageBase64 ? data.imageBase64.length : 0,
          hasPhotoData: !!data.photoData,
          photoDataLength: data.photoData ? data.photoData.length : 0,
          hasPhotoPreview: !!data.photoPreview,
          photoPreviewLength: data.photoPreview ? data.photoPreview.length : 0,
          photoPreviewStart: data.photoPreview ? data.photoPreview.substring(0, 20) : ''
        });
        
        // Get user info
        let userEmail = '';
        
        // First check if the email is already stored in the log
        if (data.userEmail && data.userEmail.includes('@')) {
          userEmail = data.userEmail;
          console.log("Using email from log data:", userEmail);
        } 
        // Then check if the userId exists and try to fetch the email
        else if (data.userId) {
          try {
            // Try to get the user document directly from firebase auth
            const usersCollectionQuery = query(
              collection(db, 'users'),
              where('uid', '==', data.userId),
              limit(1)
            );
            
            const userDocs = await getDocs(usersCollectionQuery);
            
            if (!userDocs.empty && userDocs.docs[0].data().email) {
              userEmail = userDocs.docs[0].data().email;
              console.log("Found email via uid query:", userEmail);
            } else {
              // Try direct lookup by userId
              const userRef = doc(db, 'users', data.userId);
              const userSnap = await getDoc(userRef);
              
              if (userSnap.exists() && userSnap.data().email) {
                userEmail = userSnap.data().email;
                console.log("Found email via direct lookup:", userEmail);
              } else {
                // Try additional queries as a fallback
                const additionalQueries = [
                  query(collection(db, 'users'), where('id', '==', data.userId), limit(1)),
                  query(collection(db, 'users'), where('email', '!=', ''), limit(20))
                ];
                
                for (const q of additionalQueries) {
                  try {
                    const queryDocs = await getDocs(q);
                    if (!queryDocs.empty) {
                      for (const userDoc of queryDocs.docs) {
                        const userData = userDoc.data();
                        if (userData.email && userData.email.endsWith('@cit.edu')) {
                          userEmail = userData.email;
                          console.log("Found email via additional query:", userEmail);
                          break;
                        }
                      }
                      if (userEmail) break;
                    }
                  } catch (e) {
                    console.error("Error in additional query:", e);
                  }
                }
              }
            }
          } catch (e) {
            console.error('Error fetching user email:', e);
          }
        }
        
        // Verify the email follows the expected format or provide a clear fallback
        if (!userEmail || !userEmail.includes('@')) {
          // If we still don't have a valid email, try looking for a CIT email format
          if (data.name && typeof data.name === 'string') {
            // Try to construct a CIT email from name (if available)
            const nameParts = data.name.split(' ');
            if (nameParts.length >= 2) {
              const possibleEmail = `${nameParts[0].toLowerCase()}.${nameParts[nameParts.length-1].toLowerCase()}@cit.edu`;
              console.log("Constructed possible email from name:", possibleEmail);
              userEmail = possibleEmail;
            }
          }
          
          // If still no valid email, use a clear fallback
          if (!userEmail || !userEmail.includes('@')) {
            userEmail = data.userId ? 
              `${data.name || 'Unknown User'} (CIT Student)` : 
              'Unknown User';
            console.log("Using fallback identifier:", userEmail);
          }
        }
        
        // Update the binLog document to store the email for future reference
        if (userEmail && userEmail.includes('@') && !data.userEmail) {
          try {
            // Use the separate function to update the document
            await updateBinLogEmail(doc.id, userEmail);
          } catch (e) {
            console.error("Error updating binLog with email:", e);
          }
        }
        
        // For now, skip trying to get the actual image URL due to CORS issues
        let photoUrl = null;
        
        // First check if there's full base64 data, which will be the best quality source
        if (!photoUrl && data.imageBase64) {
          try {
            // Check if this is chunked image data
            if (data.imageBase64_chunks && typeof data.imageBase64_chunks === 'number') {
              console.log(`Found chunked image data with ${data.imageBase64_chunks} chunks for log ${doc.id}`);
              
              // Reassemble the chunks
              let fullImageData = data.imageBase64 || '';
              for (let i = 2; i <= data.imageBase64_chunks; i++) {
                const chunkKey = `imageBase64_part${i}`;
                if (data[chunkKey]) {
                  fullImageData += data[chunkKey];
                  console.log(`Added chunk ${i} with length ${data[chunkKey].length}`);
                } else {
                  console.warn(`Missing chunk ${i} for log ${doc.id}`);
                }
              }
              
              console.log(`Reassembled full image data with length: ${fullImageData.length}`);
              
              // Check if the reassembled data is valid
              if (isValidBase64(fullImageData)) {
                // Make sure the base64 string is properly formatted for data URLs
                photoUrl = safeBase64ToDataUrl(fullImageData);
                console.log(`Using reassembled image data for log ${doc.id} (length: ${fullImageData.length})`);
              } else {
                console.warn(`Invalid reassembled image data for log ${doc.id}`);
              }
            } 
            // Regular non-chunked image data
            else if (isValidBase64(data.imageBase64)) {
              // Make sure the base64 string is properly formatted for data URLs
              photoUrl = safeBase64ToDataUrl(data.imageBase64);
              console.log(`Using imageBase64 data for log ${doc.id} (length: ${data.imageBase64.length})`);
            } else {
              console.warn(`Invalid imageBase64 data found in log ${doc.id}`);
            }
          } catch (error) {
            console.error(`Error processing imageBase64 for log ${doc.id}:`, error);
          }
        }
        
        // If there's photoData in the document (mobile app might store it differently)
        if (!photoUrl && data.photoData) {
          if (isValidBase64(data.photoData)) {
            // Make sure the base64 string is properly formatted for data URLs
            photoUrl = safeBase64ToDataUrl(data.photoData);
            console.log(`Using photoData field for image in log ${doc.id} (length: ${data.photoData.length})`);
          } else {
            console.warn(`Invalid photoData found in log ${doc.id}`);
          }
        }
        
        // If there's a photoPreview field in the document (from Firebase screenshot)
        // This is typically truncated so it's our last resort before trying storage
        if (!photoUrl && data.photoPreview) {
          try {
            console.log(`Checking photoPreview for ${doc.id}:`);
            console.log(`- Type: ${typeof data.photoPreview}`);
            console.log(`- Length: ${data.photoPreview.length}`);
            console.log(`- First 30 chars: ${data.photoPreview.substring(0, 30)}`);
            
            // Check for truncated data early
            const isTruncated = data.photoPreview.startsWith('/9j/') && data.photoPreview.length < 1000;
            if (isTruncated) {
              console.log(`Base64 data appears to be truncated (length: ${data.photoPreview.length}), using placeholder instead`);
            } 
            else if (isValidBase64(data.photoPreview) || isValidBase64JPEG(data.photoPreview)) {
              // Make sure the base64 string is properly formatted for data URLs
              photoUrl = safeBase64ToDataUrl(data.photoPreview);
              if (photoUrl) {
                console.log(`Generated photoUrl (first 50 chars): ${photoUrl.substring(0, 50)}...`);
                console.log(`Using photoPreview field for image in log ${doc.id} (starts with: ${data.photoPreview.substring(0, 20)})`);
              } else {
                console.log(`Failed to generate valid photoUrl from photoPreview, using placeholder instead`);
              }
            } else {
              console.warn(`Invalid photoPreview found in log ${doc.id}, falling back to placeholder image`);
            }
          } catch (error) {
            console.error("Error processing photoPreview:", error);
          }
        }
        
        // If base64 image loading failed and there's a photoRef, try Firebase Storage
        if (!photoUrl && data.photoRef) {
          try {
            console.log(`Trying to fetch image from Firebase Storage with ref: ${data.photoRef}`);
            
            // Try different paths - some may be in waste_images, others directly at the root
            const possiblePaths = [
              `waste_images/${data.photoRef}`,
              `${data.photoRef}`,
              `images/${data.photoRef}`,
              `uploads/${data.photoRef}`,
              `photos/${data.photoRef}`,
              // Try with additional file extensions
              `${data.photoRef}.jpg`,
              `${data.photoRef}.jpeg`,
              `${data.photoRef}.png`,
              `waste_images/${data.photoRef}.jpg`
            ];

            let foundImage = false;
            
            // For testing purposes, to ensure the storage is properly configured,
            // let's try an alternative approach using a known path pattern
            try {
              const storageRef = ref(storage);
              console.log("Storage bucket info:", storage.app.options.storageBucket);
              
              // Try to list all items in the root to see available paths
              try {
                const result = await listAll(storageRef);
                console.log("Available items at storage root:", 
                  result.items.map(item => item.fullPath),
                  "Prefixes:", 
                  result.prefixes.map(prefix => prefix.fullPath)
                );
              } catch (listError) {
                console.error("Error listing storage items:", listError);
              }
            } catch (infoError) {
              console.error("Error getting storage info:", infoError);
            }
            
            // Try each path until we find the image
            for (const path of possiblePaths) {
              if (foundImage) continue;
              
              console.log("Attempting to retrieve image from path:", path);
              
              try {
                const storageRef = ref(storage, path);
                
                // Try to get the download URL with a timeout to prevent long waits
                let timeoutPromise = new Promise((_, reject) => 
                  setTimeout(() => reject(new Error('Timeout')), 5000)
                );
                
                const directUrl = await Promise.race([
                  getDownloadURL(storageRef),
                  timeoutPromise
                ]);
                
                // Image found!
                photoUrl = directUrl;
                console.log("Successfully retrieved image URL from path:", path);
                foundImage = true;
                break;
              } catch (pathError) {
                console.log(`Image not found at path: ${path}`);
                // Continue to next path
              }
            }
            
            if (!foundImage) {
              throw new Error("Image not found in any of the attempted paths");
            }
          } catch (error) {
            console.error("Error getting image URL from storage:", error.message || error);
            
            // Fall back to a default image based on waste type
            console.log("Using placeholder image based on waste type");
          }
        }
        
        // Format the waste type for display
        const wasteType = formatWasteType(data.wasteType);
        
        // Create a fallback colored SVG placeholder
        const wasteTypeKey = data.wasteType?.toLowerCase() || 'general';
        let imgSrc = generateColoredSVG(wasteTypeKey);
        
        // Use actual photo data if available, otherwise use the placeholder
        if (photoUrl) {
          imgSrc = photoUrl;
          console.log(`Using actual photo for ${doc.id}`);
        } else {
          console.log(`Using colored placeholder for ${doc.id} (waste type: ${wasteTypeKey}), SVG length: ${imgSrc.length}`);
          console.log(`SVG sample: ${imgSrc.substring(0, 50)}...`);
        }
        
        // Make custom timestamp for this activity
        const timestamp = formatTimestamp(data.timestamp);
        
        // Determine if data is truncated (only for data coming from photoPreview)
        const isTruncated = data.photoPreview && data.photoPreview.startsWith('/9j/') && data.photoPreview.length < 1000;
        
        return {
          id: doc.id,
          ...data,
          userEmail,
          timestamp: data.submittedAt || data.timestamp || Date.now(),
          pointsEarned: data.potentialPoints || 0,
          status: data.status || 'pending',
          approved: data.approved || false,
          photoUrl: photoUrl, // Include the actual photo URL if available
          hasPhotoRef: !!data.photoRef, // Track if there is a photo reference
          photoPreview: data.photoPreview, // Include the photoPreview data
          wasteType: wasteType,
          imgSrc: imgSrc, // Use the actual photo if available, otherwise use colored placeholder
          formattedTimestamp: timestamp,
          hasTruncatedData: isTruncated // Track if we identified truncated data
        };
      }));
      
      // Sort on client-side
      const sortedLogs = logsList.sort((a, b) => {
        const timeA = a.submittedAt ? (typeof a.submittedAt === 'number' ? a.submittedAt : a.submittedAt.toDate?.().getTime() || 0) : 0;
        const timeB = b.submittedAt ? (typeof b.submittedAt === 'number' ? b.submittedAt : b.submittedAt.toDate?.().getTime() || 0) : 0;
        return timeB - timeA; // Descending order (newest first)
      }).slice(0, 20); // Limit to 20 after sorting
      
      console.log("Sorted logs:", sortedLogs);
      
      setBinLogs(sortedLogs);
      setShowQRModal(true);
    } catch (error) {
      console.error('Error fetching bin logs:', error, error.stack);
      setModalError('Failed to load activity logs. Please try again.');
    } finally {
      setLoadingLogs(false);
    }
  };
  
  const closeQRModal = () => {
    setShowQRModal(false);
    setSelectedQRBin(null);
    setBinLogs([]);
  };
  
  const formatDate = (timestamp) => {
    if (!timestamp) return 'Unknown';
    
    try {
      const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      return 'Invalid date';
    }
  };

  const handleVerifyEntry = async (log, isApproved) => {
    if (!isAdmin || !log || !log.id) return;
    
    setVerifyingEntry(true);
    
    try {
      // Update the status in Firestore
      const logRef = doc(db, 'binLogs', log.id);
      
      await updateDoc(logRef, {
        status: isApproved ? 'approved' : 'rejected',
        approved: isApproved,
        processed: true,
        processedAt: serverTimestamp(),
        processedBy: currentUser.uid
      });
      
      // If approved, update the user's points
      if (isApproved && log.userId) {
        try {
          // Get the current user's points
          const userRef = doc(db, 'users', log.userId);
          const userSnap = await getDoc(userRef);
          
          if (userSnap.exists()) {
            const currentPoints = userSnap.data().points || 0;
            const newPoints = currentPoints + (log.potentialPoints || log.pointsEarned || 0);
            
            // Update the user's points
            await updateDoc(userRef, {
              points: newPoints
            });
            
            console.log(`Updated user ${log.userId} points to ${newPoints}`);
          }
        } catch (e) {
          console.error('Error updating user points:', e);
        }
      }
      
      // Refresh the logs
      await showQRCodeModal(selectedQRBin);
      
    } catch (error) {
      console.error('Error verifying entry:', error);
      alert('Failed to update status. Please try again.');
    } finally {
      setVerifyingEntry(false);
    }
  };

  // Updated to use actual photos when available and detect truncated data
  const handleImageClick = (imageUrl, wasteType, photoPreview, log) => {
    // If the log object has chunked imageBase64 data, reassemble it
    if (log && log.imageBase64_chunks && typeof log.imageBase64_chunks === 'number') {
      try {
        console.log(`Reassembling ${log.imageBase64_chunks} chunks for image preview`);
        
        // Reassemble the chunks
        let fullImageData = log.imageBase64 || '';
        for (let i = 2; i <= log.imageBase64_chunks; i++) {
          const chunkKey = `imageBase64_part${i}`;
          if (log[chunkKey]) {
            fullImageData += log[chunkKey];
          }
        }
        
        // Check if the reassembled data is valid
        if (fullImageData && fullImageData.length > 1000 && isValidBase64(fullImageData)) {
          const fullImageUrl = safeBase64ToDataUrl(fullImageData);
          if (fullImageUrl) {
            console.log(`Opening image preview with reassembled image data (${fullImageData.length} bytes)`);
            setPreviewImage(fullImageUrl);
            return;
          }
        }
      } catch (error) {
        console.error("Error reassembling chunked image data:", error);
      }
    }
    
    // If the log object has imageBase64, use it as the highest priority
    if (log && log.imageBase64 && typeof log.imageBase64 === 'string') {
      try {
        if (isValidBase64(log.imageBase64)) {
          const fullImageUrl = safeBase64ToDataUrl(log.imageBase64);
          if (fullImageUrl && fullImageUrl.length > 1000) {
            console.log(`Opening image preview with full image from imageBase64`);
            setPreviewImage(fullImageUrl);
            return;
          } else {
            console.log(`Generated image URL from imageBase64 is invalid`);
          }
        } else {
          console.log(`Log imageBase64 data is not valid base64`);
        }
      } catch (error) {
        console.error("Error processing imageBase64 for modal:", error);
      }
    }
    
    // If no imageBase64 but there's photoData, try that next
    if (log && log.photoData && typeof log.photoData === 'string') {
      try {
        if (isValidBase64(log.photoData)) {
          const photoDataUrl = safeBase64ToDataUrl(log.photoData);
          if (photoDataUrl && photoDataUrl.length > 1000) {
            console.log(`Opening image preview with photo from photoData`);
            setPreviewImage(photoDataUrl);
            return;
          }
        }
      } catch (error) {
        console.error("Error processing photoData for modal:", error);
      }
    }
    
    // If we have photo preview data, check if it's valid and not truncated
    if (photoPreview && typeof photoPreview === 'string') {
      // Check if the data is truncated first
      if (photoPreview.startsWith('/9j/') && photoPreview.length < 1000) {
        console.log(`Cannot use truncated photoPreview data for modal (length: ${photoPreview.length})`);
      }
      // Only try to use the image data if it's probably valid
      else if (photoPreview.length > 1000 && (isValidBase64(photoPreview) || isValidBase64JPEG(photoPreview))) {
        try {
          const actualImage = safeBase64ToDataUrl(photoPreview);
          if (actualImage && actualImage.length > 1000) {
            console.log(`Opening image preview with actual photo from photoPreview`);
            setPreviewImage(actualImage);
            return;
          } else {
            console.log(`Generated photo URL is invalid or truncated`);
          }
        } catch (error) {
          console.error("Error processing photo preview for modal:", error);
        }
      }
    }
    
    // If imageUrl is a valid data URI (not a placeholder), use it directly
    if (imageUrl && imageUrl.startsWith('data:image') && imageUrl.length > 1000) {
      console.log(`Opening image preview with provided image URL`);
      setPreviewImage(imageUrl);
      return;
    }
    
    // Fall back to a colored placeholder
    const placeholderImage = generateColoredSVG(wasteType?.toLowerCase() || 'general');
    console.log(`Opening image preview with placeholder for ${wasteType}`);
    // Set data attributes for better debugging
    document.documentElement.style.setProperty('--waste-type-color', 
      wasteType === 'plastic' ? '#EA4335' : 
      wasteType === 'paper' ? '#4285F4' : 
      wasteType === 'glass' ? '#FBBC05' : 
      wasteType === 'metal' ? '#34A853' : 
      wasteType === 'organic' ? '#8D6E63' : '#757575'
    );
    setPreviewImage(placeholderImage);
  };

  const closeImagePreview = () => {
    setPreviewImage(null);
  };

  const getPlaceholderImage = (wasteType) => {
    // Use the same generateColoredSVG helper to maintain consistency
    return generateColoredSVG(wasteType?.toLowerCase() || 'general');
  };

  const getWasteIcon = (wasteType) => {
    // Find and return the icon for the waste type
    const waste = wasteTypes.find(w => w.id === wasteType);
    return waste?.icon || '♻️';
  };

  const retryLoadLogs = () => {
    if (selectedQRBin) {
      showQRCodeModal(selectedQRBin);
    }
  };

  // Add this function to check if any images failed to load
  const handleImageLoadError = () => {
    setHasImageLoadErrors(true);
  };

  // Special handling for base64 image data - instead of trying to process it,
  // just render a placeholder for now
  const renderPlaceholderForWasteType = (wasteType) => {
    // Return a placeholder element based on waste type
    const icon = getWasteIcon(wasteType || 'recyclable');
    return (
      <div className="placeholder-image">
        {icon}
      </div>
    );
  };

  // Fix the document update function
  const updateBinLogEmail = async (docId, email) => {
    if (!docId || !email) return;
    
    try {
      // Get a direct reference to the document
      const docRef = doc(db, 'binLogs', docId);
      
      // Update the document
      await updateDoc(docRef, {
        userEmail: email
      });
      console.log(`Successfully updated binLog ${docId} with email ${email}`);
    } catch (error) {
      console.error(`Error updating binLog ${docId}:`, error);
    }
  };

  // Function to check if an entry is too old and should be removed
  const isEntryTooOld = (entry) => {
    // Only consider processed entries (approved or rejected)
    if (entry.status !== 'approved' && entry.status !== 'rejected') {
      return false;
    }
    
    // Check if it was processed more than RETENTION_PERIOD_MINUTES ago
    try {
      const processedTimestamp = entry.processedAt?.toDate?.() || 
                                (typeof entry.processedAt === 'number' ? new Date(entry.processedAt) : null);
      
      if (!processedTimestamp) return false;
      
      const now = new Date();
      const ageInMinutes = (now - processedTimestamp) / (1000 * 60);
      
      return ageInMinutes > RETENTION_PERIOD_MINUTES;
    } catch (error) {
      console.error("Error checking entry age:", error);
      return false;
    }
  };
  
  // Function to clean up old logs
  const cleanupOldLogs = async () => {
    if (!isAdmin || cleaningLogs) return;
    
    setCleaningLogs(true);
    
    try {
      // Find entries that are old enough to be deleted
      const logsRef = collection(db, 'binLogs');
      const cutoffDate = new Date();
      cutoffDate.setMinutes(cutoffDate.getMinutes() - RETENTION_PERIOD_MINUTES);
      const cutoffTimestamp = Timestamp.fromDate(cutoffDate);
      
      // Query for processed entries older than the cutoff date
      const q = query(
        logsRef,
        where('processed', '==', true),
        where('processedAt', '<', cutoffTimestamp)
      );
      
      const oldLogs = await getDocs(q);
      console.log(`Found ${oldLogs.docs.length} old logs to clean up`);
      
      // Delete each old log
      const deletePromises = oldLogs.docs.map(doc => deleteDoc(doc.ref));
      await Promise.all(deletePromises);
      
      console.log(`Deleted ${oldLogs.docs.length} old logs`);
      
      // Refresh logs if we're looking at them
      if (selectedQRBin) {
        await showQRCodeModal(selectedQRBin);
      }
      
    } catch (error) {
      console.error("Error cleaning up old logs:", error);
    } finally {
      setCleaningLogs(false);
    }
  };
  
  // Filter logs client-side to hide old processed entries
  const getFilteredLogs = (logs) => {
    return logs.filter(log => !isEntryTooOld(log));
  };

  return (
    <div className="bins-container">
      <Navigation />
      
      <main className="bins-content">
        <h1 className="bins-title">
          {isAdmin ? 'Manage Recycling Bins' : 'Find Recycling Bins'}
        </h1>
        
        {loading ? (
          <div className="loading">Loading bin locations...</div>
        ) : isAdmin ? (
          /* Admin View with QR Codes */
          <div className="qr-bins-grid">
            {binTypes.map(binType => (
              <div key={binType.id} className="qr-bin-card" style={{ borderColor: binType.color }}>
                <div className="qr-bin-header" style={{ backgroundColor: binType.color + '20' }}>
                  <span className="qr-bin-icon">{binType.icon}</span>
                  <h2 className="qr-bin-title">{binType.name}</h2>
                </div>
                
                <div className="qr-code-container">
                  <QRCodeSVG 
                    value={JSON.stringify({ binId: binType.id, binName: binType.name })} 
                    size={180} 
                    level="H"
                    includeMargin={true}
                  />
                  <p className="qr-instruction">Print this QR code and place it on {binType.name}</p>
                </div>
                
                <button
                  className="view-logs-button"
                  onClick={() => showQRCodeModal(binType)}
                >
                  View Recent Activity Logs
                </button>
                
                <div className="waste-types-small">
                  <p>Accepts:</p>
                  <div className="waste-icons">
                    {binType.acceptedWaste.map(waste => (
                      <span key={waste} className="waste-icon-small">
                        {wasteTypes.find(w => w.id === waste)?.icon || '♻️'}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          /* Standard User View */
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
      
      {/* QR Code Modal with Activity Logs */}
      {showQRModal && selectedQRBin && (
        <div className="modal-overlay">
          <div className="qr-modal" style={{ display: "flex", flexDirection: "column" }}>
            <div className="modal-header">
              <h2>{selectedQRBin.name} Activity Logs</h2>
              <button className="close-button" onClick={closeQRModal}>&times;</button>
            </div>
            
            <div className="modal-content" style={{ width: "100%", padding: 0, flex: 1 }}>
              {loadingLogs ? (
                <div className="loading">
                  <div>Loading activity logs...</div>
                </div>
              ) : modalError ? (
                <div className="modal-error">
                  <div className="error-message">{modalError}</div>
                  <button className="retry-button" onClick={retryLoadLogs}>Retry</button>
                </div>
              ) : binLogs.length === 0 ? (
                <div className="no-logs">
                  <div>
                    <p>No activity logs found for this bin type.</p>
                    <p>Activity will appear here when users scan and use this bin.</p>
                  </div>
                </div>
              ) : (
                <>
                  {hasImageLoadErrors && (
                    <div className="cors-error-notice">
                      <CorsErrorInfo showAdmin={isAdmin} />
                    </div>
                  )}
                  <div className="logs-container" style={{ width: "100%", padding: 0 }}>
                    <table className="logs-table" style={{ width: "100%" }}>
                      <colgroup>
                        <col style={{ width: "18%" }} />
                        <col style={{ width: "10%" }} />
                        <col style={{ width: "15%" }} />
                        <col style={{ width: "7%" }} />
                        <col style={{ width: "18%" }} />
                        <col style={{ width: "12%" }} />
                        <col style={{ width: "20%" }} />
                      </colgroup>
                      <thead>
                        <tr>
                          <th>User Email</th>
                          <th>Waste Type</th>
                          <th>Photo</th>
                          <th>Points</th>
                          <th>Date</th>
                          <th>Status</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {getFilteredLogs(binLogs).map(log => (
                          <tr key={log.id} className={log.status === 'approved' ? 'approved-row' : log.status === 'rejected' ? 'rejected-row' : ''}>
                            <td>{log.userEmail}</td>
                            <td>
                              <span className="waste-type-log">
                                {getWasteIcon(log.wasteType)} 
                                {log.wasteType}
                              </span>
                            </td>
                            <td>
                              {(() => {
                                // Check if we have a valid actual photo or need to use a placeholder
                                const hasValidPhoto = log.imgSrc && log.imgSrc.startsWith('data:image') && log.imgSrc.length > 1000;
                                const placeholderSrc = generateColoredSVG(log.wasteType?.toLowerCase() || 'general');
                                
                                // For image loading issues, always prepare a fallback
                                if (hasValidPhoto) {
                                  return (
                                    <div className="photo-thumbnail actual-photo">
                                      <img 
                                        src={log.imgSrc}
                                        alt={`${log.wasteType} waste`}
                                        onClick={(e) => handleImageClick(log.imgSrc, log.wasteType, log.photoPreview, log)}
                                        onError={(e) => {
                                          console.log(`Image load error for ${log.id} (waste type: ${log.wasteType}). Using placeholder.`);
                                          e.target.onerror = null;
                                          // Always use a placeholder to avoid infinite error loops
                                          e.target.src = placeholderSrc;
                                          handleImageLoadError();
                                          // Add a class for better styling
                                          e.target.parentNode.classList.add('placeholder-fallback');
                                          e.target.parentNode.classList.remove('actual-photo');
                                        }}
                                        style={{ 
                                          width: '60px', 
                                          height: '60px', 
                                          borderRadius: '6px',
                                          objectFit: 'cover' 
                                        }}
                                        data-image-info={`ref:${log.hasPhotoRef ? 'yes' : 'no'}, preview:${log.photoPreview ? log.photoPreview.length : 0}, base64:${log.imageBase64 ? log.imageBase64.length : 0}`}
                                      />
                                      <div className="photo-indicator" title="Actual photo">
                                        📸
                                      </div>
                                    </div>
                                  );
                                } else {
                                  // No valid image - display a colored placeholder
                                  return (
                                    <div className="photo-thumbnail">
                                      <img
                                        src={placeholderSrc}
                                        alt={`${log.wasteType} placeholder`}
                                        onClick={(e) => handleImageClick(placeholderSrc, log.wasteType, log.photoPreview, log)}
                                        style={{ 
                                          width: '60px', 
                                          height: '60px', 
                                          borderRadius: '6px',
                                          objectFit: 'contain' 
                                        }}
                                      />
                                      {log.hasTruncatedData ? (
                                        <div className="truncated-data-indicator" title="Image data is truncated">
                                          🔍
                                        </div>
                                      ) : log.hasPhotoRef ? (
                                        <div className="photo-ref-indicator" title="Photo reference exists but cannot be displayed">
                                          ⚠️
                                        </div>
                                      ) : null}
                                    </div>
                                  );
                                }
                              })()}
                            </td>
                            <td className="points-cell">+{log.pointsEarned}</td>
                            <td>{log.formattedTimestamp}</td>
                            <td className={`status-cell status-${log.status}`}>
                              {log.status === 'approved' ? '✅ Approved' : 
                               log.status === 'rejected' ? '❌ Rejected' : 
                               '⏳ Pending'}
                            </td>
                            <td>
                              {log.status === 'pending' ? (
                                <div className="action-buttons">
                                  <button 
                                    className="verify-button"
                                    onClick={(e) => handleVerifyEntry(log, true)}
                                    disabled={verifyingEntry}
                                    title="Verify"
                                  >
                                    <FontAwesomeIcon icon={faCheck} />
                                  </button>
                                  <button 
                                    className="reject-button"
                                    onClick={(e) => handleVerifyEntry(log, false)}
                                    disabled={verifyingEntry}
                                    title="Reject"
                                  >
                                    <FontAwesomeIcon icon={faTimes} />
                                  </button>
                                </div>
                              ) : (
                                <span className="processed-by">
                                  Processed
                                </span>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Image Preview Modal */}
      {previewImage && (
        <div className="image-preview-overlay" onClick={closeImagePreview}>
          <div className="image-preview-content" onClick={e => e.stopPropagation()}>
            <img 
              src={previewImage} 
              alt="Preview" 
              style={{ 
                maxWidth: '95vw', 
                maxHeight: '90vh', 
                objectFit: 'contain',
                border: '1px solid #ccc',
                borderRadius: '8px',
                backgroundColor: '#f8f8f8' 
              }} 
            />
            <button 
              className="close-preview-button" 
              onClick={closeImagePreview}
              aria-label="Close preview"
            >
              &times;
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Bins; 
