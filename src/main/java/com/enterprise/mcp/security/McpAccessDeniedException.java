package com.enterprise.mcp.security;

/**
 * Exception levée lorsqu'un accès MCP est refusé
 */
public class McpAccessDeniedException extends RuntimeException {
    
    public McpAccessDeniedException(String message) {
        super(message);
    }
    
    public McpAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
