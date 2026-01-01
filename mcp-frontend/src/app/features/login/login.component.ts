import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon class="logo-icon">business</mat-icon>
            MCP Enterprise
          </mat-card-title>
          <mat-card-subtitle>Gestion Commerciale avec IA</mat-card-subtitle>
        </mat-card-header>
        
        <mat-card-content>
          <form (ngSubmit)="onLogin()" #loginForm="ngForm">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Utilisateur</mat-label>
              <input matInput 
                     [(ngModel)]="username" 
                     name="username" 
                     required
                     [disabled]="loading()">
              <mat-icon matSuffix>person</mat-icon>
            </mat-form-field>
            
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Mot de passe</mat-label>
              <input matInput 
                     [type]="hidePassword() ? 'password' : 'text'"
                     [(ngModel)]="password" 
                     name="password" 
                     required
                     [disabled]="loading()">
              <button mat-icon-button matSuffix type="button"
                      (click)="hidePassword.set(!hidePassword())">
                <mat-icon>{{hidePassword() ? 'visibility_off' : 'visibility'}}</mat-icon>
              </button>
            </mat-form-field>
            
            @if (error()) {
              <div class="error-message">
                <mat-icon>error</mat-icon>
                {{ error() }}
              </div>
            }
            
            <button mat-raised-button color="primary" 
                    type="submit" 
                    class="full-width login-btn"
                    [disabled]="loading() || !loginForm.valid">
              @if (loading()) {
                <mat-spinner diameter="20"></mat-spinner>
              } @else {
                <mat-icon>login</mat-icon>
                Connexion
              }
            </button>
          </form>
        </mat-card-content>
        
        <mat-card-footer>
          <div class="demo-users">
            <p><strong>Utilisateurs de démonstration :</strong></p>
            <div class="user-chips">
              <button mat-stroked-button (click)="fillCredentials('support', 'support123')">
                <mat-icon>support_agent</mat-icon> Support
              </button>
              <button mat-stroked-button (click)="fillCredentials('manager', 'manager123')">
                <mat-icon>manage_accounts</mat-icon> Manager
              </button>
              <button mat-stroked-button (click)="fillCredentials('admin', 'admin123')">
                <mat-icon>admin_panel_settings</mat-icon> Admin
              </button>
            </div>
          </div>
        </mat-card-footer>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #1a237e 0%, #0d47a1 100%);
    }
    
    .login-card {
      width: 100%;
      max-width: 420px;
      padding: 24px;
    }
    
    mat-card-header {
      justify-content: center;
      margin-bottom: 24px;
    }
    
    mat-card-title {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 24px;
    }
    
    .logo-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: #1a237e;
    }
    
    .full-width {
      width: 100%;
    }
    
    .login-btn {
      margin-top: 16px;
      height: 48px;
      font-size: 16px;
    }
    
    .login-btn mat-icon {
      margin-right: 8px;
    }
    
    .error-message {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #f44336;
      margin: 8px 0;
      padding: 12px;
      background: #ffebee;
      border-radius: 4px;
    }
    
    mat-card-footer {
      padding: 16px;
      border-top: 1px solid #e0e0e0;
    }
    
    .demo-users {
      text-align: center;
    }
    
    .demo-users p {
      margin-bottom: 12px;
      color: #666;
    }
    
    .user-chips {
      display: flex;
      gap: 8px;
      justify-content: center;
      flex-wrap: wrap;
    }
    
    .user-chips button {
      font-size: 12px;
    }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  
  loading = signal(false);
  error = signal<string | null>(null);
  hidePassword = signal(true);

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  fillCredentials(username: string, password: string): void {
    this.username = username;
    this.password = password;
  }

  onLogin(): void {
    this.loading.set(true);
    this.error.set(null);
    
    this.authService.login({ username: this.username, password: this.password })
      .subscribe({
        next: () => {
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err.status === 401 
            ? 'Identifiants incorrects' 
            : 'Erreur de connexion au serveur');
        }
      });
  }
}
