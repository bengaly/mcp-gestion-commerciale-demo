import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { McpResponse, ChatRequest, ChatResponse } from '../models';

@Injectable({
  providedIn: 'root'
})
export class McpService {
  private readonly API_URL = '/api/chat';

  constructor(private http: HttpClient) {}

  findOrder(orderNumber: string): Observable<McpResponse> {
    return this.http.get<McpResponse>(`${this.API_URL}/test/find-order/${orderNumber}`);
  }

  analyzeInvoice(invoiceNumber: string): Observable<McpResponse> {
    return this.http.get<McpResponse>(`${this.API_URL}/test/analyze-invoice/${invoiceNumber}`);
  }

  getCustomerSummary(customerCode: string): Observable<McpResponse> {
    return this.http.get<McpResponse>(`${this.API_URL}/test/customer-summary/${customerCode}`);
  }

  sendChatMessage(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.API_URL}/llm/message`, request);
  }
}
