import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProductService } from '../../core/services/product.service';
import { Product, ProductCategory, ProductStatus, PRODUCT_CATEGORIES, PRODUCT_STATUSES } from '../../core/models/product.model';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  template: `
    <div class="products-page">
      <header class="page-header">
        <h1><mat-icon>inventory_2</mat-icon> Catalogue Produits</h1>
        <p>Gérez les produits disponibles pour les commandes</p>
      </header>
      
      <div class="products-container">
        <mat-card class="products-list-card">
          <mat-card-header>
            <mat-icon mat-card-avatar>list</mat-icon>
            <mat-card-title>Liste des produits</mat-card-title>
            <mat-card-subtitle>{{ products().length }} produit(s)</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="filters">
              <mat-form-field appearance="outline" class="search-field">
                <mat-label>Rechercher</mat-label>
                <input matInput [(ngModel)]="searchTerm" placeholder="Nom ou code produit">
                <mat-icon matSuffix>search</mat-icon>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Catégorie</mat-label>
                <mat-select [(ngModel)]="filterCategory" (selectionChange)="applyFilters()">
                  <mat-option value="">Toutes</mat-option>
                  @for (cat of categories; track cat.value) {
                    <mat-option [value]="cat.value">{{ cat.label }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <button mat-raised-button color="primary" (click)="openForm()">
                <mat-icon>add</mat-icon>
                Nouveau produit
              </button>
            </div>
            
            @if (loading()) {
              <div class="loading">
                <mat-spinner diameter="40"></mat-spinner>
              </div>
            } @else {
              <div class="products-table">
                <table mat-table [dataSource]="filteredProducts()">
                  <ng-container matColumnDef="productCode">
                    <th mat-header-cell *matHeaderCellDef>Code</th>
                    <td mat-cell *matCellDef="let product">
                      <strong>{{ product.productCode }}</strong>
                    </td>
                  </ng-container>
                  
                  <ng-container matColumnDef="name">
                    <th mat-header-cell *matHeaderCellDef>Nom</th>
                    <td mat-cell *matCellDef="let product">{{ product.name }}</td>
                  </ng-container>
                  
                  <ng-container matColumnDef="category">
                    <th mat-header-cell *matHeaderCellDef>Catégorie</th>
                    <td mat-cell *matCellDef="let product">
                      <mat-chip [class]="'category-' + product.category.toLowerCase()">
                        {{ getCategoryLabel(product.category) }}
                      </mat-chip>
                    </td>
                  </ng-container>
                  
                  <ng-container matColumnDef="unitPrice">
                    <th mat-header-cell *matHeaderCellDef>Prix unitaire</th>
                    <td mat-cell *matCellDef="let product">
                      {{ product.unitPrice | currency:'EUR':'symbol':'1.2-2' }}
                    </td>
                  </ng-container>
                  
                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef>Statut</th>
                    <td mat-cell *matCellDef="let product">
                      <mat-chip [class]="'status-' + product.status.toLowerCase()">
                        {{ getStatusLabel(product.status) }}
                      </mat-chip>
                    </td>
                  </ng-container>
                  
                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let product">
                      <button mat-icon-button color="primary" 
                              matTooltip="Modifier"
                              (click)="editProduct(product)">
                        <mat-icon>edit</mat-icon>
                      </button>
                      <button mat-icon-button color="warn" 
                              matTooltip="Désactiver"
                              (click)="deleteProduct(product)"
                              [disabled]="product.status !== 'ACTIVE'">
                        <mat-icon>delete</mat-icon>
                      </button>
                    </td>
                  </ng-container>
                  
                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
                </table>
              </div>
            }
          </mat-card-content>
        </mat-card>
        
        @if (showForm()) {
          <mat-card class="product-form-card">
            <mat-card-header>
              <mat-icon mat-card-avatar class="form-icon">{{ editingProduct() ? 'edit' : 'add_circle' }}</mat-icon>
              <mat-card-title>{{ editingProduct() ? 'Modifier le produit' : 'Nouveau produit' }}</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <form (ngSubmit)="saveProduct()" class="product-form">
                <mat-form-field appearance="outline">
                  <mat-label>Code produit</mat-label>
                  <input matInput [(ngModel)]="formData.productCode" name="productCode" 
                         required [disabled]="!!editingProduct()">
                  <mat-icon matSuffix>tag</mat-icon>
                </mat-form-field>
                
                <mat-form-field appearance="outline">
                  <mat-label>Nom</mat-label>
                  <input matInput [(ngModel)]="formData.name" name="name" required>
                </mat-form-field>
                
                <mat-form-field appearance="outline">
                  <mat-label>Description</mat-label>
                  <textarea matInput [(ngModel)]="formData.description" name="description" rows="3"></textarea>
                </mat-form-field>
                
                <mat-form-field appearance="outline">
                  <mat-label>Catégorie</mat-label>
                  <mat-select [(ngModel)]="formData.category" name="category" required>
                    @for (cat of categories; track cat.value) {
                      <mat-option [value]="cat.value">{{ cat.label }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                
                <div class="form-row">
                  <mat-form-field appearance="outline">
                    <mat-label>Prix unitaire (€)</mat-label>
                    <input matInput type="number" [(ngModel)]="formData.unitPrice" name="unitPrice" 
                           required min="0" step="0.01">
                    <mat-icon matSuffix>euro</mat-icon>
                  </mat-form-field>
                  
                  <mat-form-field appearance="outline">
                    <mat-label>Stock</mat-label>
                    <input matInput type="number" [(ngModel)]="formData.stockQuantity" name="stockQuantity" min="0">
                  </mat-form-field>
                </div>
                
                <div class="form-row">
                  <mat-form-field appearance="outline">
                    <mat-label>Unité</mat-label>
                    <input matInput [(ngModel)]="formData.unit" name="unit" placeholder="ex: unité, licence, heure">
                  </mat-form-field>
                  
                  <mat-form-field appearance="outline">
                    <mat-label>Statut</mat-label>
                    <mat-select [(ngModel)]="formData.status" name="status" required>
                      @for (status of statuses; track status.value) {
                        <mat-option [value]="status.value">{{ status.label }}</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                </div>
                
                <div class="form-actions">
                  <button mat-button type="button" (click)="cancelForm()">Annuler</button>
                  <button mat-raised-button color="primary" type="submit" [disabled]="saving()">
                    @if (saving()) {
                      <mat-spinner diameter="20"></mat-spinner>
                    } @else {
                      <mat-icon>save</mat-icon>
                      {{ editingProduct() ? 'Mettre à jour' : 'Créer' }}
                    }
                  </button>
                </div>
              </form>
            </mat-card-content>
          </mat-card>
        }
      </div>
    </div>
  `,
  styles: [`
    .products-page {
      max-width: 1400px;
      margin: 0 auto;
    }
    
    .page-header {
      margin-bottom: 24px;
    }
    
    .page-header h1 {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0;
      font-size: 28px;
    }
    
    .page-header p {
      color: #666;
      margin: 8px 0 0;
    }
    
    .products-container {
      display: grid;
      grid-template-columns: 1fr 400px;
      gap: 24px;
    }
    
    @media (max-width: 1100px) {
      .products-container {
        grid-template-columns: 1fr;
      }
    }
    
    mat-icon[mat-card-avatar] {
      background: #e3f2fd;
      color: #1976d2;
      padding: 8px;
      border-radius: 50%;
    }
    
    .form-icon {
      background: #e8f5e9 !important;
      color: #388e3c !important;
    }
    
    .filters {
      display: flex;
      gap: 16px;
      align-items: center;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }
    
    .search-field {
      flex: 1;
      min-width: 200px;
    }
    
    .loading {
      display: flex;
      justify-content: center;
      padding: 40px;
    }
    
    .products-table {
      overflow-x: auto;
    }
    
    table {
      width: 100%;
    }
    
    .category-software { background: #e3f2fd !important; color: #1565c0 !important; }
    .category-hardware { background: #fce4ec !important; color: #c2185b !important; }
    .category-service { background: #e8f5e9 !important; color: #2e7d32 !important; }
    .category-subscription { background: #fff3e0 !important; color: #ef6c00 !important; }
    .category-accessory { background: #f3e5f5 !important; color: #7b1fa2 !important; }
    
    .status-active { background: #e8f5e9 !important; color: #2e7d32 !important; }
    .status-inactive { background: #fff3e0 !important; color: #ef6c00 !important; }
    .status-discontinued { background: #ffebee !important; color: #c62828 !important; }
    
    .product-form-card {
      height: fit-content;
    }
    
    .product-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    
    .product-form mat-form-field {
      width: 100%;
    }
    
    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }
    
    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 16px;
    }
  `]
})
export class ProductsComponent implements OnInit {
  private productService = inject(ProductService);
  private snackBar = inject(MatSnackBar);
  
  products = signal<Product[]>([]);
  filteredProducts = signal<Product[]>([]);
  loading = signal(false);
  saving = signal(false);
  showForm = signal(false);
  editingProduct = signal<Product | null>(null);
  
  searchTerm = '';
  filterCategory = '';
  
  categories = PRODUCT_CATEGORIES;
  statuses = PRODUCT_STATUSES;
  displayedColumns = ['productCode', 'name', 'category', 'unitPrice', 'status', 'actions'];
  
  formData: Partial<Product> = this.getEmptyForm();
  
  ngOnInit(): void {
    this.loadProducts();
  }
  
  loadProducts(): void {
    this.loading.set(true);
    this.productService.getAll().subscribe({
      next: (products) => {
        this.products.set(products);
        this.applyFilters();
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open('Erreur lors du chargement des produits', 'Fermer', { duration: 5000 });
      }
    });
  }
  
  applyFilters(): void {
    let filtered = this.products();
    
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(p => 
        p.name.toLowerCase().includes(term) || 
        p.productCode.toLowerCase().includes(term)
      );
    }
    
    if (this.filterCategory) {
      filtered = filtered.filter(p => p.category === this.filterCategory);
    }
    
    this.filteredProducts.set(filtered);
  }
  
  getCategoryLabel(category: ProductCategory): string {
    return this.categories.find(c => c.value === category)?.label || category;
  }
  
  getStatusLabel(status: ProductStatus): string {
    return this.statuses.find(s => s.value === status)?.label || status;
  }
  
  openForm(): void {
    this.editingProduct.set(null);
    this.formData = this.getEmptyForm();
    this.showForm.set(true);
  }
  
  editProduct(product: Product): void {
    this.editingProduct.set(product);
    this.formData = { ...product };
    this.showForm.set(true);
  }
  
  cancelForm(): void {
    this.showForm.set(false);
    this.editingProduct.set(null);
    this.formData = this.getEmptyForm();
  }
  
  saveProduct(): void {
    if (!this.formData.productCode || !this.formData.name || !this.formData.category || !this.formData.unitPrice) {
      this.snackBar.open('Veuillez remplir tous les champs obligatoires', 'Fermer', { duration: 3000 });
      return;
    }
    
    this.saving.set(true);
    
    const product = this.formData as Product;
    const operation = this.editingProduct() 
      ? this.productService.update(this.editingProduct()!.id!, product)
      : this.productService.create(product);
    
    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open(
          this.editingProduct() ? 'Produit mis à jour' : 'Produit créé', 
          'Fermer', 
          { duration: 3000 }
        );
        this.cancelForm();
        this.loadProducts();
      },
      error: (err) => {
        this.saving.set(false);
        this.snackBar.open('Erreur lors de l\'enregistrement', 'Fermer', { duration: 5000 });
      }
    });
  }
  
  deleteProduct(product: Product): void {
    if (confirm(`Voulez-vous vraiment désactiver le produit "${product.name}" ?`)) {
      this.productService.delete(product.id!).subscribe({
        next: () => {
          this.snackBar.open('Produit désactivé', 'Fermer', { duration: 3000 });
          this.loadProducts();
        },
        error: () => {
          this.snackBar.open('Erreur lors de la désactivation', 'Fermer', { duration: 5000 });
        }
      });
    }
  }
  
  private getEmptyForm(): Partial<Product> {
    return {
      productCode: '',
      name: '',
      description: '',
      category: 'SOFTWARE',
      unitPrice: 0,
      stockQuantity: undefined,
      status: 'ACTIVE',
      unit: 'unité'
    };
  }
}
