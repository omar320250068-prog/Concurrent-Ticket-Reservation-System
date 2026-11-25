package shared.models;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Reservable Interface
 * ═══════════════════════════════════════════════════════════════════
 *  OOP Concept: INTERFACE
 *  
 *  This interface defines the contract for any object that can have
 *  seats reserved. Both Train and Airplane (via Vehicle) implement
 *  this interface, demonstrating interface-based polymorphism.
 *  
 *  By programming to an interface, the server can treat any Vehicle
 *  uniformly when checking availability or booking seats, regardless
 *  of whether it's a Train or Airplane.
 * ═══════════════════════════════════════════════════════════════════
 */
public interface Reservable {

    /**
     * Checks whether a specific seat number is available for booking.
     *
     * @param seatNumber the seat number to check (1-indexed)
     * @return true if the seat is available, false otherwise
     */
    boolean checkAvailability(int seatNumber);

    /**
     * Books (reserves) a specific seat on this vehicle.
     *
     * @param seatNumber the seat number to book (1-indexed)
     * @return true if the seat was successfully booked, false if unavailable
     */
    boolean bookSeat(int seatNumber);

    /**
     * Cancels a previously booked seat, restoring its availability.
     *
     * @param seatNumber the seat number to cancel
     * @return true if the seat was successfully cancelled, false otherwise
     */
    boolean cancelSeat(int seatNumber);
}
