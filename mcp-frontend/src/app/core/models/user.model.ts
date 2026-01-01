export interface User {
  username: string;
  role: McpRole;
  capabilities: McpCapability[];
}

export type McpRole = 'SUPPORT' | 'MANAGER' | 'ADMIN';

export interface McpCapability {
  name: string;
  description: string;
  requiresConfirmation: boolean;
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface CapabilitiesResponse {
  user: string;
  role: McpRole;
  capabilities: McpCapability[];
}
