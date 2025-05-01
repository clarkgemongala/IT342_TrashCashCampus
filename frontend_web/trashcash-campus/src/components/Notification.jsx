import React, { useState, useEffect } from 'react';
import './Notification.css';

const Notification = ({ show, type = 'info', message, onClose, autoClose = true, duration = 3000 }) => {
  const [visible, setVisible] = useState(show);

  useEffect(() => {
    setVisible(show);
    
    let timer;
    if (show && autoClose) {
      timer = setTimeout(() => {
        setVisible(false);
        if (onClose) setTimeout(onClose, 300); // Allow animation to finish
      }, duration);
    }
    
    return () => {
      if (timer) clearTimeout(timer);
    };
  }, [show, autoClose, duration, onClose]);

  if (!show && !visible) return null;

  const handleClose = () => {
    setVisible(false);
    if (onClose) setTimeout(onClose, 300); // Allow animation to finish
  };

  const getIcon = () => {
    switch (type) {
      case 'success': return '✓';
      case 'error': return '✕';
      case 'info': return 'ℹ';
      default: return 'ℹ';
    }
  };

  return (
    <div className={`notification ${type} ${visible ? 'show' : 'hide'}`}>
      <div className="notification-content">
        <div className="notification-icon">{getIcon()}</div>
        <div className="notification-message">{message}</div>
      </div>
    </div>
  );
};

export default Notification; 