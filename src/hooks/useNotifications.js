import { useState, useEffect } from 'react';

export const useNotifications = (userId) => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    if (userId) {
      // TODO: Fetch notifications from API
      const t = setTimeout(() => setLoading(false), 0)
      return () => clearTimeout(t)
    }
  }, [userId]);
  
  const addNotification = (notification) => {
    setNotifications((prev) => [...prev, notification]);
  };
  
  const removeNotification = (id) => {
    setNotifications((prev) => prev.filter((notif) => notif.id !== id));
  };
  
  return { notifications, loading, addNotification, removeNotification };
};

export default useNotifications;
