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
            <p>
              Built by Lakshaya Jain ·
              <a href="https://github.com/Lakshaya1008" target="_blank" rel="noopener noreferrer"> GitHub</a> ·
              <a href="https://www.linkedin.com/in/lakshaya-jain-195075252" target="_blank" rel="noopener noreferrer"> LinkedIn</a> ·
              <a href="mailto:lakshayajain93@gmail.com"> Email</a>
            </p>
          </footer>
        </main>
      </div>
    </div>
  );
};

export default Layout;