import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { McpService } from '../../core/services/mcp.service';
import { MarkdownPipe } from '../../shared/pipes/markdown.pipe';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MarkdownPipe
  ],
  template: `
    <div class="customers-page">
      <header class="page-header">
        <h1><mat-icon>people</mat-icon> Clients</h1>
        <p>Consultez le résumé d'activité des clients via MCP</p>
      </header>
      
      <mat-card class="search-card">
        <mat-card-header>
          <mat-icon mat-card-avatar>summarize</mat-icon>
          <mat-card-title>Résumé d'activité client</mat-card-title>
          <mat-card-subtitle>Capacité MCP : summarizeCustomerActivity</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form (ngSubmit)="getCustomerSummary()" class="search-form">
            <mat-form-field appearance="outline" class="search-field">
              <mat-label>Code client</mat-label>
              <input matInput 
                     [(ngModel)]="customerCode" 
                     name="customerCode"
                     placeholder="Ex: CLI-001"
                     [disabled]="loading()">
              <mat-icon matSuffix>badge</mat-icon>
            </mat-form-field>
            <button mat-raised-button color="primary" 
                    type="submit" 
                    [disabled]="loading() || !customerCode">
              @if (loading()) {
                <mat-spinner diameter="20"></mat-spinner>
              } @else {
                <mat-icon>summarize</mat-icon>
                Résumer
              }
            </button>
          </form>
          
          <div class="sample-customers">
            <span>Exemples :</span>
            <button mat-stroked-button (click)="customerCode = 'CLI-001'; getCustomerSummary()">
              CLI-001 (TechCorp)
            </button>
            <button mat-stroked-button (click)="customerCode = 'CLI-002'; getCustomerSummary()">
              CLI-002 (PharmaMed)
            </button>
            <button mat-stroked-button (click)="customerCode = 'CLI-003'; getCustomerSummary()">
              CLI-003 (RetailMax)
            </button>
          </div>
        </mat-card-content>
      </mat-card>
      
      @if (result()) {
        <mat-card class="result-card" [class.error]="result()?.status !== 'SUCCESS'">
          <mat-card-header>
            <mat-icon mat-card-avatar [class]="result()?.status === 'SUCCESS' ? 'success' : 'error'">
              {{ result()?.status === 'SUCCESS' ? 'check_circle' : 'error' }}
            </mat-icon>
            <mat-card-title>Résumé du client</mat-card-title>
            <mat-card-subtitle>Statut : {{ result()?.status }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="result-content" [innerHTML]="result()?.content | markdown"></div>
          </mat-card-content>
        </mat-card>
      }
      
      <mat-card class="info-card">
        <mat-card-header>
          <mat-icon mat-card-avatar class="info-icon">info</mat-icon>
          <mat-card-title>À propos du résumé client</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>Le résumé MCP d'un client inclut :</p>
          <ul>
            <li><strong>Informations générales</strong> - Nom, segment, statut</li>
            <li><strong>Commandes récentes</strong> - Historique des commandes</li>
            <li><strong>Situation financière</strong> - Factures, paiements, impayés</li>
            <li><strong>Indicateurs</strong> - Alertes et recommandations</li>
          </ul>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .customers-page {
      max-width: 900px;
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
    
    mat-card {
      margin-bottom: 24px;
    }
    
    mat-icon[mat-card-avatar] {
      background: #f3e5f5;
      color: #7b1fa2;
      padding: 8px;
      border-radius: 50%;
    }
    
    mat-icon[mat-card-avatar].success {
      background: #e8f5e9;
      color: #388e3c;
    }
    
    mat-icon[mat-card-avatar].error {
      background: #ffebee;
      color: #d32f2f;
    }
    
    .info-icon {
      background: #e3f2fd !important;
      color: #1976d2 !important;
    }
    
    .search-form {
      display: flex;
      gap: 16px;
      align-items: flex-start;
    }
    
    .search-field {
      flex: 1;
    }
    
    .sample-customers {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-top: 16px;
      color: #666;
      flex-wrap: wrap;
    }
    
    .result-card.error {
      border-left: 4px solid #d32f2f;
    }
    
    .result-content {
      background: #fafafa;
      padding: 16px;
      border-radius: 4px;
      overflow-x: auto;
      font-size: 14px;
      line-height: 1.6;
    }
    
    .result-content ::ng-deep {
      p { margin: 0 0 8px; }
      p:last-child { margin-bottom: 0; }
      ul, ol { margin: 8px 0; padding-left: 20px; }
      li { margin: 4px 0; }
      strong { font-weight: 600; }
      code { 
        background: rgba(0,0,0,0.08); 
        padding: 2px 6px; 
        border-radius: 4px;
        font-family: 'Roboto Mono', monospace;
        font-size: 0.9em;
      }
      pre {
        background: #263238;
        color: #aabfc9;
        padding: 12px;
        border-radius: 8px;
        overflow-x: auto;
        margin: 8px 0;
      }
      pre code { background: none; padding: 0; }
      h1, h2, h3, h4 { margin: 12px 0 8px; font-weight: 500; }
    }
    
    .info-card ul {
      margin: 0;
      padding-left: 20px;
    }
    
    .info-card li {
      margin: 8px 0;
    }
  `]
})
export class CustomersComponent implements OnInit {
  private mcpService = inject(McpService);
  private snackBar = inject(MatSnackBar);
  private route = inject(ActivatedRoute);
  
  customerCode = '';
  loading = signal(false);
  result = signal<{ status: string; content: string } | null>(null);
  
  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['summary']) {
        this.customerCode = params['summary'];
        this.getCustomerSummary();
      }
    });
  }
  
  getCustomerSummary(): void {
    if (!this.customerCode) return;
    
    this.loading.set(true);
    this.result.set(null);
    
    this.mcpService.getCustomerSummary(this.customerCode).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.result.set(response);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(
          err.status === 403 ? 'Accès refusé' : 'Erreur de connexion',
          'Fermer',
          { duration: 5000 }
        );
      }
    });
  }
}
