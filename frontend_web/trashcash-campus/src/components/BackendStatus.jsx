import { useAuth } from '../contexts/AuthContext';
import { useEffect, useState } from 'react';

/**
 * BackendStatus component shows the current backend connectivity status.
 * It can be used in various parts of the application where backend
 * connectivity information is important.
 */
function BackendStatus() {
  const { isBackendOnline, checkBackendStatus } = useAuth();
  const [showFullMessage, setShowFullMessage] = useState(false);

  // Check backend status when component mounts
  useEffect(() => {
    checkBackendStatus();
    // Clean up any timers/handlers if component unmounts
    return () => {
      setShowFullMessage(false);
    };
  }, [checkBackendStatus]);

  // Toggle full message display
  const toggleMessage = () => {
    setShowFullMessage(!showFullMessage);
  };

  // Don't render anything if status is still being determined
  if (isBackendOnline === null) {
    return null;
  }
  
  // If backend is online, show a minimal indicator
  if (isBackendOnline) {
    return (
      <div className="backend-status online">
        <div className="status-indicator"></div>
        <style jsx>{`
          .backend-status {
            display: flex;
            align-items: center;
            padding: 3px;
            border-radius: 4px;
          }
          
          .online {
            color: #388e3c;
          }
          
          .status-indicator {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #4caf50;
            box-shadow: 0 0 3px #4caf50;
          }
        `}</style>
      </div>
    );
  }
  
  // If backend is offline, show a more noticeable indicator
  return (
    <div className="backend-status offline" onClick={toggleMessage}>
      <div className="status-indicator"></div>
      <span className="status-text">
        {showFullMessage ? "Backend Offline" : ""}
      </span>
      {showFullMessage && (
        <div className="backend-offline-message">
          Firebase fallback active
        </div>
      )}
      <style jsx>{`
        .backend-status {
          display: flex;
          align-items: center;
          padding: 5px;
          border-radius: 4px;
          font-size: 12px;
          cursor: pointer;
        }
        
        .offline {
          color: #d32f2f;
        }
        
        .status-indicator {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          margin-right: 5px;
          background-color: #f44336;
          box-shadow: 0 0 3px #f44336;
        }
        
        .status-text {
          margin-right: 5px;
        }
        
        .backend-offline-message {
          font-size: 10px;
          color: #d32f2f;
          font-weight: 500;
        }
      `}</style>
    </div>
  );
}

export default BackendStatus; 