import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatChipsModule,
    MatTooltipModule,
    MatDividerModule
  ],
  template: `
    <mat-toolbar color="primary" class="navbar">
      <div class="navbar-brand">
        <mat-icon>business</mat-icon>
        <span class="brand-text">MCP Enterprise</span>
      </div>
      
      <nav class="navbar-nav">
        <a mat-button routerLink="/dashboard" routerLinkActive="active">
          <mat-icon>dashboard</mat-icon>
          Tableau de bord
        </a>
        <a mat-button routerLink="/orders" routerLinkActive="active">
          <mat-icon>shopping_cart</mat-icon>
          Commandes
        </a>
        <a mat-button routerLink="/invoices" routerLinkActive="active">
          <mat-icon>receipt</mat-icon>
          Factures
        </a>
        <a mat-button routerLink="/customers" routerLinkActive="active">
          <mat-icon>people</mat-icon>
          Clients
        </a>
        <a mat-button routerLink="/products" routerLinkActive="active">
          <mat-icon>inventory_2</mat-icon>
          Produits
        </a>
        <a mat-button routerLink="/chat" routerLinkActive="active" class="chat-btn">
          <mat-icon>smart_toy</mat-icon>
          Assistant IA
        </a>
      </nav>
      
      <div class="navbar-user">
        <mat-chip-set>
          <mat-chip [highlighted]="true" [matTooltip]="roleTooltip()">
            <mat-icon matChipAvatar>{{ roleIcon() }}</mat-icon>
            {{ user()?.role }}
          </mat-chip>
        </mat-chip-set>
        
        <button mat-icon-button [matMenuTriggerFor]="userMenu">
          <mat-icon>account_circle</mat-icon>
        </button>
        
        <mat-menu #userMenu="matMenu">
          <div class="user-menu-header">
            <mat-icon>person</mat-icon>
            <span>{{ user()?.username }}</span>
          </div>
          <mat-divider></mat-divider>
          <button mat-menu-item routerLink="/capabilities">
            <mat-icon>security</mat-icon>
            <span>Mes capacités</span>
          </button>
          <button mat-menu-item (click)="logout()">
            <mat-icon>logout</mat-icon>
            <span>Déconnexion</span>
          </button>
        </mat-menu>
      </div>
    </mat-toolbar>
  `,
  styles: [`
    .navbar {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 1000;
      display: flex;
      justify-content: space-between;
      padding: 0 16px;
    }
    
    .navbar-brand {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 20px;
      font-weight: 500;
    }
    
    .navbar-nav {
      display: flex;
      gap: 4px;
    }
    
    .navbar-nav a {
      color: rgba(255, 255, 255, 0.9);
    }
    
    .navbar-nav a.active {
      background: rgba(255, 255, 255, 0.15);
    }
    
    .navbar-nav a mat-icon {
      margin-right: 4px;
    }
    
    .chat-btn {
      background: rgba(255, 193, 7, 0.2) !important;
    }
    
    .navbar-user {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .user-menu-header {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      font-weight: 500;
    }
  `]
})
export class NavbarComponent {
  private authService = inject(AuthService);
  
  user = this.authService.currentUser;
  
  roleIcon = computed(() => {
    switch (this.user()?.role) {
      case 'ADMIN': return 'admin_panel_settings';
      case 'MANAGER': return 'manage_accounts';
      default: return 'support_agent';
    }
  });
  
  roleTooltip = computed(() => {
    const caps = this.user()?.capabilities.length ?? 0;
    return `${caps} capacité(s) MCP disponible(s)`;
  });

  logout(): void {
    this.authService.logout();
  }
}
