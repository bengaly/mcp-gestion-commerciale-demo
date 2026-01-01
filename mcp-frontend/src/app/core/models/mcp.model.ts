export interface McpResponse {
  status: McpResponseStatus;
  content: string;
  correlationId?: string;
  requiresConfirmation: boolean;
}

export type McpResponseStatus = 
  | 'SUCCESS'
  | 'NOT_FOUND'
  | 'VALIDATION_FAILED'
  | 'REQUIRES_CONFIRMATION'
  | 'ERROR'
  | 'ACCESS_DENIED';

export interface ChatRequest {
  message: string;
  conversationId?: string;
}

export interface ChatResponse {
  response: string;
  correlationId: string;
  conversationId: string;
}
