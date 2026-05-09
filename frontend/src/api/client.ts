import { ENDPOINTS } from './endpoints';
import type { UserResponse } from './types';

let isRefreshing = false;
let failedQueue: { resolve: (token: string) => void; reject: (err: any) => void }[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token as string);
    }
  });
  failedQueue = [];
};

export const getAccessToken = () => localStorage.getItem('accessToken');
export const getRefreshToken = () => localStorage.getItem('refreshToken');

export const setTokens = (accessToken: string, refreshToken: string) => {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
};

export const clearTokens = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
};

export async function apiFetch(url: string, options: RequestInit = {}): Promise<Response> {
  const token = getAccessToken();
  const headers = new Headers(options.headers);
  
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  
  if (!headers.has('Content-Type') && options.method && options.method !== 'GET') {
    headers.set('Content-Type', 'application/json');
  }

  const newOptions = { ...options, headers };
  let response = await fetch(url, newOptions);
  
  if (response.status === 401 && !url.includes('/api/users/refresh') && !url.includes('/api/users/login') && !url.includes('/api/users/register')) {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      clearTokens();
      window.dispatchEvent(new Event('auth:unauthorized'));
      return response;
    }
    
    if (isRefreshing) {
      return new Promise<Response>((resolve, reject) => {
        failedQueue.push({
          resolve: (newToken) => {
            const retryHeaders = new Headers(newOptions.headers);
            retryHeaders.set('Authorization', `Bearer ${newToken}`);
            resolve(fetch(url, { ...newOptions, headers: retryHeaders }));
          },
          reject: (err) => reject(err)
        });
      });
    }
    
    isRefreshing = true;
    
    try {
      const refreshResponse = await fetch(ENDPOINTS.users.refresh, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ refreshToken })
      });
      
      if (!refreshResponse.ok) {
        throw new Error('Refresh failed');
      }
      
      const data: UserResponse = await refreshResponse.json();
      setTokens(data.accessToken, data.refreshToken);
      processQueue(null, data.accessToken);
      
      const retryHeaders = new Headers(newOptions.headers);
      retryHeaders.set('Authorization', `Bearer ${data.accessToken}`);
      response = await fetch(url, { ...newOptions, headers: retryHeaders });
    } catch (err) {
      processQueue(err, null);
      clearTokens();
      window.dispatchEvent(new Event('auth:unauthorized'));
    } finally {
      isRefreshing = false;
    }
  }
  
  return response;
}
