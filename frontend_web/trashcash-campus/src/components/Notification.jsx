import React, { useEffect, useState } from 'react';
import './Notification.css';

const Notification = ({ message, type = 'success', duration = 3000, onClose }) => {
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsVisible(false);
      if (onClose) onClose();
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  if (!isVisible) return null;

  return (
    <div className={`notification ${type} ${isVisible ? 'show' : 'hide'}`}>
      <div className="notification-content">
        <div className="notification-icon">
          {type === 'success' && <span className="icon">✓</span>}
          {type === 'error' && <span className="icon">✕</span>}
          {type === 'info' && <span className="icon">ℹ</span>}
        </div>
        <div className="notification-message">{message}</div>
      </div>
    </div>
  );
};

export default Notification; 