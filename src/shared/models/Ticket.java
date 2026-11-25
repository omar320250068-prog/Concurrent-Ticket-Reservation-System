package shared.models;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Ticket Class (Reservation Record)
 * ═══════════════════════════════════════════════════════════════════
 *  OOP Concept: ENCAPSULATION
 *
 *  Represents a single reservation/ticket. All attributes are
 *  private with proper getters and setters.
 *
 *  This class is Serializable so it can be:
 *  1. Sent from Server to Client via ObjectOutputStream
 *  2. Stored/loaded from reservations.txt (as CSV-formatted text)
 *
 *  Each Ticket maps to one line in reservations.txt:
 *  TicketID,PassengerName,VehicleID,SeatNumber,SeatClass,FinalPrice
 * ═══════════════════════════════════════════════════════════════════
 */
public class Ticket implements Serializable {

    private static final long serialVersionUID = 4L;

    // ── Private Attributes (Encapsulation) ──────────────────────
    private String ticketId;        // Unique ticket ID (e.g., "TKT-001")
    private String passengerName;   // Name of the passenger
    private String vehicleId;       // Associated vehicle ID
    private int seatNumber;         // Reserved seat number
    private String seatClass;       // "Economy", "Business", or "First"
    private double finalPrice;      // Final calculated price after taxes

    // ── Constructors ────────────────────────────────────────────

    /**
     * Default constructor.
     */
    public Ticket() {
    }

    /**
     * Parameterized constructor.
     *
     * @param ticketId      unique ticket identifier
     * @param passengerName passenger's full name
     * @param vehicleId     ID of the vehicle
     * @param seatNumber    booked seat number
     * @param seatClass     seat class type
     * @param finalPrice    final ticket price
     */
    public Ticket(String ticketId, String passengerName, String vehicleId,
                  int seatNumber, String seatClass, double finalPrice) {
        this.ticketId = ticketId;
        this.passengerName = passengerName;
        this.vehicleId = vehicleId;
        this.seatNumber = seatNumber;
        this.seatClass = seatClass;
        this.finalPrice = finalPrice;
    }

    // ── Getters and Setters (Encapsulation) ─────────────────────

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
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

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    // ── Utility Methods ─────────────────────────────────────────

    /**
     * Converts the ticket to a CSV-formatted string for file storage.
     *
     * @return CSV line representing this ticket
     */
    public String toCsvLine() {
        return String.format("%s,%s,%s,%d,%s,%.2f",
                ticketId, passengerName, vehicleId,
                seatNumber, seatClass, finalPrice);
    }

    /**
     * Creates a Ticket object from a CSV-formatted line.
     *
     * @param csvLine the CSV line from reservations.txt
     * @return a new Ticket object
     */
    public static Ticket fromCsvLine(String csvLine) {
        String[] parts = csvLine.split(",");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid ticket CSV line: " + csvLine);
        }
        return new Ticket(
                parts[0].trim(),                           // ticketId
                parts[1].trim(),                           // passengerName
                parts[2].trim(),                           // vehicleId
                Integer.parseInt(parts[3].trim()),         // seatNumber
                parts[4].trim(),                           // seatClass
                Double.parseDouble(parts[5].trim())        // finalPrice
        );
    }

    @Override
    public String toString() {
        return String.format("🎫 Ticket [%s] | Passenger: %s | Vehicle: %s | Seat: %d (%s) | Price: $%.2f",
                ticketId, passengerName, vehicleId, seatNumber, seatClass, finalPrice);
    }
}
