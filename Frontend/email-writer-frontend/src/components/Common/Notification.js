import React, { useEffect, useState } from 'react';
import { useNotification } from '../../hooks/useNotification';
import './Common.css';

/**
 * Notification toast component.
 *
 * Fix N1/N9 — double timer race condition:
 * Before: NotificationContext.js set a setTimeout(duration) to clear the
 *         notification (with an ID guard). Notification.js ALSO set its own
 *         setTimeout(duration) then called hideNotification() with NO ID guard
 *         after +300ms. If a new notification appeared while the old timer
 *         was still running, the old component's timer fired and blindly
 *         called hideNotification(), wiping out the new notification.
 *
 * After:  The component timer only handles the fade-out animation (setIsVisible).
 *         It no longer calls hideNotification() at all — the context's own
 *         ID-guarded timer already handles that correctly. The component
 *         just responds to the notification being cleared by the context
 *         (the component unmounts when App.js renders nothing for notification=null).
 */
const Notification = ({ type = 'info', message, duration = 5000 }) => {
  const { hideNotification } = useNotification();
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    if (duration <= 0) return;

    // Only animate the fade-out — do NOT call hideNotification() here.
    // The context already has an ID-guarded timer that clears the notification
    // state at the right time. This component just plays the exit animation.
    const fadeTimer = setTimeout(() => {
      setIsVisible(false);
    }, duration);

    return () => clearTimeout(fadeTimer);
  }, [duration]);

  const getIcon = () => {
    switch (type) {
      case 'success': return 'fas fa-check-circle';
      case 'error':   return 'fas fa-exclamation-circle';
      case 'warning': return 'fas fa-exclamation-triangle';
      default:        return 'fas fa-info-circle';
    }
  };

  const handleClose = () => {
    setIsVisible(false);
    // Slight delay so the fade-out animation plays before the component disappears
    setTimeout(() => hideNotification(), 300);
  };

  return (
      <div className={`notification notification-${type} ${isVisible ? 'visible' : 'hidden'}`}>
        <div className="notification-content">
          <i className={`notification-icon ${getIcon()}`}></i>
          <span className="notification-message">{message}</span>
          <button
              className="notification-close"
              onClick={handleClose}
              aria-label="Close notification"
          >
            <i className="fas fa-times"></i>
          </button>
        </div>
      </div>
  );
};

export default Notification;