// Read backend URLs from Vite env at build/dev time.
// Set VITE_USER_SERVICE_URL and VITE_PRODUCT_SERVICE_URL in frontend/.env.local
// (or any .env file Vite picks up). See frontend/.env.example for the template.
const requireEnv = (name: string): string => {
  const value = import.meta.env[name];
  if (typeof value !== 'string' || value.trim() === '') {
    throw new Error(
      `Missing required env var "${name}". Add it to frontend/.env.local (see frontend/.env.example).`
    );
  }
  // Strip a trailing slash so we can safely append paths like "/api/users/login".
  return value.trim().replace(/\/+$/, '');
};

export const USER_SERVICE_URL = requireEnv('VITE_USER_SERVICE_URL');
export const PRODUCT_SERVICE_URL = requireEnv('VITE_PRODUCT_SERVICE_URL');

export const ENDPOINTS = {
  users: {
    register: `${USER_SERVICE_URL}/api/users/register`,
    login: `${USER_SERVICE_URL}/api/users/login`,
    refresh: `${USER_SERVICE_URL}/api/users/refresh`,
    logout: `${USER_SERVICE_URL}/api/users/logout`,
  },
  products: {
    list: `${PRODUCT_SERVICE_URL}/api/products`,
    getById: (id: number) => `${PRODUCT_SERVICE_URL}/api/products/${id}`,
  },
  orders: {
    create: `${USER_SERVICE_URL}/api/orders`,
    my: `${USER_SERVICE_URL}/api/orders/my`,
  }
};
