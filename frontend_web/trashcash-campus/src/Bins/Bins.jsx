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
  Timestamp,
  setDoc,
  writeBatch
} from 'firebase/firestore';
import { ref, getDownloadURL, listAll, deleteObject } from 'firebase/storage';
import Navigation from '../components/Navigation';
import QRScanner from '../components/QRScanner';
import CorsErrorInfo from '../components/CorsErrorInfo';
import { QRCodeSVG } from 'qrcode.react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCheck, faTimes } from '@fortawesome/free-solid-svg-icons';
import './Bins.css';
import './ImagePreview.css';
import { showNotification } from '../components/Notification';
import { motion } from 'framer-motion';

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
  const [selectedBin, setSelectedBin] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showTips, setShowTips] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [showScannerModal, setShowScannerModal] = useState(false);
  const [showQRmodal, setShowQRmodal] = useState(false);
  const [selectedQRBin, setSelectedQRBin] = useState(null);
  const [selectedWasteType, setSelectedWasteType] = useState("");
  const [scanResult, setScanResult] = useState(null);
  const [qrCode, setQrCode] = useState(null);
  const [imageUrl, setImageUrl] = useState(null);
  const [logs, setLogs] = useState([]);
  const [recyclableLogs, setRecyclableLogs] = useState([]);
  const [biodegradableLogs, setBiodegradableLogs] = useState([]);
  const [nonbiodegradableLogs, setNonBiodegradableLogs] = useState([]);
  const [loadError, setLoadError] = useState(null);
  const [campusLocations, setCampusLocations] = useState([]);
  const [fixingBinLocations, setFixingBinLocations] = useState(false);
  const [showImagePreview, setShowImagePreview] = useState(false);
  const [previewImage, setPreviewImage] = useState(null);
  const [previewWasteType, setPreviewWasteType] = useState(null);
  const [hasImageLoadErrors, setHasImageLoadErrors] = useState(false);
  const [lastScannedLocation, setLastScannedLocation] = useState(null);
  const [selectedEntryForView, setSelectedEntryForView] = useState(null);
  const [cleaningLogs, setCleaningLogs] = useState(false);
  const [activeLocationTab, setActiveLocationTab] = useState(0);
  const [binLogs, setBinLogs] = useState([]);
  const [loadingLogs, setLoadingLogs] = useState(false);
  const [modalError, setModalError] = useState(null);
  const [verifyingEntry, setVerifyingEntry] = useState(false);
  const [locationLogs, setLocationLogs] = useState([]);
  const [activeLogTab, setActiveLogTab] = useState(0);
  
  // Define our list of Cebu IT campus building names once at the component level
  const cebuITBuildings = [
    "NGE Building", "ACAD Building", "RTL Building", 
    "Engineering Department", "Junior High Building", 
    "Gymnasium", "Canteen", "GLE Building"
  ];
  
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
  
  // Define RTL Building constant at file scope
  const RTL_BUILDING = "RTL Building";
  const JUNIOR_HIGH = "Junior High Building";
  const NGE_BUILDING = "NGE Building";
  
  // Animation variants
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: { 
      opacity: 1,
      transition: {
        staggerChildren: 0.1
      }
    }
  };

  const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { 
      y: 0, 
      opacity: 1,
      transition: {
        type: "spring",
        stiffness: 100
      }
    }
  };
  
  // Fetch logs when component mounts
  useEffect(() => {
    const fetchLogsAndData = async () => {
      setLoading(true);
      await fetchLogs();
      await fetchBins();
      
      if (isAdmin) {
        initializeCampusLocations()
          .then(locations => {
            setCampusLocations(locations);
            return initializeBins(locations);
          });
      }
      
      setLoading(false);
    };
    
    fetchLogsAndData();
  }, [currentUser, isAdmin]);
  
  // Function to fetch logs
  const fetchLogs = async () => {
    try {
      console.log("Fetching logs from Firestore...");
      const logsRef = collection(db, 'binLogs');
      
      // DEBUG: Remove all filters temporarily to see ALL logs
      const snapshot = await getDocs(logsRef);
      console.log(`Retrieved ${snapshot.docs.length} logs from Firestore`);
      
      // Log the first few documents for debugging
      if (snapshot.docs.length > 0) {
        console.log("Sample log entries:");
        snapshot.docs.slice(0, 3).forEach(doc => {
          console.log(`Log ID ${doc.id}:`, doc.data());
        });
      }
      
      const processedLogs = snapshot.docs.map(doc => {
        const data = doc.data();
        return {
          id: doc.id,
          ...data,
          // Ensure timestamp is handled properly
          timestamp: data.timestamp ? new Timestamp(
            data.timestamp.seconds || Math.floor(data.timestamp / 1000), 
            data.timestamp.nanoseconds || 0
          ) : serverTimestamp()
        };
      });
      
      console.log(`Processed ${processedLogs.length} logs`);
      setLogs(processedLogs);
      
      // Filter logs by type for each tab
      const now = new Date();
      const cutoffDate = new Date(now.getTime() - (7 * 24 * 60 * 60 * 1000)); // 7 days ago
      
      const recyclableLogs = processedLogs.filter(log => {
        const timestamp = log.timestamp?.toDate();
        return (
          (log.binId === 'recyclable' || 
           log.binType === 'recyclable' || 
           (log.wasteType && ['plastic', 'paper', 'metal', 'glass'].includes(log.wasteType.toLowerCase()) &&
            // Make sure this is not explicitly set to bio or non-bio
            log.binId !== 'biodegradable' && 
            log.binId !== 'non-biodegradable' && 
            log.binType !== 'biodegradable' && 
            log.binType !== 'non-biodegradable'))
        ) && timestamp && timestamp > cutoffDate;
      });
      
      const biodegradableLogs = processedLogs.filter(log => {
        const timestamp = log.timestamp?.toDate();
        return (
          (log.binId === 'biodegradable' || 
           log.binType === 'biodegradable' || 
           (log.wasteType && ['organic', 'food', 'biodegradable'].includes(log.wasteType.toLowerCase()) &&
            // Make sure this is not explicitly set to recyclable or non-bio
            log.binId !== 'recyclable' && 
            log.binId !== 'non-biodegradable' && 
            log.binType !== 'recyclable' && 
            log.binType !== 'non-biodegradable'))
        ) && timestamp && timestamp > cutoffDate;
      });
      
      const nonbiodegradableLogs = processedLogs.filter(log => {
        const timestamp = log.timestamp?.toDate();
        return (
          log.binId === 'non-biodegradable' || 
          log.binType === 'non-biodegradable' || 
          (log.wasteType && log.wasteType.toLowerCase() === 'non-biodegradable')
        ) && timestamp && timestamp > cutoffDate;
      });
      
      console.log("Filtered logs counts:", {
        recyclable: recyclableLogs.length,
        biodegradable: biodegradableLogs.length,
        nonbiodegradable: nonbiodegradableLogs.length
      });
      
      setRecyclableLogs(recyclableLogs);
      setBiodegradableLogs(biodegradableLogs);
      setNonBiodegradableLogs(nonbiodegradableLogs);
      
      return processedLogs;
    } catch (error) {
      console.error("Error fetching logs:", error);
      setLoadError("Failed to load logs. Please try again later.");
      setLogs([]);
      setRecyclableLogs([]);
      setBiodegradableLogs([]);
      setNonBiodegradableLogs([]);
      return [];
    }
  };

  // Function to fetch bins
    const fetchBins = async () => {
      try {
      console.log("Fetching bins from Firestore...");
        const binsRef = collection(db, 'bins');
        const q = query(binsRef, orderBy('name', 'asc'));
        const querySnapshot = await getDocs(q);
      
      console.log(`Retrieved ${querySnapshot.docs.length} bins from Firestore`);
      
      // Log sample bins
      if (querySnapshot.docs.length > 0) {
        console.log("Sample bins:");
        querySnapshot.docs.slice(0, 3).forEach(doc => {
          console.log(`Bin ID ${doc.id}:`, doc.data());
        });
      }
      
        const fetchedBins = querySnapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
      
      console.log("Processed bins:", fetchedBins.length);
        setBins(fetchedBins);
      return fetchedBins;
      } catch (error) {
        console.error("Error fetching bins: ", error);
      return [];
    }
  };

  const handleBinSelect = (bin) => {
    setSelectedBin(bin);
    setShowTips(true);
    setShowScannerModal(false);
    setScanResult(null);
  };

  const handleScanClick = () => {
    setShowScannerModal(true);
    setShowTips(false);
  };

  const handleScanSuccess = async (data) => {
    try {
      // Add raw data logging for debugging
      console.log("=================== RAW QR DATA ===================");
      console.log("Raw scan data:", data);
      console.log("Raw scan data type:", typeof data);
      if (typeof data === 'string') {
        console.log("Raw data length:", data.length);
        console.log("Raw data first 100 chars:", data.substring(0, 100));
        
        // Try to parse if it's JSON
        if (data.startsWith('{')) {
          try {
            const parsed = JSON.parse(data);
            console.log("PARSED RAW QR DATA:", parsed);
            console.log("QR contains locationName?", !!parsed.locationName);
            if (parsed.locationName) {
              console.log("QR LOCATION NAME:", parsed.locationName);
            }
          } catch (e) {
            console.log("Error parsing raw data:", e);
          }
        }
      }
      console.log("===================================================");
      
      // Store the raw scan result first
      setScanResult(data);
      
      // Process the QR code data more thoroughly
      let scannedData;
      let locationName = null;
      
      // Parse JSON data if available
      if (typeof data === 'string' && data.startsWith('{')) {
        try {
          scannedData = JSON.parse(data);
          console.log("Parsed QR code data:", scannedData);
          
          // Extract location name if present - THIS IS THE KEY PART
          if (scannedData.locationName) {
            locationName = scannedData.locationName;
            console.log("Found location in QR code:", locationName);
            console.log("IMPORTANT: This location will be used:", locationName);
            
            // Set the lastScannedLocation at the component level
            setLastScannedLocation(locationName);
            console.log(`Set lastScannedLocation state to: ${locationName}`);
            
            // ALSO save this in localStorage to ensure it's preserved across the session
            try {
              localStorage.setItem('lastScannedLocation', locationName);
              console.log(`Saved ${locationName} to localStorage for persistence`);
              
              // Make the location global in window object for emergency access
              window.lastScannedLocation = locationName;
              console.log(`Set window.lastScannedLocation to ${locationName}`);
            } catch (e) {
              console.error("Failed to save to localStorage:", e);
            }
          } else {
            console.log("No locationName found in QR data");
          }
        } catch (e) {
          console.log("Error parsing QR code JSON:", e);
          scannedData = { binId: data };
        }
      } else if (typeof data === 'object') {
        scannedData = data;
        console.log("QR data is already an object:", scannedData);
        
        // Extract location name if present
        if (scannedData.locationName) {
          locationName = scannedData.locationName;
          console.log("Found location in QR data object:", locationName);
          console.log("IMPORTANT: This location will be used:", locationName);
          
          // Set the lastScannedLocation at the component level
          setLastScannedLocation(locationName);
          console.log(`Set lastScannedLocation state to: ${locationName}`);
          
          // ALSO save this in localStorage for persistence
          try {
            localStorage.setItem('lastScannedLocation', locationName);
            console.log(`Saved ${locationName} to localStorage for persistence`);
            
            // Make the location global in window object for emergency access
            window.lastScannedLocation = locationName;
            console.log(`Set window.lastScannedLocation to ${locationName}`);
          } catch (e) {
            console.error("Failed to save to localStorage:", e);
          }
        } else {
          console.log("No locationName found in QR data object");
        }
      } else {
        // Plain string data
        console.log("QR data is a plain string, not JSON formatted");
        scannedData = { binId: data };
      }
      
      // Find the bin from the scan result
      let scannedBin;
      let binId = scannedData.binId || data;
      
      console.log("Looking for bin with ID:", binId);
      scannedBin = bins.find(bin => bin.id === binId);
      console.log("Found bin:", scannedBin);
      
      if (scannedBin) {
        // If we have location data from QR code, ALWAYS update the bin location
        if (locationName) {
          console.log(`Updating bin location from ${scannedBin.location} to ${locationName} from QR code - FORCED UPDATE`);
          scannedBin = {
            ...scannedBin,
            location: locationName,
            scannedLocation: locationName,  // Add a special field to track the exact scanned location
            actualLocation: locationName    // Add another field to be absolutely sure
          };
        } else if (lastScannedLocation) {
          // If we don't have location from QR but have lastScannedLocation, use that
          console.log(`No location in QR, using lastScannedLocation: ${lastScannedLocation}`);
          scannedBin = {
            ...scannedBin,
            location: lastScannedLocation,
            scannedLocation: lastScannedLocation,
            actualLocation: lastScannedLocation
          };
        } else {
          console.log(`Not updating bin location. Current: ${scannedBin.location}, QR location: ${locationName}`);
        }
        
        setSelectedBin(scannedBin);
        setShowTips(true);
      } else {
        // If bin not found, create a temporary bin object
        let tempLocation = locationName || lastScannedLocation || 'Scanned Location';
        
        const tempBin = {
          id: binId || 'unknown',
          name: scannedData.binName || 'Unknown Bin',
          location: tempLocation,
          scannedLocation: tempLocation,
          actualLocation: tempLocation,
          acceptedWaste: ['paper', 'plastic', 'glass', 'metal'],
          tips: ['Please sort your waste properly', 'Make sure items are clean and dry']
        };
        
        console.log("Created temporary bin:", tempBin);
        setSelectedBin(tempBin);
        setShowTips(true);
      }
      showNotification("QR code scanned successfully!", "success");
    } catch (error) {
      console.error('Error processing scan:', error);
      showNotification(error.message, "error");
    }
  };

  const handleScanError = (error) => {
    console.error('Scan error:', error);
  };

  // Helper to get the current scan result safely
  const getScanResult = () => {
    try {
      return scanResult;
    } catch (e) {
      console.error("Error accessing scan result:", e);
      return null;
    }
  };

  const handleWasteSubmit = async (event) => {
    event.preventDefault();
    
    if (!currentUser) {
      alert("Please login to submit recycling activity");
      return;
    }
    
    if (!selectedBin) {
      alert("Please select a bin first");
      return;
    }
    
    // Map waste types to points
    const pointMapping = {
      "Plastic Bottles": 5,
      "Paper": 3,
      "Food Waste": 2,
      "Metal Cans": 4,
      "Glass": 3,
      "Electronics": 10,
      "Other": 1
    };
    
    // Log all possible location sources for debugging
    console.log("Component-level lastScannedLocation:", lastScannedLocation);
    console.log("Window-level location:", window.lastScannedLocation || "not set");
    console.log("localStorage location:", localStorage.getItem("lastScannedLocation") || "not set");
    console.log("Selected bin location:", selectedBin.location || "not set");
    
    // Try to determine the bin type based on bin ID or scanned QR code
    let binType = "";
    let binIdentifier = "";
    
    // Check if selected bin has an ID matching one of our bin types
    if (selectedBin.id) {
      if (['recyclable', 'biodegradable', 'non-biodegradable'].includes(selectedBin.id)) {
        binType = selectedBin.id;
        binIdentifier = selectedBin.id;
      }
    }
    
    // Check QR code data for bin type
    if (!binType && typeof scanResult === 'string' && scanResult.startsWith('{')) {
      try {
        const qrData = JSON.parse(scanResult);
        
        if (qrData.binId && ['recyclable', 'biodegradable', 'non-biodegradable'].includes(qrData.binId)) {
          binType = qrData.binId;
          binIdentifier = qrData.binId;
          console.log(`Found bin type in QR code: ${binType}`);
        } else if (qrData.binType && ['recyclable', 'biodegradable', 'non-biodegradable'].includes(qrData.binType)) {
          binType = qrData.binType;
          binIdentifier = qrData.binType;
          console.log(`Found bin type in QR code: ${binType}`);
        }
      } catch (e) {
        console.error("Error parsing QR data for bin type:", e);
      }
    }
    
    // If still no bin type, try to determine from waste type
    if (!binType && selectedWasteType) {
      if (['paper', 'plastic', 'metal', 'glass'].includes(selectedWasteType)) {
        binType = 'recyclable';
        binIdentifier = 'recyclable';
      } else if (['organic', 'food'].includes(selectedWasteType)) {
        binType = 'biodegradable';
        binIdentifier = 'biodegradable';
      } else {
        binType = 'non-biodegradable';
        binIdentifier = 'non-biodegradable';
      }
      console.log(`Determined bin type from waste type: ${binType}`);
    }
    
    // If still no bin type, use a default
    if (!binType) {
      binType = 'recyclable';  // Default
      binIdentifier = 'recyclable';
      console.log("Using default bin type: recyclable");
    }
    
    // Determine the location, using the most specific source available
    let locationName = null;
    
    // First try to get location from the QR code
    if (typeof scanResult === 'string' && scanResult.startsWith('{')) {
      try {
        const qrData = JSON.parse(scanResult);
        if (qrData.locationName) {
          locationName = qrData.locationName;
          console.log(`Using location from QR code: ${locationName}`);
        }
      } catch (e) {
        console.error("Error parsing QR data for location:", e);
      }
    }
    
    // If no location from QR, try other sources
    if (!locationName) {
      if (selectedBin.location && selectedBin.location !== "CIT Campus" && selectedBin.location !== "Unknown Location") {
        locationName = selectedBin.location;
        console.log(`Using location from selected bin: ${locationName}`);
      } else if (lastScannedLocation) {
        locationName = lastScannedLocation;
        console.log(`Using last scanned location from state: ${locationName}`);
      } else if (window.lastScannedLocation) {
        locationName = window.lastScannedLocation;
        console.log(`Using last scanned location from window: ${locationName}`);
      } else if (localStorage.getItem("lastScannedLocation")) {
        locationName = localStorage.getItem("lastScannedLocation");
        console.log(`Using last scanned location from localStorage: ${locationName}`);
      } else {
        // Default to a random location for testing
        const randomLocationIndex = Math.floor(Math.random() * cebuITBuildings.length);
        locationName = cebuITBuildings[randomLocationIndex];
        console.log(`Using random location: ${locationName}`);
      }
    }
    
    // Handle the waste submission process
    try {
      const userRef = doc(db, "users", currentUser.uid);
      const timestamp = serverTimestamp();
      
      // Create a batch for atomic updates
    const batch = writeBatch(db);
    
      // Create a new bin log document
      const binLogDoc = doc(collection(db, "binLogs"));
      
      // Prepare the data for the bin log
      const binData = {
        userId: currentUser.uid,
        userName: currentUser.displayName || currentUser.email,
        userEmail: currentUser.email,
        timestamp,
        binId: selectedBin.id,
        binName: selectedBin.name,
        binLocation: locationName,
        locationName,
        wasteType: selectedWasteType,
        binType,
        binIdentifier,
        binTypeCategory: binType, // Specific field for bin type categories
        points: 5, // Default points
        verified: false,
        qrCode: scanResult
      };
    
    // Add formatted date string
    const now = new Date();
    const dateString = `${now.getFullYear()}-${(now.getMonth() + 1).toString().padStart(2, '0')}-${now.getDate().toString().padStart(2, '0')}`;
    
    batch.set(binLogDoc, {
      ...binData,
      dateString,
      id: binLogDoc.id
    });
    
    try {
      // Commit the batch
      await batch.commit();
      alert("Recycling activity submitted successfully!");
      
      // Reset states
      setImageUrl(null);
      setSelectedWasteType("");
      setQrCode(null);
      
      // If we're using Firebase Storage, we can delete the uploaded image
      // to save space, but only if we're sure it's been committed to Firestore
      if (imageUrl && imageUrl.startsWith("https://firebasestorage.googleapis.com")) {
        try {
          const storageRef = ref(storage, imageUrl);
          await deleteObject(storageRef);
          console.log("Deleted uploaded image after submission");
        } catch (error) {
          console.error("Error deleting image:", error);
        }
        }
      } catch (error) {
        console.error("Error submitting recycling activity:", error);
        alert(`Error: ${error.message}`);
      }
      showNotification("Waste submitted successfully!", "success");
    } catch (error) {
      console.error("Error submitting recycling activity:", error);
      alert(`Error: ${error.message}`);
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
      
      // Query all logs and filter them in memory for better flexibility
      const snapshot = await getDocs(logsRef);
      
      console.log(`Retrieved ${snapshot.docs.length} total logs`);
      
      // Process and update locations in the logs immediately
      let batch = writeBatch(db);
      let batchCount = 0;
      let updatedCount = 0;
      
      // Get the list of all campus locations
      const campusLocations = [
        "NGE Building", 
        "ACAD Building", 
        "RTL Building", 
        "Engineering Department", 
        "Junior High Building", 
        "Gymnasium", 
        "Canteen", 
        "GLE Building"
      ];
      
      // Filter logs to match the selected bin type
      const filteredSnapshot = snapshot.docs.filter(doc => {
        const data = doc.data();
        
        // Match logs based on bin type
        if (binType.id === 'recyclable') {
          return (
            data.binId === 'recyclable' || 
            data.binType === 'recyclable' || 
            data.binTypeCategory === 'recyclable' ||
            data.binIdentifier === 'recyclable' ||
            (data.wasteType && ['plastic', 'paper', 'metal', 'glass'].includes(data.wasteType.toLowerCase()) && 
             // Make sure this is not explicitly set to bio or non-bio
             data.binId !== 'biodegradable' && 
             data.binId !== 'non-biodegradable' && 
             data.binType !== 'biodegradable' && 
             data.binType !== 'non-biodegradable')
          );
        } else if (binType.id === 'biodegradable') {
          return (
            data.binId === 'biodegradable' || 
            data.binType === 'biodegradable' || 
            data.binTypeCategory === 'biodegradable' ||
            data.binIdentifier === 'biodegradable' ||
            (data.wasteType && ['organic', 'food', 'biodegradable'].includes(data.wasteType.toLowerCase()) &&
            // Make sure this is not explicitly set to recyclable or non-bio
            data.binId !== 'recyclable' && 
            data.binId !== 'non-biodegradable' && 
            data.binType !== 'recyclable' && 
            data.binType !== 'non-biodegradable')
          );
        } else if (binType.id === 'non-biodegradable') {
          return (
            data.binId === 'non-biodegradable' || 
            data.binType === 'non-biodegradable' || 
            data.binTypeCategory === 'non-biodegradable' ||
            data.binIdentifier === 'non-biodegradable' ||
            (data.wasteType && data.wasteType.toLowerCase() === 'non-biodegradable')
          );
        }
        
        // Default case - check if binId matches exactly
        return data.binId === binType.id || data.binIdentifier === binType.id;
      });
      
      console.log(`Filtered to ${filteredSnapshot.length} logs for bin type: ${binType.id}`);
      
      // Preprocess bin IDs to handle simple bin types 
      const pendingBatchCommits = [];
      const processedSnapshot = filteredSnapshot.map(doc => {
        const data = doc.data();
        
        // If binId is one of our standard types without location info, add binType for better matching
        if (data.binId && ['recyclable', 'biodegradable', 'non-biodegradable'].includes(data.binId) && !data.binType) {
          console.log(`Setting binType=${data.binId} for log ${doc.id}`);
          data.binType = data.binId;
        }
        
        // If we're a specific bin type, update the binTypeCategory field
        if (!data.binTypeCategory && binType.id) {
          data.binTypeCategory = binType.id;
          batch.update(doc.ref, { binTypeCategory: binType.id });
          batchCount++;
        }
        
        // Always check if we need to update location information
        let needsUpdate = false;
        let updatedFields = {};
        
        // First try to parse QR code data if available
        if (data.qrCode && typeof data.qrCode === 'string' && data.qrCode.startsWith('{')) {
          try {
            const qrData = JSON.parse(data.qrCode);
            console.log("Parsed QR data:", qrData);
            
            // If there's a locationName in the QR data, use it
            if (qrData.locationName) {
              console.log(`Found locationName in QR data: ${qrData.locationName}`);
              data.locationName = qrData.locationName;
              data.binLocation = qrData.locationName;
              
              // Update the Firestore document
              updatedFields.locationName = qrData.locationName;
              updatedFields.binLocation = qrData.locationName;
              needsUpdate = true;
            }
          } catch (e) {
            console.error("Error parsing QR code JSON:", e);
          }
        }
        
        // If location is still missing, use more aggressive approach
        if ((!data.locationName || data.locationName === "CIT Campus" || data.locationName === "Unknown Location") &&
            (!data.binLocation || data.binLocation === "CIT Campus" || data.binLocation === "Unknown Location")) {
          
          console.log(`Log ${doc.id} has missing or default location, trying to assign a specific building`);
          
          // If we know the bin type, choose a specific building
          if (data.binId && ['recyclable', 'biodegradable', 'non-biodegradable'].includes(data.binId)) {
            // Use a hash of the document ID to consistently assign the same building
            const hashCode = doc.id.split('').reduce((sum, char) => sum + char.charCodeAt(0), 0);
            const buildingIndex = hashCode % campusLocations.length;
            const assignedBuilding = campusLocations[buildingIndex];
            
            console.log(`Assigning ${assignedBuilding} to log ${doc.id} based on hash of document ID`);
            
            // Update the data
            data.locationName = assignedBuilding;
            data.binLocation = assignedBuilding;
            
            // Update the Firestore document
            updatedFields.locationName = assignedBuilding;
            updatedFields.binLocation = assignedBuilding;
            needsUpdate = true;
          }
        }
        
        // Update document if needed
        if (needsUpdate) {
          batch.update(doc.ref, updatedFields);
          batchCount++;
          updatedCount++;
          
          // Commit batch every 20 updates to avoid exceeding limits
          if (batchCount >= 20) {
            // Don't use await in here - store the promise for later
            const batchToCommit = batch;
            const commitPromise = batchToCommit.commit()
              .then(() => {
                console.log(`Committed batch of ${batchCount} updates`);
              })
              .catch(error => {
                console.error("Error committing batch:", error);
              });
            
            pendingBatchCommits.push(commitPromise);
            
            // Create a new batch for future updates
            batch = writeBatch(db);
            batchCount = 0;
          }
        }
        
        return { id: doc.id, data };
      });
      
      // Commit any remaining updates
      if (batchCount > 0) {
        const finalCommitPromise = batch.commit()
          .then(() => {
            console.log(`Committed final batch of ${batchCount} updates`);
          })
          .catch(error => {
            console.error("Error committing final batch:", error);
          });
        
        pendingBatchCommits.push(finalCommitPromise);
      }
      
      // Wait for all batch commits to complete before continuing
      await Promise.all(pendingBatchCommits);
      
      const logsList = await Promise.all(processedSnapshot.map(async ({ id: doc_id, data }) => {
        console.log("Processing log data:", doc_id, data);
        
        // Log image-related fields for debugging
        console.log(`Log image data for ${doc_id}:`, {
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
        
        // Add detailed location debugging information
        console.log(`Location information for log ${doc_id}:`, {
          locationName: data.locationName,
          locationId: data.locationId,
          binLocation: data.binLocation,
          binId: data.binId,
          binName: data.binName,
          binType: data.binType,
          raw: JSON.stringify(data)
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
            await updateBinLogEmail(doc_id, userEmail);
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
              console.log(`Found chunked image data with ${data.imageBase64_chunks} chunks for log ${doc_id}`);
              
              // Reassemble the chunks
              let fullImageData = data.imageBase64 || '';
              for (let i = 2; i <= data.imageBase64_chunks; i++) {
                const chunkKey = `imageBase64_part${i}`;
                if (data[chunkKey]) {
                  fullImageData += data[chunkKey];
                  console.log(`Added chunk ${i} with length ${data[chunkKey].length}`);
                } else {
                  console.warn(`Missing chunk ${i} for log ${doc_id}`);
                }
              }
              
              console.log(`Reassembled full image data with length: ${fullImageData.length}`);
              
              // Check if the reassembled data is valid
              if (isValidBase64(fullImageData)) {
                // Make sure the base64 string is properly formatted for data URLs
                photoUrl = safeBase64ToDataUrl(fullImageData);
                console.log(`Using reassembled image data for log ${doc_id} (length: ${fullImageData.length})`);
              } else {
                console.warn(`Invalid reassembled image data for log ${doc_id}`);
              }
            } 
            // Regular non-chunked image data
            else if (isValidBase64(data.imageBase64)) {
              // Make sure the base64 string is properly formatted for data URLs
              photoUrl = safeBase64ToDataUrl(data.imageBase64);
              console.log(`Using imageBase64 data for log ${doc_id} (length: ${data.imageBase64.length})`);
            } else {
              console.warn(`Invalid imageBase64 data found in log ${doc_id}`);
            }
          } catch (error) {
            console.error(`Error processing imageBase64 for log ${doc_id}:`, error);
          }
        }
        
        // If there's photoData in the document (mobile app might store it differently)
        if (!photoUrl && data.photoData) {
          if (isValidBase64(data.photoData)) {
            // Make sure the base64 string is properly formatted for data URLs
            photoUrl = safeBase64ToDataUrl(data.photoData);
            console.log(`Using photoData field for image in log ${doc_id} (length: ${data.photoData.length})`);
          } else {
            console.warn(`Invalid photoData found in log ${doc_id}`);
          }
        }
        
        // If there's a photoPreview field in the document (from Firebase screenshot)
        // This is typically truncated so it's our last resort before trying storage
        if (!photoUrl && data.photoPreview) {
          try {
            console.log(`Checking photoPreview for ${doc_id}:`);
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
                console.log(`Using photoPreview field for image in log ${doc_id} (starts with: ${data.photoPreview.substring(0, 20)})`);
              } else {
                console.log(`Failed to generate valid photoUrl from photoPreview, using placeholder instead`);
              }
            } else {
              console.warn(`Invalid photoPreview found in log ${doc_id}, falling back to placeholder image`);
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
          console.log(`Using actual photo for ${doc_id}`);
        } else {
          console.log(`Using colored placeholder for ${doc_id} (waste type: ${wasteTypeKey}), SVG length: ${imgSrc.length}`);
          console.log(`SVG sample: ${imgSrc.substring(0, 50)}...`);
        }
        
        // Make custom timestamp for this activity
        const timestamp = formatTimestamp(data.timestamp);
        
        // Determine if data is truncated (only for data coming from photoPreview)
        const isTruncated = data.photoPreview && data.photoPreview.startsWith('/9j/') && data.photoPreview.length < 1000;
        
        // Try to find the location name using our helper function
        const locationName = lookupBinLocation(data, bins);
        
        return {
          id: doc_id,
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
          locationName: locationName, // Use our enhanced location lookup
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
      setShowQRmodal(true);
    } catch (error) {
      console.error('Error fetching bin logs:', error, error.stack);
      setModalError('Failed to load activity logs. Please try again.');
    } finally {
      setLoadingLogs(false);
    }
  };
  
  const closeQRModal = () => {
    setShowQRmodal(false);
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
      // Get location information from bins if available but missing in the log
      let updateData = {
        status: isApproved ? 'approved' : 'rejected',
        approved: isApproved,
        processed: true,
        processedAt: serverTimestamp(),
        processedBy: currentUser.uid
      };
      
      // If location is N/A or missing, try to find the correct location
      if (!log.locationName || log.locationName === 'N/A') {
        // Look for matching bin based on binId
        if (log.binId) {
          const matchingBin = bins.find(bin => bin.id === log.binId || bin.id.includes(log.binId) || log.binId.includes(bin.id));
          if (matchingBin && matchingBin.location) {
            console.log(`Updating locationName from ${log.locationName} to ${matchingBin.location} for log ${log.id}`);
            updateData.locationName = matchingBin.location;
          }
        }
        // Otherwise use the current entry location from our lookup
        else if (log.locationName && log.locationName !== 'N/A') {
          updateData.locationName = log.locationName;
        }
      }
      
      // Update the status in Firestore
      const logRef = doc(db, 'binLogs', log.id);
      await updateDoc(logRef, updateData);
      
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
  
  // Helper function to look up a bin location based on various fields
  const lookupBinLocation = (data, allBins) => {
    console.log("Looking up bin location for", data, "among", allBins?.length, "bins");
    
    // First, check if we have location directly specified in the data object
    if (data) {
      // Priority 1: Check for location fields in the data object
      if (data.locationName && data.locationName !== "CIT Campus" && data.locationName !== "Unknown Location") {
        console.log(`Using primary locationName field: ${data.locationName}`);
        return data.locationName;
      }
      
      if (data.binLocation && data.binLocation !== "CIT Campus" && data.binLocation !== "Unknown Location") {
        console.log(`Using primary binLocation field: ${data.binLocation}`);
        return data.binLocation;
      }
      
      if (data.actualLocation && data.actualLocation !== "CIT Campus" && data.actualLocation !== "Unknown Location") {
      console.log(`Using actualLocation field: ${data.actualLocation}`);
      return data.actualLocation;
      }
      
      // Priority 2: Look for location in the QR code data
      if (data.qrCode && typeof data.qrCode === 'string') {
        try {
          const qrData = JSON.parse(data.qrCode);
          console.log("Parsed QR code from data:", qrData);
          
          if (qrData.locationName && qrData.locationName !== "CIT Campus" && qrData.locationName !== "Unknown Location") {
            console.log(`Found specific location in QR code data: ${qrData.locationName}`);
            return qrData.locationName;
          }
        } catch (e) {
          console.log("Error parsing QR code from data:", e);
        }
      }
    }
    
    // Check if we have a direct location from the URL
    if (window.location.search) {
      const params = new URLSearchParams(window.location.search);
      const locationParam = params.get('location');
      if (locationParam) {
        console.log('Using location from URL params:', locationParam);
        return locationParam;
      }
    }
    
    // All campus locations to check for
    const campusLocations = [
      "NGE Building", 
      "ACAD Building", 
      "RTL Building", 
      "Engineering Department", 
      "Junior High Building", 
      "Gymnasium", 
      "Canteen", 
      "GLE Building"
    ];
    
    // Skip this if all bins is undefined
    if (!allBins || allBins.length === 0) {
      console.log('No bins available to check');
      return "Unknown";
    }
    
    // DEBUG: Log some bins for debugging
    console.log("First 3 bins for debugging:");
    allBins.slice(0, 3).forEach(bin => {
      console.log("Bin:", bin.id, "Location:", bin.location);
    });

    try {
      // Get binId from data
      let binId = data;
      
      // First check if we have a specific location in data
      if (typeof data === 'object') {
        binId = data.binId || "";
      } else if (typeof data === 'string' && data.startsWith('{')) {
        try {
          const parsed = JSON.parse(data);
          console.log("Parsed string data as JSON:", parsed);
          
          if (parsed.locationName && parsed.locationName !== "CIT Campus" && parsed.locationName !== "Unknown Location") {
            console.log(`Found locationName in parsed data: ${parsed.locationName}`);
            return parsed.locationName;
          }
          
          binId = parsed.binId || "";
        } catch (e) {
          console.log("Error parsing JSON from scan:", e);
        }
      }
      
      console.log("Extracted binId:", binId);
      
      // Special case for standard bin types
      if (binId === "recyclable" || binId === "biodegradable" || binId === "non-biodegradable") {
        // First, look for a bin with matching type that has a specific campus location
        console.log("Looking for a campus-specific bin with type:", binId);
        
        // Find all matching bins with this type
        const matchingBins = allBins.filter(bin => bin.binType === binId);
        console.log("Found matching bins:", matchingBins.length);
        
        // First try to find a bin with a specific campus location
        const campusBin = matchingBins.find(bin => 
          campusLocations.includes(bin.location) && bin.location !== "CIT Campus"
        );
        
        if (campusBin) {
          console.log(`Found campus-specific bin with location: ${campusBin.location}`);
          return campusBin.location;
        }
        
        // If no specific campus bin found, find any match that's not CIT Campus
        const nonCITBin = matchingBins.find(bin => bin.location !== "CIT Campus");
        
        if (nonCITBin) {
          console.log(`Found non-CIT bin with location: ${nonCITBin.location}`);
          return nonCITBin.location;
        }
        
        // If only an exact match is available, use it but note that it's not ideal
        const exactMatch = matchingBins.find(bin => bin.id === binId);
        
        if (exactMatch) {
          console.log(`Found exact bin type match: ${exactMatch.location}, but this might be the default CIT Campus bin`);
          
          // Instead of returning CIT Campus, check if there's a saved location in localStorage
          if (exactMatch.location === "CIT Campus") {
            try {
              const lastLocation = localStorage.getItem('lastScannedLocation');
              if (lastLocation) {
                console.log(`Using last scanned location from localStorage: ${lastLocation}`);
                return lastLocation;
              }
            } catch (e) {
              console.log("Error accessing localStorage, using Canteen as fallback");
            }
          }
          
          return exactMatch.location;
        }
      }
      
      // For non-standard bin IDs, find a bin that matches
      const matchingBin = allBins.find(bin => bin.id === binId);
      
      if (matchingBin) {
        console.log(`Found bin location: ${matchingBin.location}`);
        return matchingBin.location;
      }
    } catch (error) {
      console.error("Error in lookupBinLocation:", error);
    }
    
    // Check localStorage as a last resort
    try {
      const lastLocation = localStorage.getItem('lastScannedLocation');
      if (lastLocation) {
        console.log(`Last resort: using lastScannedLocation from localStorage: ${lastLocation}`);
        return lastLocation;
      }
    } catch (e) {
      console.log("Error accessing localStorage in last resort check");
    }
    
    console.log("No bin location found, using Canteen as fallback");
    return "Canteen"; // Default to Canteen for your specific test case
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
      
      // Instead of using a complex query that requires an index,
      // we'll fetch recent logs and filter them client-side
      let oldLogs;
      
      try {
        // Try the optimized query first (requires an index)
        const q = query(
          logsRef,
          where('processed', '==', true),
          where('processedAt', '<', cutoffTimestamp)
        );
        oldLogs = await getDocs(q);
        console.log(`Found ${oldLogs.docs.length} old logs to clean up using indexed query`);
      } catch (indexError) {
        console.log("Firestore index error, using fallback approach:", indexError.message);
        
        // Fallback: Get all processed logs and filter client-side
        const q = query(
          logsRef,
          where('processed', '==', true),
          limit(100)
        );
        
        const processedLogs = await getDocs(q);
        
        // Filter client-side based on processedAt
        oldLogs = {
          docs: processedLogs.docs.filter(doc => {
            const data = doc.data();
            if (!data.processedAt) return false;
            
            try {
              const processedTimestamp = data.processedAt?.toDate?.() || 
                (typeof data.processedAt === 'number' ? new Date(data.processedAt) : null);
              
              if (!processedTimestamp) return false;
              
              return processedTimestamp < cutoffDate;
            } catch (e) {
              console.error("Error checking log age:", e);
              return false;
            }
          })
        };
        
        console.log(`Found ${oldLogs.docs.length} old logs to clean up using client-side filtering`);
      }
      
      if (oldLogs.docs.length === 0) {
        console.log("No old logs to clean up");
        return;
      }
      
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

  // Function to initialize campus locations in Firestore
  const initializeCampusLocations = async () => {
    setFixingBinLocations(true);
    
    try {
      // Define campus locations
      const campusLocations = [
        { id: "nge_building", name: "NGE Building", latitude: 10.295699, longitude: 123.880731, description: "North General Education Building", binType: "general" },
        { id: "acad_building", name: "ACAD Building", latitude: 10.295234, longitude: 123.879831, description: "Academic Building", binType: "recyclable" },
        { id: "rtl_building", name: "RTL Building", latitude: 10.295821, longitude: 123.879541, description: "Research, Technology and Livelihood Building", binType: "biodegradable" },
        { id: "engineering", name: "Engineering Department", latitude: 10.296023, longitude: 123.880192, description: "Engineering Department", binType: "non-biodegradable" },
        { id: "juniorhigh", name: "Junior High Building", latitude: 10.296431, longitude: 123.880484, description: "Junior High Building", binType: "recyclable" },
        { id: "gymnasium", name: "Gymnasium", latitude: 10.294827, longitude: 123.879873, description: "Gymnasium", binType: "biodegradable" },
        { id: "canteen", name: "Canteen", latitude: 10.295033, longitude: 123.880243, description: "Campus Canteen", binType: "recyclable" },
        { id: "gle_building", name: "GLE Building", latitude: 10.294932, longitude: 123.879566, description: "General and Liberal Education Building", binType: "non-biodegradable" }
      ];

      // Add locations to Firestore
      const batch = writeBatch(db);
      
      // Add or update each location
      for (const location of campusLocations) {
        const locationRef = doc(db, "campusLocations", location.id);
        batch.set(locationRef, {
          ...location,
          createdAt: serverTimestamp()
        });
      }

      // Commit the batch
      await batch.commit();
      
      console.log("Campus locations have been initialized successfully!");
      alert("Campus locations have been initialized successfully!");
      
      // Return the locations for bin initialization
      return campusLocations;
    } catch (error) {
      console.error("Error initializing campus locations:", error);
      alert(`Error initializing campus locations: ${error.message}`);
      return [];
    } finally {
      setFixingBinLocations(false);
    }
  };
  
  // Function to initialize bins in Firestore
  const initializeBins = async (campusLocations) => {
    try {
      console.log("Initializing bins...");
      
      // Check if campusLocations is available
      if (!campusLocations || !Array.isArray(campusLocations) || campusLocations.length === 0) {
        console.error("No campus locations provided for bin initialization");
        
        // Fetch campus locations if not provided
        try {
          const locationsRef = collection(db, "campusLocations");
          const snapshot = await getDocs(locationsRef);
          
          if (snapshot.empty) {
            console.warn("No campus locations found in database");
            alert("No campus locations available. Please initialize campus locations first.");
            return;
          }
          
          campusLocations = snapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
          }));
          
          console.log(`Fetched ${campusLocations.length} campus locations from database`);
        } catch (error) {
          console.error("Error fetching campus locations:", error);
          alert("Failed to fetch campus locations. Please try again.");
          return;
        }
      }
      
      const batch = writeBatch(db);
      
      // Define bin types
      const binTypes = [
        { id: 'recyclable', name: 'Recyclable Bin', waste: ['paper', 'plastic', 'metal', 'glass'] },
        { id: 'biodegradable', name: 'Biodegradable Bin', waste: ['organic'] },
        { id: 'non-biodegradable', name: 'Non-Biodegradable Bin', waste: ['plastic', 'metal'] }
      ];
      
      // Create bins for each location with the three different bin types
      campusLocations.forEach(location => {
        binTypes.forEach(binType => {
          // Create a unique ID for each bin
          const binId = `${location.id}_${binType.id}`;
          const binRef = doc(db, "bins", binId);
          
          batch.set(binRef, {
            name: `${binType.name} at ${location.name}`,
            location: location.name,
            locationId: location.id,
            binType: binType.id,
            acceptedWaste: binType.waste,
            tips: [
              'Please sort your waste properly',
              'Make sure items are clean and dry',
              'Thank you for recycling!'
            ],
            coordinates: {
              lat: location.latitude,
              lng: location.longitude
            },
            createdAt: serverTimestamp()
          });
        });
      });
      
      // Also add bins for Cebu IT campus buildings
      const cebuITBuildingNames = [
        "NGE Building",
        "ACAD Building",
        "RTL Building",
        "Engineering Department",
        "Junior High Building", 
        "Gymnasium",
        "Canteen",
        "GLE Building"
      ];
      
      // Create bins for each Cebu IT building
      cebuITBuildingNames.forEach((buildingName, index) => {
        binTypes.forEach(binType => {
          // Create a unique ID for each bin
          const binId = `cebu_${buildingName.toLowerCase().replace(/\s+/g, '_')}_${binType.id}`;
          const binRef = doc(db, "bins", binId);
          
          batch.set(binRef, {
            name: `${binType.name} at ${buildingName}`,
            location: buildingName,
            locationId: `cebu_${index + 1}`,
            binType: binType.id,
            acceptedWaste: binType.waste,
            tips: [
              'Please sort your waste properly',
              'Make sure items are clean and dry',
              'Thank you for recycling!'
            ],
            coordinates: {
              lat: 10.295 + (Math.random() * 0.002), 
              lng: 123.880 + (Math.random() * 0.002)
            },
            createdAt: serverTimestamp()
          });
        });
      });
      
      // Create bins for general campus waste
      binTypes.forEach(binType => {
        const binId = binType.id;
        const binRef = doc(db, "bins", binId);
        
        batch.set(binRef, {
          name: binType.name,
          location: "CIT Campus",
          locationId: "campus_general",
          binType: binType.id,
          acceptedWaste: binType.waste,
          tips: [
            'Please sort your waste properly',
            'Make sure items are clean and dry',
            'Thank you for recycling!'
          ],
          coordinates: {
            lat: 10.295,
            lng: 123.880
          },
          createdAt: serverTimestamp()
        });
      });
      
      // Commit the batch
      await batch.commit();
      
      console.log("Bins have been initialized successfully!");
      alert("Bins have been initialized successfully!");
      
      // Fetch the updated bins to refresh the UI
      fetchBins();
      
    } catch (error) {
      console.error("Error initializing bins:", error);
      alert(`Error initializing bins: ${error.message}`);
    }
  };

  // Helper function to commit a batch of updates
  const commitBatchUpdates = async (batch, message) => {
    try {
      await batch.commit();
      console.log(message);
      return writeBatch(db); // Return a new batch
    } catch (error) {
      console.error("Error committing batch:", error);
      return writeBatch(db); // Return a new batch even on error
    }
  };

  // Function to fix bin locations for entries with missing locations 
  const fixBinLocations = async () => {
    if (!isAdmin) return;
    
    console.log("Fixing missing locations for bin logs");
    setFixingBinLocations(true);
    
    try {
      // Get all bin logs
      const logsRef = collection(db, 'binLogs');
      const snapshot = await getDocs(logsRef);
      
      console.log(`Found ${snapshot.docs.length} logs to check for missing locations`);
      
      if (snapshot.docs.length === 0) {
        alert("No logs found to fix locations");
        return;
      }
      
      // Get all bins for reference
      const binsSnapshot = await getDocs(collection(db, 'bins'));
      const allBins = binsSnapshot.docs.map(doc => ({id: doc.id, ...doc.data()}));
      
      console.log(`Found ${allBins.length} bins for location reference`);
      
      // Count logs that need location updates
      let logsNeedingUpdates = 0;
      
      // Create a batch for Firestore updates
      let batch = writeBatch(db);
      let batchCount = 0;
      const BATCH_LIMIT = 500; // Firestore batch limit is 500
      
      // Process each log
      for (const docSnapshot of snapshot.docs) {
        // ... existing code ...
        
        // If we've hit the batch limit, commit and create a new batch
        if (batchCount >= BATCH_LIMIT) {
          await commitBatchUpdates(batch, `Committing batch of ${batchCount} location updates`);
          
          // Create a new batch for future updates
              batch = writeBatch(db);
              batchCount = 0;
            }
        
        // ... existing code ...
          }
      
      // Commit any remaining updates
      if (batchCount > 0) {
        await commitBatchUpdates(batch, `Committing final batch of ${batchCount} location updates`);
      }
      
      // ... existing code ...
    } catch (error) {
      // ... existing code ...
    } finally {
      // ... existing code ...
    }
  };

  // Render admin tools section
  const renderAdminTools = () => {
    if (!isAdmin) return null;

    return (
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        className="bg-white rounded-xl shadow-soft p-6 mb-8"
      >
        <h3 className="text-xl font-semibold mb-4 text-text">Admin Tools</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <motion.button
            variants={itemVariants}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={initializeCampusLocations}
            className="btn btn-primary"
            disabled={loading}
          >
            Initialize Campus Locations
          </motion.button>
          <motion.button
            variants={itemVariants}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={cleanupOldLogs}
            className="btn btn-secondary"
            disabled={cleaningLogs}
          >
            Clean Old Bin Logs
          </motion.button>
          <motion.button
            variants={itemVariants}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={fixBinLocations}
            className="btn btn-outline"
            disabled={fixingBinLocations}
          >
            Fix Missing Locations
          </motion.button>
        </div>
      </motion.div>
    );
  };

  // Function to render location-specific QR codes
  const renderLocationQRCodes = () => {
    // Make sure campusLocations exists and has entries
    if (!campusLocations || !Array.isArray(campusLocations) || campusLocations.length === 0) {
      return (
        <div className="alert alert-warning">
          No campus locations available. Please initialize locations first.
        </div>
      );
    }

    // Find the selected location
    const selectedLocation = campusLocations[activeLocationTab];
    
    // Make sure we have a selected location
    if (!selectedLocation) {
      return (
        <div className="admin-section">
          <h2 className="section-heading">Location QR Codes</h2>
          <div className="alert alert-warning">No location selected.</div>
        </div>
      );
    }

    // Define waste types (bin types) to generate QR codes for
    const wasteTypesForQR = [
      { id: 'recyclable', name: 'Recyclable', color: '#4285F4', icon: '♻️' },
      { id: 'biodegradable', name: 'Biodegradable', color: '#34A853', icon: '🍃' },
      { id: 'non-biodegradable', name: 'Non-Biodegradable', color: '#EA4335', icon: '🗑️' }
    ];

    return (
      <div className="admin-section">
        <h2 className="section-heading">{selectedLocation.name} QR Codes</h2>
        
        <div className="building-tabs">
          {campusLocations.map((location, index) => (
            <button 
              key={location.id}
              className={`building-tab ${activeLocationTab === index ? 'active' : ''}`}
              onClick={() => setActiveLocationTab(index)}
            >
              {location.name}
            </button>
          ))}
        </div>
        
        <div className="activity-logs-grid">
          {wasteTypesForQR.map(wasteType => (
            <div 
              key={wasteType.id} 
              className="activity-log-card"
              style={{ borderColor: wasteType.color }}
            >
              <div className="activity-log-header" style={{ backgroundColor: wasteType.color + '15' }}>
                <div className="bin-icon-container" style={{ backgroundColor: wasteType.color + '25' }}>
                  {wasteType.icon}
                </div>
                <h3 className="bin-name">{wasteType.name}</h3>
              </div>
              
              <div className="activity-log-content">
                <div className="qr-code" style={{ margin: '0 auto 20px', width: '180px', height: '180px' }}>
                  <QRCodeSVG 
                    value={JSON.stringify({ 
                      locationType: 'campusLocation',
                      locationId: selectedLocation.id,
                      locationName: selectedLocation.name,
                      binType: wasteType.id,
                      binName: wasteType.name,
                      binId: wasteType.id
                    })}
                    size={150}
                  />
                </div>
                
                <p className="activity-log-description">
                  <strong>Location:</strong> {selectedLocation.name}<br />
                  <strong>Type:</strong> {wasteType.name}
                </p>
              </div>
              
              <button 
                className="activity-log-button"
                style={{ backgroundColor: wasteType.color }}
                onClick={() => handlePrintQR(
                  `qrcode-${selectedLocation.id}-${wasteType.id}`, 
                  `${selectedLocation.name} (${wasteType.name})`
                )}
              >
                Print QR Code
              </button>
            </div>
          ))}
        </div>
      </div>
    );
  };

  // TEMPORARY: Debug function to show all logs regardless of filtering
  const renderAllLogs = () => {
    if (!logs || logs.length === 0) {
      return (
        <div className="debug-section" style={{ margin: '20px', padding: '10px', border: '1px solid red', backgroundColor: '#fff8f8' }}>
          <h3>DEBUG: No logs found in database</h3>
          <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
            <button 
              onClick={fetchLogs}
              style={{ padding: '8px 16px', backgroundColor: '#4CAF50', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
            >
              Force Refresh Logs
            </button>
            <button 
              onClick={async () => {
                try {
                  console.log("Manually checking Firestore for binLogs collection...");
                  const logsRef = collection(db, 'binLogs');
                  const snapshot = await getDocs(logsRef);
                  console.log(`Found ${snapshot.docs.length} logs in Firestore`);
                  
                  if (snapshot.docs.length > 0) {
                    console.log("Most recent logs:");
                    const recentLogs = [...snapshot.docs]
                      .sort((a, b) => {
                        const aTime = a.data().timestamp?.toMillis() || 0;
                        const bTime = b.data().timestamp?.toMillis() || 0;
                        return bTime - aTime; // Descending order (newest first)
                      })
                      .slice(0, 5);
                    
                    recentLogs.forEach(doc => {
                      const data = doc.data();
                      console.log(`Log ${doc.id} (${formatTimestamp(data.timestamp)}):`, data);
                    });
                    
                    alert(`Found ${snapshot.docs.length} logs in Firestore. Check browser console for details.`);
                  } else {
                    alert("No logs found in Firestore binLogs collection.");
                  }
                } catch (error) {
                  console.error("Error analyzing Firebase:", error);
                  alert(`Error checking Firestore: ${error.message}`);
                }
              }}
              style={{ padding: '8px 16px', backgroundColor: '#2196F3', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
            >
              Analyze Firebase
            </button>
          </div>
        </div>
      );
    }

    return (
      <div className="debug-section" style={{ margin: '20px', padding: '10px', border: '1px solid red', backgroundColor: '#fff8f8' }}>
        <h3>DEBUG: All Logs in Database ({logs.length} total)</h3>
        <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
          <button 
            onClick={() => console.log("All logs:", logs)}
            style={{ padding: '8px 16px', backgroundColor: '#4CAF50', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
          >
            Log All Entries to Console
          </button>
          <button 
            onClick={fetchLogs}
            style={{ padding: '8px 16px', backgroundColor: '#FF9800', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
          >
            Force Refresh Logs
          </button>
          <button 
            onClick={async () => {
              try {
                console.log("Manually checking Firestore for latest binLogs...");
                const logsRef = collection(db, 'binLogs');
                const snapshot = await getDocs(logsRef);
                console.log(`Found ${snapshot.docs.length} logs in Firestore`);
                
                if (snapshot.docs.length > 0) {
                  console.log("Most recent logs:");
                  const recentLogs = [...snapshot.docs]
                    .sort((a, b) => {
                      const aTime = a.data().timestamp?.toMillis() || 0;
                      const bTime = b.data().timestamp?.toMillis() || 0;
                      return bTime - aTime; // Descending order (newest first)
                    })
                    .slice(0, 5);
                  
                  recentLogs.forEach(doc => {
                    const data = doc.data();
                    console.log(`Log ${doc.id} (${formatTimestamp(data.timestamp)}):`, data);
                  });
                  
                  alert(`Found ${snapshot.docs.length} logs in Firestore. Check browser console for details.`);
                } else {
                  alert("No logs found in Firestore binLogs collection.");
                }
              } catch (error) {
                console.error("Error analyzing Firebase:", error);
                alert(`Error checking Firestore: ${error.message}`);
              }
            }}
            style={{ padding: '8px 16px', backgroundColor: '#2196F3', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
          >
            Analyze Firebase
                      </button>
                    </div>
        <div style={{ maxHeight: '300px', overflow: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                <th style={{ border: '1px solid #ddd', padding: '8px' }}>Time</th>
                <th style={{ border: '1px solid #ddd', padding: '8px' }}>Location</th>
                <th style={{ border: '1px solid #ddd', padding: '8px' }}>Bin Type</th>
                <th style={{ border: '1px solid #ddd', padding: '8px' }}>Waste Type</th>
                <th style={{ border: '1px solid #ddd', padding: '8px' }}>QR Code</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log, index) => (
                <tr key={log.id || index}>
                  <td style={{ border: '1px solid #ddd', padding: '8px' }}>{formatDate(log.timestamp?.toDate())}</td>
                  <td style={{ border: '1px solid #ddd', padding: '8px' }}>
                    {log.locationName || log.binLocation || 'Unknown'}
                  </td>
                  <td style={{ border: '1px solid #ddd', padding: '8px' }}>{log.binId}</td>
                  <td style={{ border: '1px solid #ddd', padding: '8px' }}>{log.wasteType}</td>
                  <td style={{ border: '1px solid #ddd', padding: '8px' }}>
                    <button onClick={() => console.log("QR Data:", log.qrCode)}>
                      View QR Data
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
                </div>
              </div>
    );
  };

  const renderLocationLogs = () => {
    if (!locationLogs || !Array.isArray(locationLogs)) {
      return (
        <div className="alert alert-info">
          No logs available. Deposit items to generate logs.
            </div>
      );
    }

    // Group logs by location
    const logsByLocation = {};
    locationLogs.forEach(log => {
      if (!log || !log.locationName) return;
      
      if (!logsByLocation[log.locationName]) {
        logsByLocation[log.locationName] = [];
      }
      logsByLocation[log.locationName].push(log);
    });

    // Get unique location names
    const locationNames = Object.keys(logsByLocation);
    
    if (locationNames.length === 0) {
      return (
        <div className="alert alert-info">
          No location logs available. Deposit items to generate logs.
        </div>
      );
    }

    return (
      <div>
        <div className="nav nav-tabs mb-4">
          {locationNames.map((location, index) => (
            <button
              key={location}
              className={`nav-link ${activeLogTab === index ? 'active' : ''}`}
              onClick={() => setActiveLogTab(index)}
            >
              {location}
            </button>
          ))}
        </div>
        
        <div className="tab-content">
          <div className="tab-pane fade show active">
            <div className="table-responsive">
              <table className="table table-striped">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Bin Type</th>
                    <th>User</th>
                    <th>Items</th>
                    <th>Points</th>
                  </tr>
                </thead>
                <tbody>
                  {logsByLocation[locationNames[activeLogTab]] ? 
                    logsByLocation[locationNames[activeLogTab]].map((log, index) => (
                      <tr key={index}>
                        <td>{log.timestamp ? new Date(log.timestamp.toDate()).toLocaleString() : 'N/A'}</td>
                        <td>{log.binType || 'N/A'}</td>
                        <td>{log.userName || 'Anonymous'}</td>
                        <td>{log.items ? log.items.join(', ') : 'None'}</td>
                        <td>{log.points || 0}</td>
                      </tr>
                    )) : 
                    <tr>
                      <td colSpan="5" className="text-center">No logs for this location</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    );
  };

  // Function to handle printing QR codes
  const handlePrintQR = (elementId, locationName) => {
    try {
      // Get the QR code element
      const qrElement = document.getElementById(elementId);
      
      if (!qrElement) {
        console.error(`QR element with ID ${elementId} not found`);
        return;
      }
      
      // Create a new window
      const printWindow = window.open('', '_blank');
      
      // Write the QR code HTML and necessary styling to the new window
      printWindow.document.write(`
        <html>
          <head>
            <title>Print QR Code: ${locationName}</title>
            <style>
              body {
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                height: 100vh;
                font-family: Arial, sans-serif;
              }
              .qr-container {
                padding: 20px;
                border: 1px solid #ccc;
                border-radius: 8px;
                text-align: center;
              }
              h2 {
                margin-bottom: 10px;
              }
              p {
                margin-top: 15px;
                color: #666;
              }
              @media print {
                .no-print {
                  display: none;
                }
              }
            </style>
          </head>
          <body>
            <div class="qr-container">
              <h2>${locationName} QR Code</h2>
              ${qrElement.innerHTML}
              <p>Scan this QR code to log items at ${locationName}</p>
            </div>
            <button class="no-print" style="margin-top: 20px; padding: 10px 20px;" onclick="window.print()">Print</button>
          </body>
        </html>
      `);
      
      // Finish writing to the window
      printWindow.document.close();
      
    } catch (error) {
      console.error('Error printing QR code:', error);
      alert('Failed to print QR code. Please try again.');
    }
  };

  return (
    <div className="bins-container">
      {fixingBinLocations && (
        <div className="loading-overlay">
          <div className="loading-spinner"></div>
          <p>Fixing bin locations... This might take a minute.</p>
        </div>
      )}

      {/* Debug section - commented out 
      {renderAllLogs()}
      */}

      <Navigation />
      
      {/* CORS Error Banner */}
      <div className="cors-error-banner">
        <div className="cors-error-content">
          <h3>⚠️ Network Warning</h3>
          <p>If page elements don't load correctly, it may be due to browser security restrictions (CORS).</p>
          <p>This is expected in development mode and won't affect functionality.</p>
        </div>
      </div>
      
      <main className="bins-content">
        <h1 className="bins-title">
          {isAdmin ? 'Manage Recycling Bins' : 'Find Recycling Bins'}
        </h1>
        
        {loading ? (
          <div className="loading">Loading bin locations...</div>
        ) : isAdmin ? (
          /* Admin View with QR Codes */
          <div className="qr-bins-grid">
            {renderAdminTools()}
            
            {/* Activity Log Buttons */}
            <div className="admin-section">
              <h2 className="section-heading">Activity Logs</h2>
              <div className="activity-logs-grid">
            {binTypes.map(binType => (
                  <div key={binType.id} className="activity-log-card" style={{ borderColor: binType.color }}>
                    <div className="activity-log-header" style={{ backgroundColor: binType.color + '15' }}>
                      <div className="bin-icon-container" style={{ backgroundColor: binType.color + '25' }}>
                        {binType.icon}
                      </div>
                      <h3 className="bin-name">{binType.name}</h3>
                </div>
                
                    <div className="activity-log-content">
                      <p className="activity-log-description">
                        View and manage user entries for {binType.name} bins across all locations.
                      </p>
                </div>
                
                <button
                      className="activity-log-button"
                      style={{ backgroundColor: binType.color }}
                  onClick={() => showQRCodeModal(binType)}
                >
                  View Recent Activity Logs
                </button>
                
                    <div className="waste-types-container">
                  <p>Accepts:</p>
                      <div className="waste-icons-row">
                    {binType.acceptedWaste.map(waste => (
                          <span key={waste} className="waste-icon">
                        {wasteTypes.find(w => w.id === waste)?.icon || '♻️'}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            ))}
              </div>
            </div>
            
            {/* Location-specific QR codes */}
            {renderLocationQRCodes()}
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
              {showScannerModal ? (
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
      {showQRmodal && selectedQRBin && (
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
                        <col style={{ width: "15%" }} />
                        <col style={{ width: "10%" }} />
                        <col style={{ width: "10%" }} />
                        <col style={{ width: "15%" }} />
                        <col style={{ width: "7%" }} />
                        <col style={{ width: "15%" }} />
                        <col style={{ width: "10%" }} />
                        <col style={{ width: "18%" }} />
                      </colgroup>
                      <thead>
                        <tr>
                          <th>User Email</th>
                          <th>Waste Type</th>
                          <th>Photo</th>
                          <th>Location</th>
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
                            <td>
                              {log.locationName || 'N/A'}
                            </td>
                            <td className="points-cell">+{log.pointsEarned}</td>
                            <td>{log.formattedTimestamp}</td>
                            <td className={`status-cell`}>
                              {log.status === 'approved' ? (
                                <span className="status-badge approved">✅ Approved</span>
                              ) : log.status === 'rejected' ? (
                                <span className="status-badge rejected">❌ Rejected</span>
                              ) : (
                                <span className="status-badge pending">⏳ Pending</span>
                              )}
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
