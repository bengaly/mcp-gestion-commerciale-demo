import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import { User, McpRole, CapabilitiesResponse, LoginCredentials } from '../models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = '/api/chat';
  
  private currentUserSignal = signal<User | null>(null);
  private credentialsSignal = signal<string | null>(null);
  
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);
  readonly userRole = computed(() => this.currentUserSignal()?.role ?? null);

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadStoredCredentials();
  }

  private loadStoredCredentials(): void {
    const stored = localStorage.getItem('mcp_credentials');
    if (stored) {
      this.credentialsSignal.set(stored);
      this.fetchCapabilities().subscribe();
    }
  }

  login(credentials: LoginCredentials): Observable<CapabilitiesResponse> {
    const basicAuth = btoa(`${credentials.username}:${credentials.password}`);
    this.credentialsSignal.set(basicAuth);
    
    return this.fetchCapabilities().pipe(
      tap(response => {
        localStorage.setItem('mcp_credentials', basicAuth);
        this.currentUserSignal.set({
          username: response.user,
          role: response.role,
          capabilities: response.capabilities
        });
      }),
      catchError(error => {
        this.credentialsSignal.set(null);
        throw error;
      })
    );
  }

  private fetchCapabilities(): Observable<CapabilitiesResponse> {
    return this.http.get<CapabilitiesResponse>(`${this.API_URL}/capabilities`).pipe(
      tap(response => {
        this.currentUserSignal.set({
          username: response.user,
          role: response.role,
          capabilities: response.capabilities
        });
      })
    );
  }

  logout(): void {
    localStorage.removeItem('mcp_credentials');
    this.credentialsSignal.set(null);
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }

  getAuthHeader(): string | null {
    return this.credentialsSignal();
  }

  hasCapability(capabilityName: string): boolean {
    const user = this.currentUserSignal();
    if (!user) return false;
    return user.capabilities.some(c => c.name === capabilityName);
  }

  hasRole(role: McpRole): boolean {
    const user = this.currentUserSignal();
    if (!user) return false;
    
    const roleHierarchy: Record<McpRole, number> = {
      'SUPPORT': 1,
      'MANAGER': 2,
      'ADMIN': 3
    };
    
    return roleHierarchy[user.role] >= roleHierarchy[role];
  }
}
