import { useAuthStore } from '../store/authStore';

export const useAuth = () => {
  const { user, isAuthenticated, setUser, logout, loadFromLocalStorage, saveToLocalStorage } = useAuthStore();
  
  return {
    user,
    isAuthenticated,
    setUser,
    logout,
    loadFromLocalStorage,
    saveToLocalStorage,
  };
};

export default useAuth;
