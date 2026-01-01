import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatListModule
  ],
  template: `
    <div class="dashboard">
      <header class="dashboard-header">
        <h1>Bienvenue, {{ user()?.username }}</h1>
        <p class="subtitle">Tableau de bord - Gestion Commerciale MCP</p>
      </header>
      
      <div class="stats-grid">
        <mat-card class="stat-card">
          <mat-card-content>
            <mat-icon class="stat-icon orders">shopping_cart</mat-icon>
            <div class="stat-info">
              <span class="stat-value">--</span>
              <span class="stat-label">Commandes</span>
            </div>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/orders" color="primary">Voir les commandes</a>
          </mat-card-actions>
        </mat-card>
        
        <mat-card class="stat-card">
          <mat-card-content>
            <mat-icon class="stat-icon invoices">receipt</mat-icon>
            <div class="stat-info">
              <span class="stat-value">--</span>
              <span class="stat-label">Factures</span>
            </div>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/invoices" color="primary">Voir les factures</a>
          </mat-card-actions>
        </mat-card>
        
        <mat-card class="stat-card">
          <mat-card-content>
            <mat-icon class="stat-icon customers">people</mat-icon>
            <div class="stat-info">
              <span class="stat-value">--</span>
              <span class="stat-label">Clients</span>
            </div>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/customers" color="primary">Voir les clients</a>
          </mat-card-actions>
        </mat-card>
        
        <mat-card class="stat-card highlight">
          <mat-card-content>
            <mat-icon class="stat-icon ai">smart_toy</mat-icon>
            <div class="stat-info">
              <span class="stat-value">IA</span>
              <span class="stat-label">Assistant MCP</span>
            </div>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/chat" color="accent">Ouvrir l'assistant</a>
          </mat-card-actions>
        </mat-card>
      </div>
      
      <div class="content-grid">
        <mat-card class="capabilities-card">
          <mat-card-header>
            <mat-icon mat-card-avatar>security</mat-icon>
            <mat-card-title>Vos capacités MCP</mat-card-title>
            <mat-card-subtitle>Rôle : {{ user()?.role }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <mat-list>
              @for (cap of user()?.capabilities; track cap.name) {
                <mat-list-item>
                  <mat-icon matListItemIcon [class.write-cap]="cap.requiresConfirmation">
                    {{ cap.requiresConfirmation ? 'edit' : 'visibility' }}
                  </mat-icon>
                  <span matListItemTitle>{{ cap.name }}</span>
                  <span matListItemLine>{{ cap.description }}</span>
                  @if (cap.requiresConfirmation) {
                    <mat-chip matListItemMeta>Confirmation requise</mat-chip>
                  }
                </mat-list-item>
              } @empty {
                <mat-list-item>
                  <span matListItemTitle>Aucune capacité disponible</span>
                </mat-list-item>
              }
            </mat-list>
          </mat-card-content>
        </mat-card>
        
        <mat-card class="quick-actions-card">
          <mat-card-header>
            <mat-icon mat-card-avatar>flash_on</mat-icon>
            <mat-card-title>Actions rapides</mat-card-title>
            <mat-card-subtitle>Testez les capacités MCP</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="quick-actions">
              <button mat-stroked-button routerLink="/orders" [queryParams]="{search: 'CMD-20240115-TC001'}">
                <mat-icon>search</mat-icon>
                Rechercher commande CMD-20240115-TC001
              </button>
              <button mat-stroked-button routerLink="/invoices" [queryParams]="{analyze: 'FAC-2024-000123'}">
                <mat-icon>analytics</mat-icon>
                Analyser facture FAC-2024-000123
              </button>
              <button mat-stroked-button routerLink="/customers" [queryParams]="{summary: 'CLI-001'}">
                <mat-icon>summarize</mat-icon>
                Résumé client CLI-001
              </button>
              @if (authService.hasCapability('createOrder')) {
                <button mat-raised-button color="primary" routerLink="/orders/new">
                  <mat-icon>add</mat-icon>
                  Créer une commande
                </button>
              }
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .dashboard {
      max-width: 1400px;
      margin: 0 auto;
    }
    
    .dashboard-header {
      margin-bottom: 32px;
    }
    
    .dashboard-header h1 {
      margin: 0;
      font-size: 28px;
      font-weight: 500;
    }
    
    .subtitle {
      color: #666;
      margin: 8px 0 0;
    }
    
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 24px;
      margin-bottom: 32px;
    }
    
    .stat-card {
      text-align: center;
    }
    
    .stat-card mat-card-content {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
      padding: 24px;
    }
    
    .stat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
    }
    
    .stat-icon.orders { color: #1976d2; }
    .stat-icon.invoices { color: #388e3c; }
    .stat-icon.customers { color: #7b1fa2; }
    .stat-icon.ai { color: #f57c00; }
    
    .stat-info {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
    }
    
    .stat-value {
      font-size: 32px;
      font-weight: 500;
    }
    
    .stat-label {
      color: #666;
    }
    
    .stat-card.highlight {
      background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
    }
    
    .content-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
      gap: 24px;
    }
    
    .capabilities-card mat-icon[mat-card-avatar] {
      background: #e3f2fd;
      color: #1976d2;
      padding: 8px;
      border-radius: 50%;
    }
    
    .write-cap {
      color: #f57c00;
    }
    
    .quick-actions-card mat-icon[mat-card-avatar] {
      background: #fff3e0;
      color: #f57c00;
      padding: 8px;
      border-radius: 50%;
    }
    
    .quick-actions {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    
    .quick-actions button {
      justify-content: flex-start;
    }
    
    .quick-actions button mat-icon {
      margin-right: 8px;
    }
  `]
})
export class DashboardComponent {
  authService = inject(AuthService);
  user = this.authService.currentUser;
}
