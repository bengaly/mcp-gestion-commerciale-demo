export interface Customer {
  id?: number;
  customerCode: string;
  companyName: string;
  contactName: string;
  email: string;
  phone: string;
  address: string;
  status: CustomerStatus;
  segment: CustomerSegment;
  creditLimit?: number;
  paymentTermDays: number;
  createdAt?: string;
}

export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PROSPECT';
export type CustomerSegment = 'STANDARD' | 'PREMIUM' | 'VIP' | 'ENTERPRISE';

export interface CustomerActivitySummary {
  customer: Customer;
  totalOrders: number;
  totalRevenue: number;
  totalInvoices: number;
  totalPaid: number;
  totalOutstanding: number;
  unpaidInvoicesCount: number;
  hasOverdueInvoices: boolean;
  generatedAt: string;
}
