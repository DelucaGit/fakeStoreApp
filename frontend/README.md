# CloudStore Frontend

This is the React frontend for CloudStore. It is built with React, Vite, and TypeScript.

The frontend lets users register, log in, browse products from the product service, create orders, and view their order history.

Live site: <http://ec2-13-49-75-31.eu-north-1.compute.amazonaws.com>

## Features
- **Authentication**: Register, Login, Logout with automatic token refresh handling.
- **Products**: View a list of available products.
- **Orders**: Create new orders and view order history.
- **Secure Routing**: Protected routes for authenticated users.

## Setup Instructions

1. Ensure your backend services are running:
   - User & Order Service on `http://localhost:8080`
   - Product Service on `http://localhost:8082`

2. Configure the backend URLs.

   Create a local env file if needed:

   ```bash
   cp .env.example .env.local
   ```

   For local development, use:

   ```env
   VITE_USER_SERVICE_URL=http://localhost:8080
   VITE_PRODUCT_SERVICE_URL=http://localhost:8082
   ```

   For production builds, `.env.production` points to the deployed AWS services.

3. Install dependencies:

   ```bash
   npm install
   ```

4. Start the development server:

   ```bash
   npm run dev
   ```

The local frontend runs at <http://localhost:5173>.

## Production Build

To build the frontend for production:

```bash
npm run build
```

The output is written to `dist/`. In the current AWS deployment, this build is served by Nginx on the public EC2 instance.

## Architecture & Token Flow
- **State**: React Context (`AuthContext`) manages global authentication state.
- **API Client**: A custom `fetch` wrapper (`src/api/client.ts`) automatically attaches the `Authorization: Bearer <token>` to requests.
- **Token Refresh**: On receiving a 401 Unauthorized from a protected endpoint, the API client automatically pauses the request, calls the `/api/users/refresh` endpoint using the locally stored refresh token, updates the tokens in `localStorage`, and retries the original request seamlessly.
- **Storage**: Tokens are currently stored in `localStorage` for MVP purposes.
- **Environment config**: Backend URLs come from `VITE_USER_SERVICE_URL` and `VITE_PRODUCT_SERVICE_URL`, so local and deployed builds can use different API addresses without changing source code.

## Testing Smoke Checklist
- [ ] Register a new account
- [ ] You should be automatically logged in and redirected to the products page
- [ ] Log out, then Log back in with the new credentials
- [ ] View the Products list
- [ ] Click "Buy Now" on a product
- [ ] Adjust quantity and complete the checkout
- [ ] Navigate to "My Orders" to verify your new order appears
- [ ] Click Logout to clear session and return to Login
