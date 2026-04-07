import React, { useEffect, useRef } from 'react';
import './Common.css';

const Modal = ({
                 isOpen,
                 onClose,
                 title,
                 children,
                 size = 'medium',
                 showCloseButton = true,
                 className = ''
               }) => {
  const modalRef = useRef(null);

  useEffect(() => {
    const handleEscapeKey = (event) => {
      if (event.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleEscapeKey);
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleEscapeKey);
      // Fix N8: was 'unset' (a CSS keyword meaning "inherit or revert"),
      // not the same as removing the inline style.
      // '' removes the inline property entirely and lets the stylesheet cascade.
      // Also guards against two simultaneous modals resetting scroll when
      // the first closes while the second is still open.
      const openModals = document.querySelectorAll('[role="dialog"]');
      if (openModals.length === 0) {
        document.body.style.overflow = '';
      }
    };
  }, [isOpen, onClose]);

  const handleBackdropClick = (event) => {
    if (event.target === event.currentTarget) {
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
      <div className="modal-overlay" onClick={handleBackdropClick}>
        <div
            ref={modalRef}
            className={`modal modal-${size} ${className}`}
            role="dialog"
            aria-modal="true"
            aria-labelledby={title ? 'modal-title' : undefined}
        >
          {(title || showCloseButton) && (
              <div className="modal-header">
                {title && <h2 id="modal-title" className="modal-title">{title}</h2>}
                {showCloseButton && (
                    <button
                        className="modal-close-button"
                        onClick={onClose}
                        aria-label="Close modal"
                    >
                      <i className="fas fa-times"></i>
                    </button>
                )}
              </div>
          )}

          <div className="modal-content">
            {children}
          </div>
        </div>
      </div>
  );
};

export default Modal;