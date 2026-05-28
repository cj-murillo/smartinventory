package ec.org.cedia.smartinventory.mcp;

import java.util.List;
import java.util.Map;

public record McpToolResponse(List<Map<String, Object>> content,
                              Map<String, Object> structuredContent,
                              boolean isError) {
}
