import { useState, useEffect } from 'react';

const THEME_STORAGE_KEY = 'email_writer_theme';

export const useTheme = () => {
  const [theme, setTheme] = useState(() => {
    // Get theme from localStorage or default to 'light'
    const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
    return savedTheme || 'light';
  });

  useEffect(() => {
    // Apply theme to document root element
    const root = document.documentElement;

    // Remove any existing theme attribute first
    root.removeAttribute('data-theme');

    // Apply new theme
    root.setAttribute('data-theme', theme);

    // Also apply to body for better coverage
    document.body.setAttribute('data-theme', theme);

    // Save to localStorage
    localStorage.setItem(THEME_STORAGE_KEY, theme);

  }, [theme]);

  const toggleTheme = () => {
    setTheme(prevTheme => prevTheme === 'light' ? 'dark' : 'light');
  };

  return { theme, toggleTheme };
};

