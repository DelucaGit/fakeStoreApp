export interface UserResponse {
  accessToken: string;
  refreshToken: string;
}

export interface ProductResponse {
  id: number;
  title: string;
  price: number;
  description: string;
  imageUrl: string;
}

export interface OrderItemRequest {
  productId: number;
  quantity: number;
}

export interface OrderRequest {
  items: OrderItemRequest[];
}

export interface OrderItemResponse {
  productId: number;
  quantity: number;
  priceAtPurchase: number;
  lineTotal: number;
}

export interface OrderResponse {
  orderId: string;
  userId: string;
  totalAmount: number;
  createdAt: string;
  items: OrderItemResponse[];
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
