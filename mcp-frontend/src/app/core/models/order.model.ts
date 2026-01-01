export interface Order {
  id?: number;
  orderNumber: string;
  customer: CustomerSummary;
  status: OrderStatus;
  orderDate: string;
  expectedDeliveryDate?: string;
  actualDeliveryDate?: string;
  shippingAddress: string;
  billingAddress: string;
  lines: OrderLine[];
  totalAmount: number;
  taxAmount: number;
  notes?: string;
  createdBy?: string;
}

export interface OrderLine {
  id?: number;
  productCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  discountPercent?: number;
  lineTotal: number;
  notes?: string;
}

export interface CustomerSummary {
  id: number;
  customerCode: string;
  companyName: string;
}

export type OrderStatus = 
  | 'DRAFT'
  | 'PENDING_VALIDATION'
  | 'VALIDATED'
  | 'IN_PREPARATION'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'RETURNED';

export interface CreateOrderRequest {
  customerCode: string;
  lines: CreateOrderLineRequest[];
  shippingAddress?: string;
  notes?: string;
  expectedDeliveryDate?: string;
}

export interface CreateOrderLineRequest {
  productCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  discountPercent?: number;
  notes?: string;
}
