package shared.models;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Vehicle Abstract Class
 * ═══════════════════════════════════════════════════════════════════
 *  OOP Concepts: ABSTRACTION, ENCAPSULATION, INHERITANCE (base class)
 *
 *  This abstract class serves as the base for all vehicle types
 *  (Train, Airplane). It implements:
 *
 *  - ENCAPSULATION: All attributes are private with getters/setters.
 *  - ABSTRACTION: calculateTicketPrice() is abstract, forcing each
 *    subclass to provide its own pricing logic (POLYMORPHISM).
 *  - INTERFACE IMPLEMENTATION: Implements Reservable for seat mgmt.
 *  - SERIALIZABLE: Allows objects to be sent over sockets via
 *    ObjectOutputStream / ObjectInputStream.
 *
 *  The bookedSeats set tracks which specific seats are reserved,
 *  enabling precise seat-level availability checking.
 * ═══════════════════════════════════════════════════════════════════
 */
public abstract class Vehicle implements Serializable, Reservable {

    private static final long serialVersionUID = 1L;

    // ── Private Attributes (Encapsulation) ──────────────────────
    private String vehicleId;       // Unique identifier (e.g., "TRN-001")
    private String type;            // "Train" or "Airplane"
    private String route;           // Route description (e.g., "Cairo-Alexandria")
    private int totalSeats;         // Total number of seats on the vehicle
    private int availableSeats;     // Current number of available (unbooked) seats
    private double basePrice;       // Base ticket price in the default currency
    private Set<Integer> bookedSeats; // Set of seat numbers that are currently booked

    // ── Constructors ────────────────────────────────────────────

    /**
     * Default constructor - initializes bookedSeats to empty set.
     */
    public Vehicle() {
        this.bookedSeats = new HashSet<>();
    }

    /**
     * Parameterized constructor for creating a vehicle with all attributes.
     *
     * @param vehicleId      unique vehicle identifier
     * @param type           vehicle type ("Train" or "Airplane")
     * @param route          travel route
     * @param totalSeats     total seat capacity
     * @param availableSeats currently available seats
     * @param basePrice      base ticket price
     */
    public Vehicle(String vehicleId, String type, String route,
                   int totalSeats, int availableSeats, double basePrice) {
        this.vehicleId = vehicleId;
        this.type = type;
        this.route = route;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.basePrice = basePrice;
        this.bookedSeats = new HashSet<>();
    }

    // ── Abstract Method (Polymorphism) ──────────────────────────

    /**
     * Calculates the final ticket price based on the seat class.
     * Each subclass (Train, Airplane) provides its own pricing logic,
     * demonstrating POLYMORPHISM (same method signature, different behavior).
     *
     * @param seatClass the class of seat ("Economy", "Business", or "First")
     * @return the calculated final price
     */
    public abstract double calculateTicketPrice(String seatClass);

    // ── Reservable Interface Implementation ─────────────────────

    /**
     * Checks if a specific seat is available for booking.
     * A seat is available if:
     *   1. The seat number is within valid range (1 to totalSeats)
     *   2. The seat has not been previously booked
     *
     * @param seatNumber the seat to check
     * @return true if available
     */
    @Override
    public boolean checkAvailability(int seatNumber) {
        return seatNumber >= 1
            && seatNumber <= totalSeats
            && !bookedSeats.contains(seatNumber);
    }

    /**
     * Books a seat if it is available. Updates the bookedSeats set
     * and decrements the available seat count.
     *
     * NOTE: This method is NOT inherently thread-safe. The DataManager
     * wraps calls to this method inside a write-lock to prevent race
     * conditions between concurrent clients.
     *
     * @param seatNumber the seat to book
     * @return true if booking succeeded
     */
    @Override
    public boolean bookSeat(int seatNumber) {
        if (checkAvailability(seatNumber)) {
            bookedSeats.add(seatNumber);
            availableSeats--;
            return true;
        }
        return false;
    }

    /**
     * Cancels a previously booked seat, restoring its availability.
     *
     * @param seatNumber the seat to cancel
     * @return true if cancellation succeeded
     */
    @Override
    public boolean cancelSeat(int seatNumber) {
        if (bookedSeats.contains(seatNumber)) {
            bookedSeats.remove(seatNumber);
            availableSeats++;
            return true;
        }
        return false;
    }

    // ── Getters and Setters (Encapsulation) ─────────────────────

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public Set<Integer> getBookedSeats() {
        return bookedSeats;
    }

    public void setBookedSeats(Set<Integer> bookedSeats) {
        this.bookedSeats = bookedSeats;
    }

    // ── toString ────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("[%s] %s | Route: %s | Seats: %d/%d | Base Price: $%.2f",
                vehicleId, type, route, availableSeats, totalSeats, basePrice);
    }
}
