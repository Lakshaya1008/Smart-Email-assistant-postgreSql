import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useNotification } from '../../hooks/useNotification';
import { useTheme } from '../../hooks/useTheme';
import { formatName } from '../../utils/helpers';
import Modal from '../Common/Modal';
import './Layout.css';

// currentView was previously passed as a prop but never destructured — dead prop.
// Now destructured so Header can show the active view state if needed in future.
const Header = ({ currentView, onViewChange }) => {
  const { user, logout } = useAuth();
  const { showSuccess } = useNotification();
  const { theme, toggleTheme } = useTheme();
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showAbout, setShowAbout] = useState(false);
  const menuContainerRef = useRef(null);

  // Close dropdown when clicking outside — previously only worked on mobile
  // via the mobile-overlay div. On desktop, clicking outside had no effect.
  useEffect(() => {
    if (!showUserMenu) return;

    const handleClickOutside = (e) => {
      if (menuContainerRef.current && !menuContainerRef.current.contains(e.target)) {
        setShowUserMenu(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showUserMenu]);

  const handleLogout = () => {
    logout();
    showSuccess('You have been logged out successfully.');
    setShowUserMenu(false);
  };

  return (
      <header className="header">
        <div className="header-content">
          <div className="header-brand">
            <h1 className="brand-title">
              <i className="fas fa-envelope-open-text brand-icon"></i>
              Smart Email Assistant
            </h1>
          </div>

          <div className="header-user">
            {/* ref attached so the outside-click handler knows the menu boundary */}
            <div className="user-menu-container" ref={menuContainerRef}>
              <button
                  className="user-menu-trigger"
                  onClick={() => setShowUserMenu(!showUserMenu)}
                  aria-expanded={showUserMenu}
                  aria-haspopup="true"
              >
                <div className="user-avatar">
                  <i className="fas fa-user"></i>
                </div>
                <div className="user-info">
                <span className="user-name">
                  {formatName(user?.firstName, user?.lastName) || user?.username}
                </span>
                  <span className="user-email">{user?.email}</span>
                </div>
                <i className={`fas fa-chevron-down menu-arrow ${showUserMenu ? 'open' : ''}`}></i>
              </button>

              {showUserMenu && (
                  <div className="user-menu-dropdown">
                    <div className="user-menu-header">
                      <div className="user-details">
                        <strong>{formatName(user?.firstName, user?.lastName) || user?.username}</strong>
                        <small>{user?.email}</small>
                      </div>
                    </div>

                    <div className="user-menu-divider"></div>

                    <div className="user-menu-items">
                      <button
                          className="user-menu-item"
                          onClick={() => { onViewChange('statistics'); setShowUserMenu(false); }}
                      >
                        <i className="fas fa-chart-bar"></i>
                        View Statistics
                      </button>

                      <button
                          className="user-menu-item"
                          onClick={() => { onViewChange('saved'); setShowUserMenu(false); }}
                      >
                        <i className="fas fa-bookmark"></i>
                        Saved Replies
                      </button>
                    </div>

                    <div className="user-menu-divider"></div>

                    <div className="user-menu-items">
                      <button
                          className="user-menu-item"
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleTheme();
                            setTimeout(() => setShowUserMenu(false), 300);
                          }}
                      >
                        <i className={`fas ${theme === 'light' ? 'fa-moon' : 'fa-sun'}`}></i>
                        {theme === 'light' ? 'Dark Mode' : 'Light Mode'}
                      </button>

                      <button
                          className="user-menu-item"
                          onClick={() => { setShowAbout(true); setShowUserMenu(false); }}
                      >
                        <i className="fas fa-info-circle"></i>
                        About
                      </button>
                    </div>

                    <div className="user-menu-divider"></div>

                    <div className="user-menu-items">
                      <button className="user-menu-item danger" onClick={handleLogout}>
                        <i className="fas fa-sign-out-alt"></i>
                        Sign Out
                      </button>
                    </div>
                  </div>
              )}
            </div>
          </div>
        </div>

        {/* About Modal — personal email removed (issue N10: privacy concern on public app) */}
        {showAbout && (
            <Modal
                isOpen={true}
                onClose={() => setShowAbout(false)}
                title="About Smart Email Assistant"
                size="medium"
            >
              <div className="about-modal">
                <div className="about-header">
                  <i className="fas fa-envelope-open-text about-icon"></i>
                  <h3>Smart Email Assistant</h3>
                  <p className="about-version">Version 1.0.0</p>
                </div>

                <div className="about-description">
                  <p>AI-powered email reply generation using advanced language models</p>
                </div>

                <div className="about-developer">
                  <h4>Developer</h4>
                  <p>Lakshaya Jain</p>
                </div>

                <div className="about-links">
                  <h4>Connect</h4>
                  <div className="link-buttons">
                    <a
                        href="https://github.com/Lakshaya1008"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="about-link"
                    >
                      <i className="fab fa-github"></i>
                      GitHub
                    </a>

                    <a
                        href="https://www.linkedin.com/in/lakshaya-jain-195075252"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="about-link"
                    >
                      <i className="fab fa-linkedin"></i>
                      LinkedIn
                    </a>
                  </div>
                </div>
              </div>
            </Modal>
        )}
      </header>
  );
};

export default Header;