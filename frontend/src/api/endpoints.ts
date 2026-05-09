export const USER_SERVICE_URL = 'http://localhost:8080';
export const PRODUCT_SERVICE_URL = 'http://localhost:8082';

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
