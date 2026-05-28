package ec.org.cedia.smartinventory.mcp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class McpSessionRegistry {

    private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();

    public McpSession create(String protocolVersion) {
        McpSession session = new McpSession(UUID.randomUUID().toString(), protocolVersion);
        sessions.put(session.id(), session);
        return session;
    }

    public McpSession getRequired(String sessionId) {
        McpSession session = sessions.get(sessionId);
        if (session == null) {
            throw new McpProtocolException(McpErrorCode.INVALID_REQUEST, "Sesión MCP no encontrada");
        }
        return session;
    }

    public void delete(String sessionId) {
        sessions.remove(sessionId);
    }
}
