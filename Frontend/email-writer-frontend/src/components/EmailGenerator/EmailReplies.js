import React, { useState } from 'react';
import { replyService } from '../../services/replyService';
import { useNotification } from '../../hooks/useNotification';
import { copyToClipboard } from '../../utils/helpers';
import LoadingSpinner from '../Common/LoadingSpinner';
import Modal from '../Common/Modal';
import './EmailGenerator.css';

const EmailReplies = ({ replies, onRegenerate, onClear, canRegenerate, loading }) => {
  const { showSuccess, showError } = useNotification();
  const [savingIndex, setSavingIndex] = useState(null);
  const [expandedReply, setExpandedReply] = useState(null);
  const [summaryExpanded, setSummaryExpanded] = useState(false);

  const handleCopySummary = async () => {
    try {
      await copyToClipboard(replies.summary);
      showSuccess('Email summary copied to clipboard!');
    } catch (error) {
      showError('Failed to copy summary to clipboard');
    }
  };

  const toggleSummaryExpanded = () => {
    setSummaryExpanded(prev => !prev);
  };

  // Helper function to truncate text
  const truncateText = (text, maxLength = 150) => {
    if (!text || text.length <= maxLength) return text;
    return text.slice(0, maxLength) + '...';
  };

  const handleCopyReply = async (reply, index) => {
    try {
      await copyToClipboard(reply);
      showSuccess(`Reply ${index + 1} copied to clipboard!`);
    } catch (error) {
      showError('Failed to copy reply to clipboard');
    }
  };

  const handleSaveReply = async (reply, index) => {
    setSavingIndex(index);
    try {
      await replyService.saveReply({
        emailSubject: 'Generated Reply',
        emailContent: 'Original email content',
        tone: 'professional',
        replyText: reply,
        summary: replies.summary
      });
      showSuccess(`Reply ${index + 1} saved successfully!`);
    } catch (error) {
      showError('Failed to save reply');
    } finally {
      setSavingIndex(null);
    }
  };

  const handleExpandReply = (reply, index) => {
    setExpandedReply({ reply, index });
  };

  const closeExpandedReply = () => {
    setExpandedReply(null);
  };

  if (!replies) return null;

  return (
    <div className="email-replies">
      {/* Email Summary Section */}
      {replies.summary && (
        <div className="reply-card summary-card fade-in">
          <div className="reply-header">
            <h3 className="reply-number">
              <i className="fas fa-file-alt"></i>
              Email Summary
            </h3>
            <div className="reply-actions">
              <button
                onClick={handleCopySummary}
                className="action-btn"
                title="Copy summary to clipboard"
              >
                <i className="fas fa-copy"></i>
              </button>
              {replies.summary && replies.summary.length > 150 && (
                <button
                  onClick={toggleSummaryExpanded}
                  className="action-btn"
                  title={summaryExpanded ? 'Show less' : 'Read more'}
                >
                  <i className={summaryExpanded ? 'fas fa-chevron-up' : 'fas fa-chevron-down'}></i>
                </button>
              )}
            </div>
          </div>
          <div className="reply-content">
            <p className={`reply-text ${summaryExpanded ? 'summary-expanded' : 'summary-collapsed'}`}>
              {summaryExpanded ? replies.summary : truncateText(replies.summary, 150)}
            </p>
          </div>
          <div className="reply-footer">
            <div className="reply-stats">
              <span className="reply-length">{replies.summary.split(' ').length} words</span>
              <span className="reply-chars">{replies.summary.length} characters</span>
            </div>
            {replies.summary && replies.summary.length > 150 && (
              <button
                onClick={toggleSummaryExpanded}
                className="btn btn-outline btn-small"
              >
                {summaryExpanded ? 'Show Less' : 'Read More'}
              </button>
            )}
          </div>
        </div>
      )}

      {/* Generated Replies Section */}
      <div className="replies-header">
        <div className="replies-title-section">
          <h2 className="replies-title">
            <i className="fas fa-reply-all"></i>
            Generated Replies
          </h2>
        </div>

        <div className="replies-actions">
          <button
            onClick={onRegenerate}
            className="btn btn-outline"
            disabled={!canRegenerate || loading}
          >
            {loading ? (
              <>
                <LoadingSpinner size="small" />
                Generating...
              </>
            ) : (
              <>
                <i className="fas fa-sync-alt"></i>
                Regenerate
              </>
            )}
          </button>
          <button
            onClick={onClear}
            className="btn btn-secondary"
            disabled={loading}
          >
            <i className="fas fa-times"></i>
            Clear
          </button>
        </div>
      </div>

      <div className="replies-grid">
        {replies.replies && replies.replies.map((reply, index) => (
          <div key={index} className="reply-card">
            <div className="reply-header">
              <h3 className="reply-number">
                <i className="fas fa-comment-alt"></i>
                Reply Option {index + 1}
              </h3>
              <div className="reply-actions">
                <button
                  onClick={() => handleCopyReply(reply, index)}
                  className="action-btn"
                  title="Copy to clipboard"
                >
                  <i className="fas fa-copy"></i>
                </button>
                <button
                  onClick={() => handleSaveReply(reply, index)}
                  className="action-btn"
                  disabled={savingIndex === index}
                  title="Save reply"
                >
                  {savingIndex === index ? (
                    <i className="fas fa-spinner fa-spin"></i>
                  ) : (
                    <i className="fas fa-bookmark"></i>
                  )}
                </button>
                <button
                  onClick={() => handleExpandReply(reply, index)}
                  className="action-btn"
                  title="View full reply"
                >
                  <i className="fas fa-expand"></i>
                </button>
              </div>
            </div>

            <div className="reply-content">
              <p className="reply-text">{reply}</p>
            </div>

            <div className="reply-footer">
              <div className="reply-stats">
                <span className="reply-length">
                  {reply.split(' ').length} words
                </span>
                <span className="reply-chars">
                  {reply.length} characters
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Expanded Reply Modal */}
      {expandedReply && (
        <Modal
          isOpen={true}
          onClose={closeExpandedReply}
          title={`Reply Option ${expandedReply.index + 1} - Full View`}
          size="large"
        >
          <div className="expanded-reply">
            <div className="expanded-reply-content">
              <p className="expanded-reply-text">{expandedReply.reply}</p>
            </div>
            
            <div className="expanded-reply-actions">
              <button
                onClick={() => handleCopyReply(expandedReply.reply, expandedReply.index)}
                className="btn btn-outline"
              >
                <i className="fas fa-copy"></i>
                Copy to Clipboard
              </button>
              
              <button
                onClick={() => handleSaveReply(expandedReply.reply, expandedReply.index)}
                className="btn btn-primary"
                disabled={savingIndex === expandedReply.index}
              >
                {savingIndex === expandedReply.index ? (
                  <>
                    <LoadingSpinner size="small" />
                    Saving...
                  </>
                ) : (
                  <>
                    <i className="fas fa-bookmark"></i>
                    Save Reply
                  </>
                )}
              </button>
            </div>

            <div className="expanded-reply-stats">
              <div className="stat-item">
                <i className="fas fa-file-word"></i>
                <span>{expandedReply.reply.split(' ').length} words</span>
              </div>
              <div className="stat-item">
                <i className="fas fa-font"></i>
                <span>{expandedReply.reply.length} characters</span>
              </div>
              <div className="stat-item">
                <i className="fas fa-paragraph"></i>
                <span>{expandedReply.reply.split('\n').length} paragraphs</span>
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default EmailReplies;