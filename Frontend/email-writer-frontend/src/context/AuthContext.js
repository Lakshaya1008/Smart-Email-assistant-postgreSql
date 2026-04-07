import React, { createContext, useState, useEffect, useRef } from 'react';
import { authService } from '../services/authService';
import {
  getStoredToken, removeStoredToken,
  getStoredUser, setStoredUser, removeStoredUser
} from '../utils/storage';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const logoutTimerRef = useRef(null);

  // ── Timer helpers ──────────────────────────────────────────────────────

  const clearLogoutTimer = () => {
    if (logoutTimerRef.current) {
      clearTimeout(logoutTimerRef.current);
      logoutTimerRef.current = null;
    }
  };

  // Schedule a silent auto-logout at the exact millisecond the JWT expires.
  // This replaces the previous behaviour where an expired token caused an
  // abrupt 401 redirect mid-session, losing any unsaved work.
  const scheduleAutoLogout = (expiresAtMs) => {
    clearLogoutTimer();
    const delay = expiresAtMs - Date.now();
    if (delay > 0) {
      logoutTimerRef.current = setTimeout(() => {
        removeStoredToken();
        removeStoredUser();
        setUser(null);
      }, delay);
    }
  };

  // ── Startup ────────────────────────────────────────────────────────────

  useEffect(() => {
    const initializeAuth = () => {
      try {
        const token = getStoredToken();
        if (!token) return; // No token — stay logged out

        // Validate JWT structure (must be 3 dot-separated parts)
        const parts = token.split('.');
        if (parts.length !== 3) throw new Error('Invalid token format');

        // Decode and check expiry entirely client-side.
        // JWTs are self-validating for expiry — no network call needed.
        // The backend filter validates the signature on every real API call.
        const payload = JSON.parse(atob(parts[1]));
        if (!payload?.sub || !payload?.exp) throw new Error('Invalid token payload');

        const expiresAtMs = payload.exp * 1000;
        if (expiresAtMs <= Date.now()) throw new Error('Token expired');

        // Restore the full user object (email, firstName, lastName, id)
        // saved to localStorage during login/register.
        // Falls back to username-only for old sessions created before this fix.
        const storedUser = getStoredUser();
        if (storedUser && storedUser.username === payload.sub) {
          setUser({ ...storedUser, token });
        } else {
          setUser({ username: payload.sub, token });
        }

        scheduleAutoLogout(expiresAtMs);

      } catch (error) {
        console.error('Auth initialization failed:', error.message);
        removeStoredToken();
        removeStoredUser();
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    initializeAuth();

    return () => clearLogoutTimer(); // clean up timer if component unmounts
  }, []);

  // ── Auth actions ───────────────────────────────────────────────────────

  const login = async (credentials) => {
    const response = await authService.login(credentials);

    const userObj = {
      id: response.id,
      username: response.username,
      email: response.email,
      firstName: response.firstName,
      lastName: response.lastName,
      token: response.token
    };

    setUser(userObj);
    // Persist to localStorage so full user data survives page refresh
    setStoredUser(userObj);

    // Set up proactive auto-logout for this session
    try {
      const payload = JSON.parse(atob(response.token.split('.')[1]));
      if (payload?.exp) scheduleAutoLogout(payload.exp * 1000);
    } catch {
      // If token decode fails here the session still works; expiry
      // will be caught on the next initializeAuth if the user refreshes
    }

    return response;
  };

  const register = async (userData) => {
    const response = await authService.register(userData);

    const userObj = {
      id: response.id,
      username: response.username,
      email: response.email,
      firstName: response.firstName,
      lastName: response.lastName,
      token: response.token
    };

    setUser(userObj);
    setStoredUser(userObj);

    try {
      const payload = JSON.parse(atob(response.token.split('.')[1]));
      if (payload?.exp) scheduleAutoLogout(payload.exp * 1000);
    } catch {
      // same reasoning as in login
    }

    return response;
  };

  const logout = () => {
    clearLogoutTimer();      // cancel any pending auto-logout
    removeStoredToken();
    removeStoredUser();      // was missing before — clears persisted user data
    setUser(null);
  };

  // ── Context value ──────────────────────────────────────────────────────

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