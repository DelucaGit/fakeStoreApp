import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShoppingBag, LogOut, Package, User } from 'lucide-react';

export const Navbar = () => {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-brand">
          <ShoppingBag className="icon" />
          <span>FakeStore</span>
        </Link>
        <div className="navbar-links">
          {isAuthenticated ? (
            <>
              <Link to="/products" className="nav-link">
                <Package className="icon-small" /> Products
              </Link>
              <Link to="/orders" className="nav-link">
                <ShoppingBag className="icon-small" /> My Orders
              </Link>
              <button onClick={handleLogout} className="btn-logout">
                <LogOut className="icon-small" /> Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="nav-link">
                <User className="icon-small" /> Login
              </Link>
              <Link to="/register" className="nav-link btn-primary-outline">
                Register
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
};
