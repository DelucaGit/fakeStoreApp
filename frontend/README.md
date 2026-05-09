# FakeStore Frontend MVP

This is the frontend MVP for the FakeStore application. It is built using React, Vite, and TypeScript.
It provides a clean, minimal, yet premium user interface without relying on heavy UI frameworks.

## Features
- **Authentication**: Register, Login, Logout with automatic token refresh handling.
- **Products**: View a list of available products.
- **Orders**: Create new orders and view order history.
- **Secure Routing**: Protected routes for authenticated users.

## Setup Instructions

1. Ensure your backend services are running:
   - User & Order Service on `http://localhost:8080`
   - Product Service on `http://localhost:8082`

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

## Architecture & Token Flow
- **State**: React Context (`AuthContext`) manages global authentication state.
- **API Client**: A custom `fetch` wrapper (`src/api/client.ts`) automatically attaches the `Authorization: Bearer <token>` to requests.
- **Token Refresh**: On receiving a 401 Unauthorized from a protected endpoint, the API client automatically pauses the request, calls the `/api/users/refresh` endpoint using the locally stored refresh token, updates the tokens in `localStorage`, and retries the original request seamlessly.
- **Storage**: Tokens are currently stored in `localStorage` for MVP purposes.

## Testing Smoke Checklist
- [ ] Register a new account
- [ ] You should be automatically logged in and redirected to the products page
- [ ] Log out, then Log back in with the new credentials
- [ ] View the Products list
- [ ] Click "Buy Now" on a product
- [ ] Adjust quantity and complete the checkout
- [ ] Navigate to "My Orders" to verify your new order appears
- [ ] Click Logout to clear session and return to Login
