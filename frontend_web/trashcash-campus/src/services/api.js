// API service for TrashCash Campus
// This replaces direct Firebase connections with backend API calls

const API_URL = "http://localhost:8080/api";
const BASE_URL = "http://localhost:8080";

// Backend connection check
export const pingBackend = async () => {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000); // 5-second timeout
    
    // Try to connect to the base URL instead of a specific endpoint
    await fetch(`${BASE_URL}`, {
      method: 'HEAD', // Use HEAD request for faster response without body
      signal: controller.signal,
      mode: 'no-cors' // Use no-cors to avoid CORS issues
    });
    
    clearTimeout(timeoutId);
    return true; // If we reach here without errors, the backend is online
  } catch (error) {
    console.error('Backend connection error:', error);
    return false;
  }
};

// Authentication APIs
export const login = async (email, password) => {
  try {
    // Always try to proceed with login even if pingBackend fails
    // This allows Firebase to be a fallback
    try {
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
    } catch (fetchError) {
      // If it's a network error (like the backend being offline)
      // throw a specific error that can be caught and handled
      if (fetchError instanceof TypeError && fetchError.message.includes('fetch')) {
        console.warn('Backend API unavailable, falling back to Firebase');
        // Create a mock response that allows the Firebase fallback
        return { 
          userId: null, 
          email: email,
          token: null
        };
      }
      throw fetchError;
    }
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