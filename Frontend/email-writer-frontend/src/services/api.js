import { API_BASE_URL } from '../utils/constants';
import { getStoredToken, removeStoredToken } from '../utils/storage';

class ApiService {
  constructor() {
    this.baseURL = API_BASE_URL;
  }

  // Create request headers
  getHeaders(includeAuth = true) {
    const headers = {
      'Content-Type': 'application/json',
    };

    if (includeAuth) {
      const token = getStoredToken();
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
    }

    return headers;
  }

  // Handle API responses
  async handleResponse(response) {
    if (response.status === 401) {
      removeStoredToken();
      window.location.href = '/';
      throw new Error('Authentication failed. Please login again.');
    }

    const contentType = response.headers.get('Content-Type');
    let data;

    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      data = await response.text();
    }

    if (!response.ok) {
      const error = new Error(data.message || data.reason || 'An error occurred');
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  }

  // GET request with AbortController timeout
  async get(endpoint, params = {}, options = {}) {
    const controller = new AbortController();
    const timeout = options.timeout || 40000;
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    try {
      const url = new URL(endpoint, this.baseURL || window.location.origin);

      Object.entries(params).forEach(([key, value]) => {
        if (value !== null && value !== undefined && value !== '') {
          url.searchParams.append(key, value);
        }
      });

      const response = await fetch(url.toString(), {
        method: 'GET',
        headers: this.getHeaders(options.includeAuth !== false),
        signal: controller.signal,
      });

      clearTimeout(timeoutId);
      return this.handleResponse(response);
    } catch (error) {
      clearTimeout(timeoutId);
      if (error.name === 'AbortError') {
        throw new Error('Request timeout. Please try again.');
      }
      throw error;
    }
  }

  // POST request with AbortController timeout
  async post(endpoint, data = {}, options = {}) {
    const controller = new AbortController();
    const timeout = options.timeout || 40000;
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    try {
      const response = await fetch(`${this.baseURL}${endpoint}`, {
        method: 'POST',
        headers: this.getHeaders(options.includeAuth !== false),
        body: JSON.stringify(data),
        signal: controller.signal,
      });

      clearTimeout(timeoutId);
      return this.handleResponse(response);
    } catch (error) {
      clearTimeout(timeoutId);
      if (error.name === 'AbortError') {
        throw new Error('Request timeout. Please try again.');
      }
      throw error;
    }
  }

  // PUT request with AbortController timeout
  async put(endpoint, data = {}, options = {}) {
    const controller = new AbortController();
    const timeout = options.timeout || 40000;
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    try {
      const response = await fetch(`${this.baseURL}${endpoint}`, {
        method: 'PUT',
        headers: this.getHeaders(options.includeAuth !== false),
        body: JSON.stringify(data),
        signal: controller.signal,
      });

      clearTimeout(timeoutId);
      return this.handleResponse(response);
    } catch (error) {
      clearTimeout(timeoutId);
      if (error.name === 'AbortError') {
        throw new Error('Request timeout. Please try again.');
      }
      throw error;
    }
  }

  // DELETE request with AbortController timeout
  async delete(endpoint, options = {}) {
    const controller = new AbortController();
    const timeout = options.timeout || 40000;
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    try {
      const response = await fetch(`${this.baseURL}${endpoint}`, {
        method: 'DELETE',
        headers: this.getHeaders(options.includeAuth !== false),
        signal: controller.signal,
      });

      clearTimeout(timeoutId);
      return this.handleResponse(response);
    } catch (error) {
      clearTimeout(timeoutId);
      if (error.name === 'AbortError') {
        throw new Error('Request timeout. Please try again.');
      }
      throw error;
    }
  }

  // Download file (used by replyService.exportReplies)
  async download(endpoint, filename, options = {}) {
    const response = await fetch(`${this.baseURL}${endpoint}`, {
      method: 'GET',
      headers: this.getHeaders(options.includeAuth !== false),
    });

    if (!response.ok) {
      throw new Error('Download failed');
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
}

export const apiService = new ApiService();