import React, { useState, useEffect } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import './QRScanner.css';

const QRScanner = ({ onSuccess, onError }) => {
  const [scanResult, setScanResult] = useState(null);
  const [showScanner, setShowScanner] = useState(false);

  // Create and configure the scanner
  useEffect(() => {
    if (!showScanner) return;

    // Configure the scanner
    const html5QrcodeScanner = new Html5QrcodeScanner(
      "qr-reader",
      {
        fps: 10,
        qrbox: {
          width: 250,
          height: 250,
        },
        rememberLastUsedCamera: true,
      },
      /* verbose= */ false
    );

    // Define success and error handlers
    const onScanSuccess = (decodedText, decodedResult) => {
      // Stop the scanner
      html5QrcodeScanner.clear();
      setScanResult(decodedText);
      setShowScanner(false);
      
      // Pass the result to the parent component
      if (onSuccess) {
        try {
          // Try to parse the QR code content as JSON
          const binData = JSON.parse(decodedText);
          onSuccess(binData);
        } catch (error) {
          // If not JSON, pass as is
          onSuccess(decodedText);
        }
      }
    };

    const onScanFailure = (error) => {
      // Handle scan failure, usually ignore
      console.warn(`QR scan error: ${error}`);
      if (onError) {
        onError(error);
      }
    };

    // Render the scanner
    html5QrcodeScanner.render(onScanSuccess, onScanFailure);

    // Clean up on unmount
    return () => {
      if (html5QrcodeScanner) {
        try {
          html5QrcodeScanner.clear();
        } catch (error) {
          console.error("Failed to clear QR scanner", error);
        }
      }
    };
  }, [showScanner, onSuccess, onError]);

  const startScan = () => {
    setScanResult(null);
    setShowScanner(true);
  };

  return (
    <div className="qr-scanner-container">
      {scanResult ? (
        <div className="scan-result">
          <h3>Bin Identified!</h3>
          <p>Preparing waste validation...</p>
          <button onClick={startScan} className="rescan-button">
            Scan Another Bin
          </button>
        </div>
      ) : (
        <div className="scanner-area">
          {!showScanner ? (
            <div className="scanner-placeholder">
              <div className="scanner-icon">📷</div>
              <h3>Scan Recycling Bin QR Code</h3>
              <p>Position your camera over a bin's QR code to identify it and begin recycling</p>
              <button onClick={startScan} className="start-scan-button">
                Start Scanning
              </button>
            </div>
          ) : (
            <div className="scanner-active">
              <div id="qr-reader"></div>
              <button onClick={() => setShowScanner(false)} className="cancel-button">
                Cancel
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default QRScanner; 