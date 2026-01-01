import { Component, inject, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { McpService } from '../../core/services/mcp.service';
import { MarkdownPipe } from '../../shared/pipes/markdown.pipe';
import { AuthService } from '../../core/services/auth.service';

interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
}

@Component({
  selector: 'app-chat',
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
    MatChipsModule,
    MarkdownPipe
  ],
  template: `
    <div class="chat-page">
      <header class="page-header">
        <h1><mat-icon>smart_toy</mat-icon> Assistant IA MCP</h1>
        <p>Interagissez avec le système via langage naturel</p>
      </header>
      
      <div class="chat-container">
        <mat-card class="chat-card">
          <mat-card-header>
            <mat-icon mat-card-avatar>chat</mat-icon>
            <mat-card-title>Conversation</mat-card-title>
            <mat-card-subtitle>
              Rôle : {{ user()?.role }} | 
              {{ user()?.capabilities?.length }} capacité(s) disponible(s)
            </mat-card-subtitle>
          </mat-card-header>
          
          <mat-card-content>
            <div class="messages-container" #messagesContainer>
              @if (messages().length === 0) {
                <div class="welcome-message">
                  <mat-icon>waving_hand</mat-icon>
                  <h3>Bienvenue !</h3>
                  <p>Je suis votre assistant IA MCP. Je peux vous aider à :</p>
                  <ul>
                    <li>Rechercher des commandes</li>
                    <li>Analyser des factures</li>
                    <li>Résumer l'activité d'un client</li>
                    @if (authService.hasCapability('createOrder')) {
                      <li>Créer des commandes (avec confirmation)</li>
                    }
                  </ul>
                  <p class="hint">Essayez : "Montre-moi la commande CMD-20240115-TC001"</p>
                </div>
              }
              
              @for (msg of messages(); track $index) {
                <div class="message" [class]="msg.role">
                  <div class="message-avatar">
                    <mat-icon>{{ msg.role === 'user' ? 'person' : 'smart_toy' }}</mat-icon>
                  </div>
                  <div class="message-content">
                    <div class="message-header">
                      <span class="message-role">{{ msg.role === 'user' ? 'Vous' : 'Assistant' }}</span>
                      <span class="message-time">{{ msg.timestamp | date:'HH:mm' }}</span>
                    </div>
                    <div class="message-text" [innerHTML]="msg.content | markdown"></div>
                  </div>
                </div>
              }
              
              @if (loading()) {
                <div class="message assistant loading">
                  <div class="message-avatar">
                    <mat-icon>smart_toy</mat-icon>
                  </div>
                  <div class="message-content">
                    <mat-spinner diameter="24"></mat-spinner>
                    <span>Réflexion en cours...</span>
                  </div>
                </div>
              }
            </div>
            
            <form (ngSubmit)="sendMessage()" class="input-form">
              <mat-form-field appearance="outline" class="message-input">
                <mat-label>Votre message</mat-label>
                <textarea matInput 
                          [(ngModel)]="userMessage" 
                          name="message"
                          placeholder="Ex: Analyse la facture FAC-2024-000123"
                          [disabled]="loading()"
                          rows="2"
                          (keydown.enter)="onEnterKey($event)"></textarea>
              </mat-form-field>
              <button mat-fab color="primary" 
                      type="submit" 
                      [disabled]="loading() || !userMessage.trim()">
                <mat-icon>send</mat-icon>
              </button>
            </form>
          </mat-card-content>
        </mat-card>
        
        <mat-card class="suggestions-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="suggestions-icon">lightbulb</mat-icon>
            <mat-card-title>Suggestions</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="suggestions">
              <button mat-stroked-button (click)="useSuggestion('Recherche la commande CMD-20240115-TC001')">
                <mat-icon>shopping_cart</mat-icon>
                Rechercher une commande
              </button>
              <button mat-stroked-button (click)="useSuggestion('Analyse la facture FAC-2024-000123')">
                <mat-icon>receipt</mat-icon>
                Analyser une facture
              </button>
              <button mat-stroked-button (click)="useSuggestion('Donne-moi un résumé du client CLI-001')">
                <mat-icon>people</mat-icon>
                Résumé client
              </button>
              @if (authService.hasCapability('createOrder')) {
                <button mat-stroked-button color="accent" 
                        (click)="useSuggestion('Crée une commande pour CLI-001 avec 2 produits P-LAPTOP-001 et 1 produit P-MOUSE-001')">
                  <mat-icon>add_shopping_cart</mat-icon>
                  Créer une commande
                </button>
              }
            </div>
            
            <div class="capabilities-info">
              <h4>Vos capacités MCP :</h4>
              <mat-chip-set>
                @for (cap of user()?.capabilities; track cap.name) {
                  <mat-chip [highlighted]="cap.requiresConfirmation">
                    <mat-icon matChipAvatar>{{ cap.requiresConfirmation ? 'edit' : 'visibility' }}</mat-icon>
                    {{ cap.name }}
                  </mat-chip>
                }
              </mat-chip-set>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .chat-page {
      max-width: 1200px;
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
    
    .chat-container {
      display: grid;
      grid-template-columns: 1fr 320px;
      gap: 24px;
    }
    
    @media (max-width: 900px) {
      .chat-container {
        grid-template-columns: 1fr;
      }
    }
    
    .chat-card {
      height: calc(100vh - 200px);
      display: flex;
      flex-direction: column;
    }
    
    .chat-card mat-card-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    
    mat-icon[mat-card-avatar] {
      background: #fff3e0;
      color: #f57c00;
      padding: 8px;
      border-radius: 50%;
    }
    
    .suggestions-icon {
      background: #e8f5e9 !important;
      color: #388e3c !important;
    }
    
    .messages-container {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      background: #fafafa;
      border-radius: 8px;
      margin-bottom: 16px;
    }
    
    .welcome-message {
      text-align: center;
      padding: 32px;
      color: #666;
    }
    
    .welcome-message mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #f57c00;
    }
    
    .welcome-message h3 {
      margin: 16px 0 8px;
    }
    
    .welcome-message ul {
      text-align: left;
      display: inline-block;
      margin: 16px 0;
    }
    
    .welcome-message .hint {
      background: #e3f2fd;
      padding: 12px;
      border-radius: 8px;
      font-style: italic;
    }
    
    .message {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;
    }
    
    .message.user {
      flex-direction: row-reverse;
    }
    
    .message-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    
    .message.user .message-avatar {
      background: #e3f2fd;
      color: #1976d2;
    }
    
    .message.assistant .message-avatar {
      background: #fff3e0;
      color: #f57c00;
    }
    
    .message-content {
      max-width: 70%;
      background: white;
      padding: 12px 16px;
      border-radius: 12px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    
    .message.user .message-content {
      background: #1976d2;
      color: white;
    }
    
    .message-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;
      font-size: 12px;
    }
    
    .message-role {
      font-weight: 500;
    }
    
    .message.user .message-role,
    .message.user .message-time {
      color: rgba(255,255,255,0.8);
    }
    
    .message-time {
      color: #999;
    }
    
    .message-text {
      line-height: 1.6;
    }
    
    .message-text ::ng-deep {
      p { margin: 0 0 8px; }
      p:last-child { margin-bottom: 0; }
      ul, ol { margin: 8px 0; padding-left: 20px; }
      li { margin: 4px 0; }
      strong { font-weight: 600; }
      code { 
        background: rgba(0,0,0,0.05); 
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
      pre code {
        background: none;
        padding: 0;
      }
      h1, h2, h3, h4 {
        margin: 12px 0 8px;
        font-weight: 500;
      }
      h1 { font-size: 1.4em; }
      h2 { font-size: 1.2em; }
      h3 { font-size: 1.1em; }
    }
    
    .message.user .message-text ::ng-deep {
      code { background: rgba(255,255,255,0.2); }
    }
    
    .message.loading .message-content {
      display: flex;
      align-items: center;
      gap: 12px;
      color: #666;
    }
    
    .input-form {
      display: flex;
      gap: 12px;
      align-items: flex-end;
    }
    
    .message-input {
      flex: 1;
    }
    
    .suggestions-card {
      height: fit-content;
    }
    
    .suggestions {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    
    .suggestions button {
      justify-content: flex-start;
      text-align: left;
    }
    
    .suggestions button mat-icon {
      margin-right: 8px;
    }
    
    .capabilities-info {
      margin-top: 24px;
      padding-top: 16px;
      border-top: 1px solid #e0e0e0;
    }
    
    .capabilities-info h4 {
      margin: 0 0 12px;
      color: #666;
      font-size: 14px;
    }
  `]
})
export class ChatComponent implements AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;
  
  private mcpService = inject(McpService);
  private snackBar = inject(MatSnackBar);
  authService = inject(AuthService);
  
  user = this.authService.currentUser;
  userMessage = '';
  conversationId = '';
  loading = signal(false);
  messages = signal<ChatMessage[]>([]);
  
  private shouldScroll = false;
  
  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }
  
  private scrollToBottom(): void {
    try {
      this.messagesContainer.nativeElement.scrollTop = 
        this.messagesContainer.nativeElement.scrollHeight;
    } catch (err) {}
  }
  
  onEnterKey(event: Event): void {
    const keyEvent = event as KeyboardEvent;
    if (!keyEvent.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
  
  useSuggestion(text: string): void {
    this.userMessage = text;
    this.sendMessage();
  }
  
  sendMessage(): void {
    const message = this.userMessage.trim();
    if (!message) return;
    
    // Add user message
    this.messages.update(msgs => [...msgs, {
      role: 'user',
      content: message,
      timestamp: new Date()
    }]);
    
    this.userMessage = '';
    this.loading.set(true);
    this.shouldScroll = true;
    
    this.mcpService.sendChatMessage({
      message,
      conversationId: this.conversationId || undefined
    }).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.conversationId = response.conversationId;
        
        this.messages.update(msgs => [...msgs, {
          role: 'assistant',
          content: response.response,
          timestamp: new Date()
        }]);
        
        this.shouldScroll = true;
      },
      error: (err) => {
        this.loading.set(false);
        
        let errorMessage = 'Erreur de connexion au serveur';
        if (err.status === 403) {
          errorMessage = 'Accès refusé - capacité non autorisée';
        } else if (err.error?.message) {
          errorMessage = err.error.message;
        }
        
        this.messages.update(msgs => [...msgs, {
          role: 'assistant',
          content: `❌ ${errorMessage}`,
          timestamp: new Date()
        }]);
        
        this.snackBar.open(errorMessage, 'Fermer', { duration: 5000 });
        this.shouldScroll = true;
      }
    });
  }
}
