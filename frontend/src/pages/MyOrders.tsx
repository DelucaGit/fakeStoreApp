import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { ENDPOINTS } from '../api/endpoints';
import type { OrderResponse, ProductResponse } from '../api/types';
import { Package, Clock } from 'lucide-react';

export const MyOrders = () => {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Quick cache for product names if we want to display them nicely
  // In a real app, backend would return product names with orders
  const [productCache, setProductCache] = useState<Record<number, string>>({});

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        const response = await apiFetch(ENDPOINTS.orders.my);
        if (!response.ok) throw new Error('Failed to fetch orders');
        const data = await response.json();
        
        // Sort orders by newest first
        data.sort((a: OrderResponse, b: OrderResponse) => 
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        
        setOrders(data);
        
        // Fetch product names for the items
        const productIds = new Set<number>();
        data.forEach((o: OrderResponse) => o.items.forEach(i => productIds.add(i.productId)));
        
        const cache: Record<number, string> = {};
        for (const pid of productIds) {
          try {
            const pRes = await apiFetch(ENDPOINTS.products.getById(pid));
            if (pRes.ok) {
              const pData: ProductResponse = await pRes.json();
              cache[pid] = pData.title;
            }
          } catch (e) {
            // Ignore individual product fetch errors
          }
        }
        setProductCache(cache);
        
      } catch (err: any) {
        setError(err.message || 'Error loading orders');
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, []);

  if (loading) {
    return <div className="page-container"><div className="loading-spinner">Loading orders...</div></div>;
  }

  if (error) {
    return <div className="page-container"><div className="alert-error">{error}</div></div>;
  }

  return (
    <div className="page-container max-w-4xl mx-auto">
      <div className="page-header">
        <h2>My Orders</h2>
        <p>View your order history</p>
      </div>

      {orders.length === 0 ? (
        <div className="empty-state">
          <Package className="icon-xl text-muted" />
          <h3>No orders yet</h3>
          <p>When you buy products, your orders will appear here.</p>
        </div>
      ) : (
        <div className="orders-list">
          {orders.map((order) => (
            <div key={order.orderId} className="order-card card">
              <div className="order-header">
                <div className="order-meta">
                  <span className="order-id">Order #{order.orderId.substring(0, 8)}</span>
                  <span className="order-date">
                    <Clock className="icon-xs" /> 
                    {new Date(order.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <div className="order-total">
                  Total: <strong>${order.totalAmount.toFixed(2)}</strong>
                </div>
              </div>
              
              <div className="order-items">
                {order.items.map((item, idx) => (
                  <div key={idx} className="order-item-row">
                    <div className="item-details">
                      <span className="item-qty">{item.quantity}x</span>
                      <span className="item-name">
                        {productCache[item.productId] || `Product ID: ${item.productId}`}
                      </span>
                    </div>
                    <div className="item-price">
                      ${item.lineTotal.toFixed(2)}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
