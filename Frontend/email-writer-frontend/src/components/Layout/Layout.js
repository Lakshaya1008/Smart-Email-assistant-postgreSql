import React, { useState, useEffect } from 'react';
import Header from './Header';
import Sidebar from './Sidebar';
import './Layout.css';

const Layout = ({ currentView, onViewChange, children, onStatsRefresh }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Check if screen is mobile size
  useEffect(() => {
    const checkMobileSize = () => {
      if (window.innerWidth >= 768) {
        setSidebarOpen(false);
      }
    };

    checkMobileSize();
    window.addEventListener('resize', checkMobileSize);

    return () => window.removeEventListener('resize', checkMobileSize);
  }, []);

  const toggleSidebar = () => {
    setSidebarOpen(!sidebarOpen);
  };

  return (
    <div className="layout">
      <Header 
        currentView={currentView} 
        onViewChange={onViewChange}
      />
      
      <div className="layout-body">
        <Sidebar
          currentView={currentView}
          onViewChange={onViewChange}
          isOpen={sidebarOpen}
          onToggle={toggleSidebar}
          onStatsRefresh={onStatsRefresh}
        />
        
        <main className={`main-content ${sidebarOpen ? 'sidebar-open' : ''}`}>
          <div className="main-header">
            <button
              className="mobile-menu-toggle"
              onClick={toggleSidebar}
              aria-label="Toggle sidebar"
            >
              <i className="fas fa-bars"></i>
            </button>
          </div>
          
          <div className="content-container">
            {children}
          </div>

          <footer className="main-footer">
            <div className="footer-content">
              <div className="footer-branding">
                <i className="fas fa-envelope-open-text footer-icon"></i>
                <h3>Smart Email Assistant</h3>
                <p className="footer-tagline">AI-Powered Email Reply Generation</p>
              </div>

              <div className="footer-divider"></div>

              <div className="footer-creator">
                <p className="creator-label">Designed & Developed by</p>
                <p className="creator-name">Lakshaya Jain</p>

                <div className="footer-links">
                  <a href="https://github.com/Lakshaya1008" target="_blank" rel="noopener noreferrer" className="footer-link">
                    <i className="fab fa-github"></i>
                    <span>GitHub</span>
                  </a>
                  <a href="https://www.linkedin.com/in/lakshaya-jain-195075252" target="_blank" rel="noopener noreferrer" className="footer-link">
                    <i className="fab fa-linkedin"></i>
                    <span>LinkedIn</span>
                  </a>
                  <button onClick={handleEmailClick} className="footer-link footer-link-button">
                    <i className="fas fa-envelope"></i>
                    <span>Email</span>
                  </button>
                </div>
              </div>

              <div className="footer-bottom">
                <p>&copy; 2026 Smart Email Assistant. All rights reserved.</p>
              </div>
            </div>
          </footer>
        </main>
      </div>
    </div>
  );
};

export default Layout;