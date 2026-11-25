package shared.protocol;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Request Class (Client → Server Communication)
 * ═══════════════════════════════════════════════════════════════════
 *  This class represents a request sent from the Client to the
 *  Server via Object Serialization (ObjectOutputStream).
 *
 *  It uses a RequestType enum to define what operation the client
 *  wants to perform. Depending on the type, different fields are
 *  populated:
 *
 *  ┌────────────────────┬─────────────────────────────────────────┐
 *  │    RequestType     │          Required Fields                │
 *  ├────────────────────┼─────────────────────────────────────────┤
 *  │ VIEW_VEHICLES      │ (none)                                  │
 *  │ BOOK_SEAT          │ vehicleId, passengerName, seatNumber,  │
 *  │                    │ seatClass                               │
 *  │ CANCEL_RESERVATION │ ticketId                                │
 *  │ VIEW_RESERVATIONS  │ passengerName (optional filter)         │
 *  │ DISCONNECT         │ (none)                                  │
 *  └────────────────────┴─────────────────────────────────────────┘
 * ═══════════════════════════════════════════════════════════════════
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 5L;

    /**
     * Enum defining all possible request types from the client.
     */
    public enum RequestType {
        VIEW_VEHICLES,          // Request list of all vehicles
        BOOK_SEAT,              // Book a specific seat on a vehicle
        CANCEL_RESERVATION,     // Cancel an existing reservation by ticket ID
        VIEW_RESERVATIONS,      // View reservations (optionally filtered by name)
        DISCONNECT              // Client is disconnecting gracefully
    }

    // ── Fields ──────────────────────────────────────────────────
    private RequestType type;
    private String vehicleId;
    private String passengerName;
    private int seatNumber;
    private String seatClass;
    private String ticketId;

    // ── Constructors ────────────────────────────────────────────

    /**
     * Default constructor.
     */
    public Request() {
    }

    /**
     * Constructor for simple requests (VIEW_VEHICLES, DISCONNECT).
     *
     * @param type the request type
     */
    public Request(RequestType type) {
        this.type = type;
    }

    // ── Static Factory Methods (for convenience) ────────────────

    /**
     * Creates a VIEW_VEHICLES request.
     */
    public static Request viewVehicles() {
        return new Request(RequestType.VIEW_VEHICLES);
    }

    /**
     * Creates a BOOK_SEAT request with all necessary booking info.
     */
    public static Request bookSeat(String vehicleId, String passengerName,
                                   int seatNumber, String seatClass) {
        Request req = new Request(RequestType.BOOK_SEAT);
        req.setVehicleId(vehicleId);
        req.setPassengerName(passengerName);
        req.setSeatNumber(seatNumber);
        req.setSeatClass(seatClass);
        return req;
    }

    /**
     * Creates a CANCEL_RESERVATION request.
     */
    public static Request cancelReservation(String ticketId) {
        Request req = new Request(RequestType.CANCEL_RESERVATION);
        req.setTicketId(ticketId);
        return req;
    }

    /**
     * Creates a VIEW_RESERVATIONS request, optionally filtered by name.
     */
    public static Request viewReservations(String passengerName) {
        Request req = new Request(RequestType.VIEW_RESERVATIONS);
        req.setPassengerName(passengerName);
        return req;
    }

    /**
     * Creates a DISCONNECT request.
     */
    public static Request disconnect() {
        return new Request(RequestType.DISCONNECT);
    }

    // ── Getters and Setters ─────────────────────────────────────

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(String seatClass) {
        this.seatClass = seatClass;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    @Override
    public String toString() {
        return "Request{type=" + type + "}";
    }
}
