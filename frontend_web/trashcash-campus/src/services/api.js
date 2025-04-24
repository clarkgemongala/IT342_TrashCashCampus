// API service for TrashCash Campus
// This replaces direct Firebase connections with backend API calls

const API_URL = "http://localhost:8080/api";
const BASE_URL = "http://localhost:8080";

// Backend connection check
export const pingBackend = async () => {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000); // 5-second timeout
    
    // Use a specific API endpoint instead of the base URL
    const response = await fetch(`${API_URL}/health`, {
      method: 'GET',
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
      }
    });
    
    clearTimeout(timeoutId);
    return response.ok; // Return true only if the response is ok
  } catch (error) {
    console.error('Backend connection error:', error);
    return false;
  }
};

// Authentication APIs
export const login = async (email, password) => {
  try {
    // First, check if the backend is available
    const isBackendAvailable = await pingBackend();
    
    if (!isBackendAvailable) {
      throw new Error('Backend service is unavailable');
    }
    
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Login failed');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
};

export const register = async (email, password, name) => {
  try {
    const response = await fetch(`${API_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password, name }),
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Registration failed');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Registration error:', error);
    throw error;
  }
};

export const requestPasswordReset = async (email) => {
  try {
    const response = await fetch(`${API_URL}/auth/request-password-reset`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email }),
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Password reset request failed');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Password reset request error:', error);
    throw error;
  }
};

// User Profile APIs
export const getProfile = async (userId, token) => {
  try {
    const response = await fetch(`${API_URL}/users/${userId}/profile`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch profile');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get profile error:', error);
    throw error;
  }
};

export const updateProfile = async (userId, token, profileData) => {
  try {
    const response = await fetch(`${API_URL}/users/${userId}/profile`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify(profileData),
    });
    
    if (!response.ok) {
      throw new Error('Failed to update profile');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Update profile error:', error);
    throw error;
  }
};

export const updateEmail = async (userId, token, email) => {
  try {
    const response = await fetch(`${API_URL}/users/${userId}/profile/email`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify({ email }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to update email');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Update email error:', error);
    throw error;
  }
};

export const updatePassword = async (userId, token, oldPassword, newPassword) => {
  try {
    const response = await fetch(`${API_URL}/users/${userId}/password`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify({ oldPassword, newPassword }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to update password');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Update password error:', error);
    throw error;
  }
};

// Pickup Location APIs
export const getPickupLocations = async () => {
  try {
    const response = await fetch(`${API_URL}/pickup-locations`);
    
    if (!response.ok) {
      throw new Error('Failed to fetch pickup locations');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get pickup locations error:', error);
    throw error;
  }
};

export const getPickupLocationById = async (id) => {
  try {
    const response = await fetch(`${API_URL}/pickup-locations/${id}`);
    
    if (!response.ok) {
      throw new Error('Failed to fetch pickup location');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Get pickup location error:', error);
    throw error;
  }
}; 