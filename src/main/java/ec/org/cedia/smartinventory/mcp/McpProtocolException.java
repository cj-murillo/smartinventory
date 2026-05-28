package ec.org.cedia.smartinventory.mcp;

public class McpProtocolException extends RuntimeException {

    private final int code;
    private final Object data;

    public McpProtocolException(int code, String message) {
        this(code, message, null);
    }

    public McpProtocolException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }
}
