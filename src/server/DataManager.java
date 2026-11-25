package server;

import shared.models.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  DataManager Class (Thread-Safe File I/O Manager)
 * ═══════════════════════════════════════════════════════════════════
 *  CRITICAL COMPONENT: Thread Synchronization + File Handling
 *
 *  This singleton class manages ALL data operations:
 *  1. Loading vehicles from vehicles.csv
 *  2. Loading reservations from reservations.txt
 *  3. Booking seats (with write-lock synchronization)
 *  4. Cancelling reservations (with write-lock synchronization)
 *  5. Querying vehicles and reservations (with read-lock)
 *
 *  ╔═══════════════════════════════════════════════════════════╗
 *  ║  THREAD SAFETY MECHANISM: ReentrantReadWriteLock         ║
 *  ║                                                           ║
 *  ║  • Read Lock:  Multiple threads can READ simultaneously   ║
 *  ║  • Write Lock: Only ONE thread can WRITE at a time        ║
 *  ║                                                           ║
 *  ║  This prevents race conditions where two clients might    ║
 *  ║  try to book the same seat at the same millisecond.       ║
 *  ║                                                           ║
 *  ║  Why ReentrantReadWriteLock over synchronized?            ║
 *  ║  → Better concurrency: reads don't block other reads      ║
 *  ║  → Only writes are exclusive (mutual exclusion)           ║
 *  ╚═══════════════════════════════════════════════════════════╝
 *
 *  Design Pattern: Singleton (only one DataManager instance)
 * ═══════════════════════════════════════════════════════════════════
 */
public class DataManager {

    // ── Singleton ───────────────────────────────────────────────
    private static DataManager instance;

    // ── Thread Safety: ReentrantReadWriteLock ────────────────────
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // ── In-Memory Data Stores ───────────────────────────────────
    private final Map<String, Vehicle> vehicles = new LinkedHashMap<>();
    private final List<Ticket> reservations = new ArrayList<>();

    // ── Ticket Counter (for generating unique IDs) ──────────────
    private int ticketCounter = 0;

    // ── File Paths ──────────────────────────────────────────────
    private static final String VEHICLES_FILE = "data/vehicles.csv";
    private static final String RESERVATIONS_FILE = "data/reservations.txt";

    // ── Logger Reference ────────────────────────────────────────
    private final Logger logger = Logger.getInstance();

    // ═════════════════════════════════════════════════════════════
    //  CONSTRUCTOR (Private - Singleton)
    // ═════════════════════════════════════════════════════════════

    private DataManager() {
        loadVehicles();
        loadReservations();
        populateBookedSeats();
    }

    /**
     * Returns the singleton DataManager instance.
     * Synchronized to prevent double-initialization in multithreaded env.
     *
     * @return the DataManager instance
     */
    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    // ═════════════════════════════════════════════════════════════
    //  FILE LOADING (Called once at startup)
    // ═════════════════════════════════════════════════════════════

    /**
     * Loads vehicle data from vehicles.csv into memory.
     *
     * CSV Format: VehicleID,Type,Route,TotalSeats,AvailableSeats,BasePrice
     *
     * Based on the 'Type' column, creates either a Train or Airplane
     * object — this is where the Factory-like creation happens.
     */
    private void loadVehicles() {
        File file = new File(VEHICLES_FILE);
        if (!file.exists()) {
            logger.warning("vehicles.csv not found. Starting with empty vehicle list.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                // Skip the header line
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length != 6) {
                    logger.warning("Skipping malformed vehicle line: " + line);
                    continue;
                }

                String id = parts[0].trim();
                String type = parts[1].trim();
                String route = parts[2].trim();
                int totalSeats = Integer.parseInt(parts[3].trim());
                int availableSeats = Integer.parseInt(parts[4].trim());
                double basePrice = Double.parseDouble(parts[5].trim());

                Vehicle vehicle;
                // Create the appropriate subclass based on type
                if ("Train".equalsIgnoreCase(type)) {
                    vehicle = new Train(id, route, totalSeats, availableSeats, basePrice);
                } else if ("Airplane".equalsIgnoreCase(type)) {
                    vehicle = new Airplane(id, route, totalSeats, availableSeats, basePrice);
                } else {
                    logger.warning("Unknown vehicle type '" + type + "', skipping.");
                    continue;
                }

                vehicles.put(id, vehicle);
            }

            logger.info("Loaded " + vehicles.size() + " vehicles from " + VEHICLES_FILE);

        } catch (IOException e) {
            logger.error("Failed to load vehicles", (Exception) e);
        } catch (NumberFormatException e) {
            logger.error("Invalid number format in vehicles.csv", (Exception) e);
        }
    }

    /**
     * Loads reservation data from reservations.txt into memory.
     *
     * Format: TicketID,PassengerName,VehicleID,SeatNumber,SeatClass,FinalPrice
     *
     * Also determines the next ticket counter value by parsing
     * existing ticket IDs.
     */
    private void loadReservations() {
        File file = new File(RESERVATIONS_FILE);
        if (!file.exists()) {
            logger.info("reservations.txt not found. Starting with empty reservations.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    Ticket ticket = Ticket.fromCsvLine(line);
                    reservations.add(ticket);

                    // Update ticket counter based on existing IDs
                    // Format: TKT-001, TKT-002, etc.
                    String numPart = ticket.getTicketId().replace("TKT-", "");
                    int num = Integer.parseInt(numPart);
                    if (num > ticketCounter) {
                        ticketCounter = num;
                    }
                } catch (Exception e) {
                    logger.warning("Skipping malformed reservation line: " + line);
                }
            }

            logger.info("Loaded " + reservations.size() + " reservations from " + RESERVATIONS_FILE);

        } catch (IOException e) {
            logger.error("Failed to load reservations", (Exception) e);
        }
    }

    /**
     * Populates each vehicle's bookedSeats set from the loaded reservations.
     * This ensures the in-memory state is consistent after loading both files.
     */
    private void populateBookedSeats() {
        for (Ticket ticket : reservations) {
            Vehicle vehicle = vehicles.get(ticket.getVehicleId());
            if (vehicle != null) {
                vehicle.getBookedSeats().add(ticket.getSeatNumber());
            }
        }
        logger.info("Populated booked seats for all vehicles from reservations.");
    }

    // ═════════════════════════════════════════════════════════════
    //  FILE SAVING (Thread-safe: called within write-lock)
    // ═════════════════════════════════════════════════════════════

    /**
     * Saves all vehicles to vehicles.csv.
     * MUST be called within a write-lock context.
     */
    private void saveVehicles() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(VEHICLES_FILE))) {
            // Write header
            bw.write("VehicleID,Type,Route,TotalSeats,AvailableSeats,BasePrice");
            bw.newLine();

            for (Vehicle v : vehicles.values()) {
                bw.write(String.format("%s,%s,%s,%d,%d,%.2f",
                        v.getVehicleId(), v.getType(), v.getRoute(),
                        v.getTotalSeats(), v.getAvailableSeats(), v.getBasePrice()));
                bw.newLine();
            }

            logger.info("Vehicles saved to " + VEHICLES_FILE);

        } catch (IOException e) {
            logger.error("Failed to save vehicles", (Exception) e);
        }
    }

    /**
     * Saves all reservations to reservations.txt.
     * MUST be called within a write-lock context.
     */
    private void saveReservations() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(RESERVATIONS_FILE))) {
            // Write header
            bw.write("TicketID,PassengerName,VehicleID,SeatNumber,SeatClass,FinalPrice");
            bw.newLine();

            for (Ticket t : reservations) {
                bw.write(t.toCsvLine());
                bw.newLine();
            }

            logger.info("Reservations saved to " + RESERVATIONS_FILE);

        } catch (IOException e) {
            logger.error("Failed to save reservations", (Exception) e);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  READ OPERATIONS (Thread-safe with Read Lock)
    // ═════════════════════════════════════════════════════════════

    /**
     * Returns a copy of all vehicles.
     * Uses READ LOCK — multiple threads can call this simultaneously
     * without blocking each other.
     *
     * @return list of all vehicles (deep copy via serialization-safe list)
     */
    public List<Vehicle> getVehicles() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(vehicles.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns reservations filtered by passenger name.
     * Uses READ LOCK for concurrent read access.
     *
     * @param passengerName the name to filter by (null or empty = all)
     * @return filtered list of tickets
     */
    public List<Ticket> getReservations(String passengerName) {
        lock.readLock().lock();
        try {
            if (passengerName == null || passengerName.trim().isEmpty()) {
                return new ArrayList<>(reservations);
            }
            return reservations.stream()
                    .filter(t -> t.getPassengerName().equalsIgnoreCase(passengerName.trim()))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  WRITE OPERATIONS (Thread-safe with Write Lock)
    // ═════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════╗
     * ║  CRITICAL SECTION: Seat Booking                          ║
     * ║                                                           ║
     * ║  This method uses a WRITE LOCK to ensure that only ONE    ║
     * ║  thread can execute this block at a time. This prevents   ║
     * ║  the classic "double booking" race condition:              ║
     * ║                                                           ║
     * ║  Thread A: checks seat 5 → available ✓                    ║
     * ║  Thread B: checks seat 5 → available ✓  (RACE!)          ║
     * ║  Thread A: books seat 5 → success                         ║
     * ║  Thread B: books seat 5 → DOUBLE BOOKING! ✗              ║
     * ║                                                           ║
     * ║  With the write lock, Thread B waits until Thread A       ║
     * ║  finishes, then sees seat 5 is already booked.            ║
     * ╚═══════════════════════════════════════════════════════════╝
     *
     * @param vehicleId     the vehicle to book on
     * @param passengerName the passenger's name
     * @param seatNumber    the seat number to book
     * @param seatClass     the seat class ("Economy", "Business", "First")
     * @return the created Ticket if successful, null otherwise
     */
    public Ticket bookSeat(String vehicleId, String passengerName,
                           int seatNumber, String seatClass) {
        lock.writeLock().lock();
        try {
            // 1. Find the vehicle
            Vehicle vehicle = vehicles.get(vehicleId);
            if (vehicle == null) {
                logger.warning("Booking failed: Vehicle '" + vehicleId + "' not found.");
                return null;
            }

            // 2. Attempt to book the seat (checks availability internally)
            //    This is the Reservable interface method being called
            if (!vehicle.bookSeat(seatNumber)) {
                logger.warning("Booking failed: Seat " + seatNumber
                        + " on " + vehicleId + " is not available.");
                return null;
            }

            // 3. Calculate the price using POLYMORPHISM
            //    If vehicle is a Train → Train.calculateTicketPrice()
            //    If vehicle is an Airplane → Airplane.calculateTicketPrice()
            double finalPrice = vehicle.calculateTicketPrice(seatClass);

            // 4. Generate a unique ticket ID
            ticketCounter++;
            String ticketId = String.format("TKT-%03d", ticketCounter);

            // 5. Create the ticket
            Ticket ticket = new Ticket(ticketId, passengerName, vehicleId,
                    seatNumber, seatClass, finalPrice);
            reservations.add(ticket);

            // 6. Persist changes to files
            saveVehicles();
            saveReservations();

            logger.success("BOOKED: " + ticket);
            return ticket;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * ╔═══════════════════════════════════════════════════════════╗
     * ║  CRITICAL SECTION: Reservation Cancellation               ║
     * ║                                                           ║
     * ║  Also uses WRITE LOCK to prevent concurrent modification  ║
     * ║  issues. Restores the seat availability in the vehicle    ║
     * ║  and removes the ticket from reservations.                ║
     * ╚═══════════════════════════════════════════════════════════╝
     *
     * @param ticketId the ticket ID to cancel
     * @return true if cancellation succeeded, false otherwise
     */
    public boolean cancelReservation(String ticketId) {
        lock.writeLock().lock();
        try {
            // 1. Find the ticket
            Ticket targetTicket = null;
            for (Ticket t : reservations) {
                if (t.getTicketId().equalsIgnoreCase(ticketId)) {
                    targetTicket = t;
                    break;
                }
            }

            if (targetTicket == null) {
                logger.warning("Cancellation failed: Ticket '" + ticketId + "' not found.");
                return false;
            }

            // 2. Find the associated vehicle and cancel the seat
            Vehicle vehicle = vehicles.get(targetTicket.getVehicleId());
            if (vehicle != null) {
                vehicle.cancelSeat(targetTicket.getSeatNumber());
            }

            // 3. Remove the ticket from reservations
            reservations.remove(targetTicket);

            // 4. Persist changes to files
            saveVehicles();
            saveReservations();

            logger.success("CANCELLED: " + targetTicket);
            return true;

        } finally {
            lock.writeLock().unlock();
        }
    }
}
