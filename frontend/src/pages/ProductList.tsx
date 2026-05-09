import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { ENDPOINTS } from '../api/endpoints';
import type { ProductResponse } from '../api/types';
import { ShoppingCart, Plus, Package } from 'lucide-react';

export const ProductList = () => {
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const navigate = useNavigate();

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await apiFetch(ENDPOINTS.products.list);
        if (!response.ok) {
          throw new Error('Failed to fetch products');
        }
        const data = await response.json();
        setProducts(data);
      } catch (err: any) {
        setError(err.message || 'Error loading products');
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  const handleBuyNow = (productId: number) => {
    navigate('/orders/new', { state: { productId, quantity: 1 } });
  };

  if (loading) {
    return <div className="loading-spinner">Loading products...</div>;
  }

  if (error) {
    return <div className="alert-error">{error}</div>;
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>Products</h2>
        <p>Discover our latest collection</p>
      </div>

      <div className="products-grid">
        {products.map((product) => (
          <div key={product.id} className="product-card group">
            <div className="product-image-container">
              <img src={product.imageUrl} alt={product.title} className="product-image" />
              <div className="product-overlay">
                <button 
                  onClick={() => handleBuyNow(product.id)}
                  className="btn-primary"
                >
                  <ShoppingCart className="icon-small" /> Buy Now
                </button>
              </div>
            </div>
            <div className="product-info">
              <h3 className="product-title">{product.title}</h3>
              <p className="product-description">{product.description}</p>
              <div className="product-footer">
                <span className="product-price">${product.price.toFixed(2)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
      
      {products.length === 0 && (
        <div className="empty-state">
          <Package className="icon-xl text-muted" />
          <h3>No products found</h3>
          <p>Check back later for new arrivals.</p>
        </div>
      )}
    </div>
  );
};
