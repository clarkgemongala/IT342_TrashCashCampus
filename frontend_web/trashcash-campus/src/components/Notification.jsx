import { Toaster, toast } from 'react-hot-toast';
import { motion, AnimatePresence } from 'framer-motion';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCheckCircle, faExclamationCircle, faInfoCircle } from '@fortawesome/free-solid-svg-icons';

const notificationVariants = {
  initial: { opacity: 0, y: -20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: 20 }
};

const NotificationToast = ({ type = 'success', message }) => {
  const getIcon = () => {
    switch (type) {
      case 'success':
        return faCheckCircle;
      case 'error':
        return faExclamationCircle;
      default:
        return faInfoCircle;
    }
  };

  const getColor = () => {
    switch (type) {
      case 'success':
        return 'bg-green-50 border-green-500 text-green-700';
      case 'error':
        return 'bg-red-50 border-red-500 text-red-700';
      default:
        return 'bg-blue-50 border-blue-500 text-blue-700';
    }
  };

  return (
    <motion.div
      variants={notificationVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      className={`flex items-center p-4 rounded-lg border ${getColor()} shadow-lg`}
    >
      <FontAwesomeIcon icon={getIcon()} className="w-5 h-5 mr-3" />
      <p className="font-medium">{message}</p>
    </motion.div>
  );
};

export const showNotification = (message, type = 'success') => {
  toast.custom((t) => (
    <NotificationToast message={message} type={type} />
  ));
};

export const NotificationProvider = () => {
  return (
    <Toaster
      position="top-right"
      reverseOrder={false}
      gutter={8}
      containerClassName=""
      containerStyle={{}}
      toastOptions={{
        duration: 3000,
        style: {
          background: 'transparent',
          boxShadow: 'none',
          padding: 0,
        },
      }}
    />
  );
}; 