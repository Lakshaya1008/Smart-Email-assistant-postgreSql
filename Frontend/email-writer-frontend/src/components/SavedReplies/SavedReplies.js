import React, { useState, useEffect, useRef, useCallback } from 'react';
import { replyService } from '../../services/replyService';
import { useNotification } from '../../hooks/useNotification';
import { debounce } from '../../utils/helpers';
import SearchBar from './SearchBar';
import FilterPanel from './FilterPanel';
import ReplyCard from './ReplyCard';
import LoadingSpinner from '../Common/LoadingSpinner';
import './SavedReplies.css';

const SavedReplies = ({ onReplyUpdated, onViewChange }) => {
  const { showError, showSuccess } = useNotification();
  const [replies, setReplies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [exportLoading, setExportLoading] = useState(false);
  const [coldStart, setColdStart] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [filters, setFilters] = useState({
    tone: '',
    fromDate: '',
    toDate: '',
    showFavoritesOnly: false
  });
  const [currentView, setCurrentView] = useState('all');
  const [pagination, setPagination] = useState({
    currentPage: 0,
    totalPages: 0,
    totalElements: 0,
    size: 20
  });

  // Keep a ref to filters so the debounced function always reads the latest
  // tone value without needing to be recreated on every filter change.
  const filtersRef = useRef(filters);
  useEffect(() => {
    filtersRef.current = filters;
  }, [filters]);

  // ── Core data loader ───────────────────────────────────────────────────
  // Accepts an optional filtersOverride so callers that just computed new
  // filters (e.g. handleFilterChange) can pass them directly without waiting
  // for the setFilters() async state update to settle.
  const loadReplies = useCallback(async (page = 0, filtersOverride = null) => {
    const activeFilters = filtersOverride !== null ? filtersOverride : filtersRef.current;

    setLoading(true);
    setColdStart(false);

    const coldStartTimer = setTimeout(() => setColdStart(true), 7000);

    try {
      let response;

      if (activeFilters.showFavoritesOnly) {
        response = await replyService.getFavoriteReplies();
        setReplies(response.favorites || []);
        setPagination(prev => ({ ...prev, totalElements: response.total || 0 }));
        setCurrentView('favorites');
      } else {
        const params = {
          page,
          size: 20,
          ...(activeFilters.tone && { tone: activeFilters.tone }),
          ...(activeFilters.fromDate && { fromDate: activeFilters.fromDate }),
          ...(activeFilters.toDate && { toDate: activeFilters.toDate })
        };

        response = await replyService.getReplyHistory(params);
        setReplies(response.content || []);
        setPagination({
          currentPage: response.currentPage || 0,
          totalPages: response.totalPages || 0,
          totalElements: response.totalElements || 0,
          size: response.size || 20
        });
        setCurrentView('all');
      }
    } catch (error) {
      showError(error.message);
      setReplies([]);
    } finally {
      clearTimeout(coldStartTimer);
      setLoading(false);
      setColdStart(false);
    }
  }, [showError]);

  // ── Initial load + favorites toggle ───────────────────────────────────
  // Intentionally only watching showFavoritesOnly — tone/date filter changes
  // are handled synchronously in handleFilterChange with the merged filters
  // passed directly, avoiding the stale-state race.
  useEffect(() => {
    loadReplies(0);
  }, [filters.showFavoritesOnly, loadReplies]);

  // ── Search — created ONCE via useRef, reads filtersRef to avoid stale tone
  const debouncedSearch = useRef(
      debounce(async (query) => {
        if (!query.trim()) return;
        setLoading(true);
        try {
          const response = await replyService.searchReplies(
              query,
              filtersRef.current.tone || null
          );
          setReplies(response.results || []);
          setPagination(prev => ({ ...prev, totalElements: response.total || 0 }));
          setCurrentView('search');
        } catch (error) {
          // showError is stable (from useNotification) so safe to capture once
        } finally {
          setLoading(false);
        }
      }, 500)
  ).current;

  useEffect(() => {
    if (searchQuery.trim()) {
      debouncedSearch(searchQuery);
    } else if (currentView === 'search') {
      setCurrentView('all');
      loadReplies(0);
    }
  }, [searchQuery]);

  // ── Filter changes ─────────────────────────────────────────────────────
  // Compute merged filters synchronously so we can pass them directly to
  // loadReplies — no waiting for the async setFilters() to settle.
  const handleFilterChange = (newFilters) => {
    const mergedFilters = { ...filtersRef.current, ...newFilters };
    setFilters(mergedFilters);

    // showFavoritesOnly changes are handled by the useEffect above.
    // All other filter changes (tone, dates) call loadReplies directly
    // with the already-merged filters so they take effect immediately.
    if (!('showFavoritesOnly' in newFilters)) {
      loadReplies(0, mergedFilters);
    }
  };

  // ── Action handlers ───────────────────────────────────────────────────
  const handleToggleFavorite = async (replyId) => {
    try {
      const response = await replyService.toggleFavorite(replyId);
      setReplies(prev => prev.map(reply =>
          reply.id === replyId
              ? { ...reply, isFavorite: response.isFavorite }
              : reply
      ));
      if (onReplyUpdated) onReplyUpdated();
      showSuccess(response.message);
    } catch (error) {
      showError(error.message);
    }
  };

  const handleDeleteReply = async (replyId) => {
    try {
      await replyService.deleteReply(replyId);
      setReplies(prev => prev.filter(reply => reply.id !== replyId));
      if (onReplyUpdated) onReplyUpdated();
      showSuccess('Reply deleted successfully');
    } catch (error) {
      showError(error.message);
    }
  };

  const handleExportReplies = async () => {
    if (exportLoading) return; // prevent double-click
    setExportLoading(true);
    try {
      await replyService.exportReplies('csv');
      showSuccess('Replies exported successfully');
    } catch (error) {
      showError(error.message);
    } finally {
      setExportLoading(false);
    }
  };

  const handlePageChange = (page) => {
    if (currentView === 'all') {
      loadReplies(page);
    }
  };

  const getViewTitle = () => {
    switch (currentView) {
      case 'favorites': return 'Favorite Replies';
      case 'search': return `Search Results for "${searchQuery}"`;
      default: return 'All Saved Replies';
    }
  };

  // ── Render ─────────────────────────────────────────────────────────────
  return (
      <div className="saved-replies">
        <div className="saved-replies-header">
          <div className="header-title-section">
            <h1 className="page-title">
              <i className="fas fa-bookmark"></i>
              Saved Replies
            </h1>
            <p className="page-description">
              Manage and search through your saved email replies
            </p>
          </div>

          <div className="header-actions">
            <button
                onClick={handleExportReplies}
                className="btn btn-outline"
                title="Export replies to CSV"
                disabled={exportLoading}
            >
              {exportLoading ? (
                  <>
                    <LoadingSpinner size="small" />
                    Exporting...
                  </>
              ) : (
                  <>
                    <i className="fas fa-download"></i>
                    Export
                  </>
              )}
            </button>
          </div>
        </div>

        <div className="saved-replies-content">
          <div className="replies-sidebar">
            <SearchBar
                value={searchQuery}
                onChange={setSearchQuery}
                placeholder="Search replies..."
            />

            <FilterPanel
                filters={filters}
                onFilterChange={handleFilterChange}
                totalCount={pagination.totalElements}
            />
          </div>

          <div className="replies-main">
            <div className="replies-view-header">
              <div className="view-info">
                <h2 className="view-title">{getViewTitle()}</h2>
                <span className="view-count">
                {loading ? '...' : `${pagination.totalElements} ${pagination.totalElements === 1 ? 'reply' : 'replies'}`}
              </span>
              </div>

              {currentView === 'search' && (
                  <button
                      onClick={() => {
                        setSearchQuery('');
                        setCurrentView('all');
                        loadReplies(0);
                      }}
                      className="btn btn-ghost btn-small"
                  >
                    <i className="fas fa-times"></i>
                    Clear Search
                  </button>
              )}
            </div>

            {loading ? (
                <div className="replies-loading">
                  <LoadingSpinner size="large" text="Loading saved replies..." />
                  {coldStart && (
                      <div className="cold-start-message">
                        <i className="fas fa-clock"></i>
                        <span>Waking up the server… This may take a few seconds.</span>
                      </div>
                  )}
                </div>
            ) : replies.length > 0 ? (
                <>
                  <div className="replies-grid">
                    {replies.map(reply => (
                        <ReplyCard
                            key={reply.id}
                            reply={reply}
                            onToggleFavorite={handleToggleFavorite}
                            onDelete={handleDeleteReply}
                        />
                    ))}
                  </div>

                  {currentView === 'all' && pagination.totalPages > 1 && (
                      <div className="replies-pagination">
                        <div className="pagination-info">
                          Showing {replies.length} of {pagination.totalElements} replies
                        </div>

                        <div className="pagination-controls">
                          <button
                              onClick={() => handlePageChange(pagination.currentPage - 1)}
                              disabled={pagination.currentPage === 0}
                              className="btn btn-ghost btn-small"
                          >
                            <i className="fas fa-chevron-left"></i>
                            Previous
                          </button>

                          <span className="pagination-current">
                      Page {pagination.currentPage + 1} of {pagination.totalPages}
                    </span>

                          <button
                              onClick={() => handlePageChange(pagination.currentPage + 1)}
                              disabled={pagination.currentPage >= pagination.totalPages - 1}
                              className="btn btn-ghost btn-small"
                          >
                            Next
                            <i className="fas fa-chevron-right"></i>
                          </button>
                        </div>
                      </div>
                  )}
                </>
            ) : (
                <div className="replies-empty">
                  <div className="empty-state">
                    <i className="fas fa-bookmark empty-icon"></i>
                    <h3>No Saved Replies</h3>
                    <p>
                      {currentView === 'search'
                          ? `No replies found matching "${searchQuery}"`
                          : currentView === 'favorites'
                              ? "You haven't marked any replies as favorites yet"
                              : "You haven't saved any email replies yet. Generate some replies to get started!"
                      }
                    </p>
                    {currentView === 'all' && (
                        <button
                            className="btn btn-primary"
                            onClick={() => onViewChange && onViewChange('generator')}
                        >
                          <i className="fas fa-magic"></i>
                          Generate Replies
                        </button>
                    )}
                  </div>
                </div>
            )}
          </div>
        </div>
      </div>
  );
};

export default SavedReplies;