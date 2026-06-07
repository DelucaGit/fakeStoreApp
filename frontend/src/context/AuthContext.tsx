import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { getAccessToken, setTokens, clearTokens, getRefreshToken } from '../api/client';
import { ENDPOINTS } from '../api/endpoints';
import type { UserResponse } from '../api/types';

interface AuthContextType {
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(!!getAccessToken());

  useEffect(() => {
    const handleUnauthorized = () => {
      setIsAuthenticated(false);
    };
    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const login = async (email: string, password: string) => {
    const response = await fetch(ENDPOINTS.users.login, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || 'Login failed');
    }

    const data: UserResponse = await response.json();
    setTokens(data.accessToken, data.refreshToken);
    setIsAuthenticated(true);
  };

  const register = async (email: string, password: string, firstName: string, lastName: string) => {
    const response = await fetch(ENDPOINTS.users.register, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, firstName, lastName })
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || 'Registration failed');
    }

    const data: UserResponse = await response.json();
    setTokens(data.accessToken, data.refreshToken);
    setIsAuthenticated(true);
  };

  const logout = async () => {
    try {
      const refreshToken = getRefreshToken();
      if (refreshToken) {
        await fetch(ENDPOINTS.users.logout, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${refreshToken}`
          }
        });
      }
    } catch (error) {
      console.error('Logout request failed', error);
    } finally {
      clearTokens();
      setIsAuthenticated(false);
    }
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
