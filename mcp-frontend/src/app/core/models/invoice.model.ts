import { CustomerSummary } from './order.model';

export interface Invoice {
  id?: number;
  invoiceNumber: string;
  customer: CustomerSummary;
  order?: OrderSummary;
  status: InvoiceStatus;
  issueDate: string;
  dueDate: string;
  paidDate?: string;
  totalAmount: number;
  taxAmount: number;
  paidAmount: number;
  remainingAmount: number;
  lines: InvoiceLine[];
  notes?: string;
}

export interface InvoiceLine {
  id?: number;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderSummary {
  id: number;
  orderNumber: string;
}

export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface InvoiceAnalysis {
  invoiceNumber: string;
  customerName: string;
  customerCode: string;
  status: string;
  totalAmount: number;
  paidAmount: number;
  remainingAmount: number;
  paidPercentage: number;
  issueDate: string;
  dueDate: string;
  isOverdue: boolean;
  daysOverdue: number;
  riskLevel: string;
  recommendations: string[];
  customerTotalPaid: number;
  customerTotalOutstanding: number;
  customerInvoiceCount: number;
}
