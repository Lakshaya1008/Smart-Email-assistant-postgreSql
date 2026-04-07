// API Configuration
const getApiBaseUrl = () => {
  if (process.env.REACT_APP_API_URL) {
    return process.env.REACT_APP_API_URL;
  }
  const hostname = window.location.hostname;
  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return 'http://localhost:8081';
  } else if (hostname.includes('staging')) {
    return '';
  } else {
    return '';
  }
};
export const API_BASE_URL = getApiBaseUrl();

// API Endpoints — versioned at /api/v1
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN:    '/api/v1/auth/login',
    REGISTER: '/api/v1/auth/register',
    TEST:     '/api/v1/auth/test'
  },
  EMAIL: {
    GENERATE:        '/api/v1/email/generate',
    REGENERATE:      '/api/v1/email/regenerate',
    GENERATE_SINGLE: '/api/v1/email/generate-single',
    TEST:            '/api/v1/email/test',
    PING:            '/api/v1/email/ping'
  },
  REPLIES: {
    SAVE:      '/api/v1/replies/save',
    HISTORY:   '/api/v1/replies/history',
    SEARCH:    '/api/v1/replies/search',
    FAVORITES: '/api/v1/replies/favorites',
    FAVORITE:  '/api/v1/replies/{id}/favorite',
    DELETE:    '/api/v1/replies/{id}',
    STATS:     '/api/v1/replies/stats',
    EXPORT:    '/api/v1/replies/export'
  }
};

export const APP_NAME = 'Smart Email Assistant';

export const NOTIFICATION_TYPES = {
  SUCCESS: 'success',
  ERROR:   'error',
  WARNING: 'warning',
  INFO:    'info'
};

export const EMAIL_TONES = [
  { value: 'professional', label: 'Professional' },
  { value: 'casual',       label: 'Casual' },
  { value: 'friendly',     label: 'Friendly' },
  { value: 'formal',       label: 'Formal' },
  { value: 'concise',      label: 'Concise' },
  { value: 'detailed',     label: 'Detailed' }
];

export const LANGUAGE_OPTIONS = [
  { value: 'en', label: 'English' },
  { value: 'es', label: 'Spanish' },
  { value: 'fr', label: 'French' },
  { value: 'de', label: 'German' },
  { value: 'hi', label: 'Hindi' }
];

export const DEFAULT_PAGE_SIZE  = 20;
export const PAGE_SIZE_OPTIONS  = [10, 20, 50, 100];

export const STORAGE_KEYS = {
  TOKEN:       'email_writer_token',
  USER:        'email_writer_user',
  PREFERENCES: 'email_writer_preferences'
};

export const VALIDATION_RULES = {
  USERNAME: {
    MIN_LENGTH: 3,
    MAX_LENGTH: 50,
    PATTERN: /^[a-zA-Z0-9_]+$/
  },
  PASSWORD: { MIN_LENGTH: 6 },
  EMAIL:    { PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/ }
};

export const DEBOUNCE_DELAY     = 500;
export const ANIMATION_DURATION = 300;
export const TOAST_DURATION     = 5000;

// Only CSV export is supported — backend has no JSON export endpoint
export const EXPORT_FORMATS = { CSV: 'csv' };