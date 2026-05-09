import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { apiFetch } from '../api/client';
import { ENDPOINTS } from '../api/endpoints';
import type { ProductResponse, OrderRequest } from '../api/types';
import { CreditCard, ArrowLeft } from 'lucide-react';

export const CreateOrder = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [quantity, setQuantity] = useState(location.state?.quantity || 1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const productId = location.state?.productId;

  useEffect(() => {
    if (!productId) {
      navigate('/products');
      return;
    }

    const fetchProduct = async () => {
      try {
        const response = await apiFetch(ENDPOINTS.products.getById(productId));
        if (!response.ok) throw new Error('Product not found');
        const data = await response.json();
        setProduct(data);
      } catch (err: any) {
        setError('Failed to load product details');
      }
    };

    fetchProduct();
  }, [productId, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!product) return;
    
    setLoading(true);
    setError('');
    
    try {
      const orderRequest: OrderRequest = {
        items: [{ productId: product.id, quantity }]
      };
      
      const response = await apiFetch(ENDPOINTS.orders.create, {
        method: 'POST',
        body: JSON.stringify(orderRequest)
      });
      
      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.message || 'Failed to create order');
      }
      
      navigate('/orders');
    } catch (err: any) {
      setError(err.message || 'Error creating order');
      setLoading(false);
    }
  };

  if (error && !product) {
    return <div className="page-container"><div className="alert-error">{error}</div></div>;
  }

  if (!product) {
    return <div className="page-container"><div className="loading-spinner">Loading checkout...</div></div>;
  }

  const total = product.price * quantity;

  return (
    <div className="page-container max-w-2xl mx-auto">
      <button onClick={() => navigate('/products')} className="btn-back">
        <ArrowLeft className="icon-small" /> Back to Products
      </button>

      <div className="card mt-4">
        <div className="card-header">
          <h2>Checkout</h2>
          <p>Review your order details</p>
        </div>
        
        {error && <div className="alert-error mx-4">{error}</div>}

        <div className="checkout-preview">
          <div className="checkout-product">
            <img src={product.imageUrl} alt={product.title} className="checkout-image" />
            <div className="checkout-details">
              <h3>{product.title}</h3>
              <p className="text-muted">${product.price.toFixed(2)}</p>
            </div>
          </div>
          
          <form onSubmit={handleSubmit} className="form checkout-form">
            <div className="form-group flex-row">
              <label>Quantity</label>
              <input 
                type="number" 
                min="1" 
                max="10" 
                value={quantity} 
                onChange={(e) => setQuantity(parseInt(e.target.value) || 1)} 
                className="quantity-input"
              />
            </div>
            
            <div className="checkout-summary">
              <div className="summary-row">
                <span>Subtotal</span>
                <span>${total.toFixed(2)}</span>
              </div>
              <div className="summary-row font-bold">
                <span>Total</span>
                <span>${total.toFixed(2)}</span>
              </div>
            </div>

            <button type="submit" disabled={loading} className="btn-primary w-full mt-4">
              <CreditCard className="icon-small" /> {loading ? 'Processing...' : `Pay $${total.toFixed(2)}`}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
