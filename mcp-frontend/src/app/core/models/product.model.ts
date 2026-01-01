export interface Product {
  id?: number;
  productCode: string;
  name: string;
  description?: string;
  category: ProductCategory;
  unitPrice: number;
  stockQuantity?: number;
  status: ProductStatus;
  unit?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type ProductCategory = 'SOFTWARE' | 'HARDWARE' | 'SERVICE' | 'SUBSCRIPTION' | 'ACCESSORY';

export type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED';

export const PRODUCT_CATEGORIES: { value: ProductCategory; label: string }[] = [
  { value: 'SOFTWARE', label: 'Logiciel' },
  { value: 'HARDWARE', label: 'Matériel' },
  { value: 'SERVICE', label: 'Service' },
  { value: 'SUBSCRIPTION', label: 'Abonnement' },
  { value: 'ACCESSORY', label: 'Accessoire' }
];

export const PRODUCT_STATUSES: { value: ProductStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Actif' },
  { value: 'INACTIVE', label: 'Inactif' },
  { value: 'DISCONTINUED', label: 'Discontinué' }
];
