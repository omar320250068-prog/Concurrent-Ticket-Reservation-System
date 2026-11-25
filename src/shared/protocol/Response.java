package shared.protocol;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Response Class (Server → Client Communication)
 * ═══════════════════════════════════════════════════════════════════
 *  This class represents a response sent from the Server to the
 *  Client via Object Serialization (ObjectOutputStream).
 *
 *  Fields:
 *  - success: whether the requested operation succeeded
 *  - message: human-readable status/error message
 *  - data:    the payload (List<Vehicle>, Ticket, List<Ticket>, etc.)
 *
 *  The 'data' field is typed as Object to allow flexibility.
 *  The client casts it to the appropriate type based on the
 *  original request type.
 *
 *  IMPORTANT: All objects placed in 'data' MUST be Serializable.
 *  (ArrayList, Vehicle, Ticket — all implement Serializable)
 * ═══════════════════════════════════════════════════════════════════
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 6L;

    // ── Fields ──────────────────────────────────────────────────
    private boolean success;    // Whether the operation succeeded
    private String message;     // Status message or error description
    private Object data;        // Response payload (must be Serializable)

    // ── Constructors ────────────────────────────────────────────

    /**
     * Default constructor.
     */
    public Response() {
    }

    /**
     * Constructor with all fields.
     *
     * @param success whether the operation succeeded
     * @param message descriptive message
     * @param data    response payload
     */
    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // ── Static Factory Methods ──────────────────────────────────

    /**
     * Creates a successful response with data.
     *
     * @param message success message
     * @param data    the response payload
     * @return a success Response
     */
    public static Response success(String message, Object data) {
        return new Response(true, message, data);
    }

    /**
     * Creates a successful response without data.
     *
     * @param message success message
     * @return a success Response
     */
    public static Response success(String message) {
        return new Response(true, message, null);
    }

    /**
     * Creates an error response.
     *
     * @param message error description
     * @return an error Response
     */
    public static Response error(String message) {
        return new Response(false, message, null);
    }

    // ── Getters and Setters ─────────────────────────────────────

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Response{success=" + success + ", message='" + message + "'}";
    }
}
