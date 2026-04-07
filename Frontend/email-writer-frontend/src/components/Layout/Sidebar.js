import React, { useState, useEffect, useCallback } from 'react';
import { replyService } from '../../services/replyService';
import { useNotification } from '../../hooks/useNotification';
import './Layout.css';

const Sidebar = ({ currentView, onViewChange, isOpen, onToggle, onStatsRefresh }) => {
  const { showError } = useNotification();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);

  // Wrapped in useCallback so the reference is stable — the onStatsRefresh
  // effect below lists loadStats as a dependency and previously received a
  // stale reference because loadStats was recreated on every render without
  // being memoised. Any refresh triggered from the parent would invoke the
  // old closure and miss state updates.
  const loadStats = useCallback(async () => {
    setLoading(true);
    try {
      const data = await replyService.getReplyStatistics();
      setStats(data);
    } catch (error) {
      showError('Failed to load statistics');
    } finally {
      setLoading(false);
    }
  }, [showError]);

  // Initial load on mount
  useEffect(() => {
    loadStats();
  }, [loadStats]);

  // Reload when sidebar opens and stats are stale
  useEffect(() => {
    if (isOpen && !stats) {
      loadStats();
    }
  }, [isOpen, stats, loadStats]);

  // Expose refresh function to parent — loadStats now stable so dep is correct
  useEffect(() => {
    if (onStatsRefresh) {
      onStatsRefresh(loadStats);
    }
  }, [onStatsRefresh, loadStats]);

  const sidebarItems = [
    {
      id: 'generator',
      label: 'Email Generator',
      icon: 'fas fa-magic',
      description: 'Generate AI-powered email replies',
      badge: null
    },
    {
      id: 'saved',
      label: 'Saved Replies',
      icon: 'fas fa-bookmark',
      description: 'View and manage your saved replies',
      badge: stats?.totalReplies || null
    },
    {
      id: 'statistics',
      label: 'Statistics',
      icon: 'fas fa-chart-bar',
      description: 'View your usage statistics',
      badge: null
    }
  ];

  const quickStats = [
    { label: 'Total Replies', value: stats?.totalReplies || 0,    icon: 'fas fa-reply',  color: 'primary' },
    { label: 'Favorites',     value: stats?.favoriteReplies || 0, icon: 'fas fa-heart',  color: 'success' },
    { label: 'Recent Activity', value: stats?.recentActivity || 0, icon: 'fas fa-clock', color: 'info'    }
  ];

  return (
      <>
        {isOpen && <div className="sidebar-overlay" onClick={onToggle}></div>}

        <aside className={`sidebar ${isOpen ? 'open' : ''}`}>
          <div className="sidebar-content">
            <div className="sidebar-header">
              <div className="sidebar-brand">
                <i className="fas fa-envelope-open-text"></i>
                <span>Smart Email</span>
              </div>
              <button className="sidebar-close" onClick={onToggle} aria-label="Close sidebar">
                <i className="fas fa-times"></i>
              </button>
            </div>

            <nav className="sidebar-nav">
              <ul className="nav-list">
                {sidebarItems.map((item) => (
                    <li key={item.id} className="nav-item-container">
                      <button
                          className={`nav-item ${currentView === item.id ? 'active' : ''}`}
                          onClick={() => { onViewChange(item.id); onToggle(); }}
                          title={item.description}
                      >
                        <i className={`nav-icon ${item.icon}`}></i>
                        <span className="nav-text">{item.label}</span>
                        {item.badge && <span className="nav-badge">{item.badge}</span>}
                      </button>
                    </li>
                ))}
              </ul>
            </nav>

            <div className="sidebar-stats">
              <h3 className="stats-title">Quick Stats</h3>

              {loading ? (
                  <div className="stats-loading">
                    <i className="fas fa-spinner fa-spin"></i>
                    <span>Loading...</span>
                  </div>
              ) : (
                  <div className="stats-grid">
                    {quickStats.map((stat, index) => (
                        <div key={index} className={`stat-item ${stat.color}`}>
                          <div className="stat-icon">
                            <i className={stat.icon}></i>
                          </div>
                          <div className="stat-content">
                            <div className="stat-value">{stat.value}</div>
                            <div className="stat-label">{stat.label}</div>
                          </div>
                        </div>
                    ))}
                  </div>
              )}

              {stats && (
                  <button
                      className="view-full-stats"
                      onClick={() => { onViewChange('statistics'); onToggle(); }}
                  >
                    View Full Statistics
                    <i className="fas fa-arrow-right"></i>
                  </button>
              )}
            </div>

            <div className="sidebar-footer">
              <div className="footer-content">
                <small>Smart Email Assistant v1.0</small>
                <div className="footer-links">
                  <button
                      className="footer-link"
                      title="Refresh statistics"
                      onClick={loadStats}
                      disabled={loading}
                  >
                    <i className={`fas fa-sync-alt ${loading ? 'fa-spin' : ''}`}></i>
                  </button>
                  <button className="footer-link" title="Help & Support">
                    <i className="fas fa-question-circle"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </>
  );
};

export default Sidebar;