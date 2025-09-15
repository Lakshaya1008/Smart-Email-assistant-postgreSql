import React, { createContext, useState, useEffect } from 'react';
import { authService } from '../services/authService';
import { getStoredToken, removeStoredToken } from '../utils/storage';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Check for existing token on app load
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const token = getStoredToken();
        if (token) {
          // Validate token format before proceeding
          if (typeof token !== 'string' || token.split('.').length !== 3) {
            throw new Error('Invalid token format');
          }
          
          // Test if token is still valid with backend
          const response = await authService.testAuth();
          
          // Parse JWT payload safely
          try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            if (payload && payload.sub) {
              setUser({
                username: payload.sub,
                token: token
              });
            } else {
              throw new Error('Invalid token payload');
            }
          } catch (parseError) {
            console.error('Token parsing failed:', parseError);
            throw parseError; // Re-throw to trigger cleanup
          }
        }
      } catch (error) {
        console.error('Auth initialization failed:', error);
        removeStoredToken();
        setUser(null); // Explicitly set user to null if validation fails
      } finally {
        setLoading(false);
      }
    };

    initializeAuth();
  }, []);

  const login = async (credentials) => {
    try {
      const response = await authService.login(credentials);
      setUser({
        id: response.id,
        username: response.username,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        token: response.token
      });
      return response;
    } catch (error) {
      throw error;
    }
  };

  const register = async (userData) => {
    try {
      const response = await authService.register(userData);
      setUser({
        id: response.id,
        username: response.username,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        token: response.token
      });
      return response;
    } catch (error) {
      throw error;
    }
  };

  const logout = () => {
    try {
      removeStoredToken();
      setUser(null);
      console.log('User logged out successfully');
    } catch (error) {
      console.error('Logout error:', error);
      // Force clear user state even if token removal fails
      setUser(null);
    }
  };

  const value = {
    user,
    login,
    register,
    logout,
    loading,
    isAuthenticated: !!user
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export { AuthContext };