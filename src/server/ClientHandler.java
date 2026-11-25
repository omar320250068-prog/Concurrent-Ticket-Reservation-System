package server;

import shared.models.*;
import shared.protocol.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  ClientHandler Class (Runnable — One Thread Per Client)
 * ═══════════════════════════════════════════════════════════════════
 *  MULTITHREADING: Each connected client gets its own ClientHandler
 *  running on a separate thread from the ExecutorService thread pool.
 *
 *  Communication Flow:
 *  ┌────────┐   Request (Object)   ┌───────────────┐
 *  │ Client │ ──────────────────►  │ ClientHandler  │
 *  │  GUI   │                      │  (on thread)   │
 *  │        │ ◄──────────────────  │                │
 *  └────────┘   Response (Object)  └───────────────┘
 *                                         │
 *                                         ▼
 *                                  ┌──────────────┐
 *                                  │  DataManager  │
 *                                  │ (thread-safe) │
 *                                  └──────────────┘
 *
 *  The handler:
 *  1. Reads Request objects from the client via ObjectInputStream
 *  2. Processes the request using the DataManager
 *  3. Sends a Response object back via ObjectOutputStream
 *  4. Loops until the client disconnects
 * ═══════════════════════════════════════════════════════════════════
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final int clientId;
    private final DataManager dataManager;
    private final Logger logger;

    /**
     * Constructs a new ClientHandler for the given socket.
     *
     * @param clientSocket the client's TCP socket
     * @param clientId     a unique ID for logging purposes
     */
    public ClientHandler(Socket clientSocket, int clientId) {
        this.clientSocket = clientSocket;
        this.clientId = clientId;
        this.dataManager = DataManager.getInstance();
        this.logger = Logger.getInstance();
    }

    /**
     * Main execution loop for this client's thread.
     * Continuously reads requests and sends responses until
     * the client disconnects.
     */
    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().getHostAddress()
                + ":" + clientSocket.getPort();

        logger.info("Client #" + clientId + " connected from " + clientAddress);

        try (
            // Object streams for serialized communication
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            // Flush the output stream header (required for ObjectOutputStream)
            out.flush();

            boolean running = true;

            while (running) {
                try {
                    // Read the next request from the client
                    Request request = (Request) in.readObject();

                    logger.info("Client #" + clientId + " → " + request.getType());

                    // Process the request and generate a response
                    Response response = processRequest(request);

                    // Send the response back to the client
                    out.writeObject(response);
                    out.flush();

                    // Reset the stream to prevent caching of stale objects
                    out.reset();

                    // Check if client wants to disconnect
                    if (request.getType() == Request.RequestType.DISCONNECT) {
                        running = false;
                    }

                } catch (EOFException e) {
                    // Client closed the connection
                    logger.info("Client #" + clientId + " connection closed (EOF).");
                    running = false;
                } catch (ClassNotFoundException e) {
                    logger.error("Invalid object received from Client #" + clientId, (Exception) e);
                }
            }

        } catch (IOException e) {
            logger.error("I/O error with Client #" + clientId, (Exception) e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Failed to close socket for Client #" + clientId, (Exception) e);
            }
            logger.info("Client #" + clientId + " disconnected.");
        }
    }

    /**
     * Processes a single request and returns the appropriate response.
     *
     * This method acts as a dispatcher, routing each request type
     * to the corresponding DataManager method.
     *
     * @param request the incoming request
     * @return the response to send back
     */
    private Response processRequest(Request request) {
        switch (request.getType()) {

            case VIEW_VEHICLES:
                return handleViewVehicles();

            case BOOK_SEAT:
                return handleBookSeat(request);

            case CANCEL_RESERVATION:
                return handleCancelReservation(request);

            case VIEW_RESERVATIONS:
                return handleViewReservations(request);

            case DISCONNECT:
                return Response.success("Goodbye! Connection closed.");

            default:
                return Response.error("Unknown request type.");
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  Request Handlers
    // ═════════════════════════════════════════════════════════════

    /**
     * Handles VIEW_VEHICLES request.
     * Returns a list of all available vehicles.
     */
    private Response handleViewVehicles() {
        List<Vehicle> vehicleList = dataManager.getVehicles();
        // Wrap in ArrayList (which is Serializable) for transmission
        return Response.success(
                "Found " + vehicleList.size() + " vehicles.",
                new ArrayList<>(vehicleList)
        );
    }

    /**
     * Handles BOOK_SEAT request.
     * Validates input, then delegates to DataManager.bookSeat().
     */
    private Response handleBookSeat(Request request) {
        // Validate required fields
        if (request.getVehicleId() == null || request.getVehicleId().isEmpty()) {
            return Response.error("Vehicle ID is required.");
        }
        if (request.getPassengerName() == null || request.getPassengerName().isEmpty()) {
            return Response.error("Passenger name is required.");
        }
        if (request.getSeatNumber() <= 0) {
            return Response.error("Seat number must be a positive integer.");
        }
        if (request.getSeatClass() == null || request.getSeatClass().isEmpty()) {
            return Response.error("Seat class is required.");
        }

        // Attempt to book (thread-safe via DataManager's write lock)
        Ticket ticket = dataManager.bookSeat(
                request.getVehicleId(),
                request.getPassengerName(),
                request.getSeatNumber(),
                request.getSeatClass()
        );

        if (ticket != null) {
            return Response.success(
                    "Booking confirmed! Ticket ID: " + ticket.getTicketId(),
                    ticket
            );
        } else {
            return Response.error(
                    "Booking failed. The seat may be already booked or the vehicle was not found."
            );
        }
    }

    /**
     * Handles CANCEL_RESERVATION request.
     * Validates input, then delegates to DataManager.cancelReservation().
     */
    private Response handleCancelReservation(Request request) {
        if (request.getTicketId() == null || request.getTicketId().isEmpty()) {
            return Response.error("Ticket ID is required for cancellation.");
        }

        boolean cancelled = dataManager.cancelReservation(request.getTicketId());

        if (cancelled) {
            return Response.success("Reservation " + request.getTicketId()
                    + " has been cancelled. Seat restored.");
        } else {
            return Response.error("Cancellation failed. Ticket '"
                    + request.getTicketId() + "' not found.");
        }
    }

    /**
     * Handles VIEW_RESERVATIONS request.
     * Returns reservations filtered by passenger name.
     */
    private Response handleViewReservations(Request request) {
        List<Ticket> tickets = dataManager.getReservations(request.getPassengerName());
        return Response.success(
                "Found " + tickets.size() + " reservation(s).",
                new ArrayList<>(tickets)
        );
    }
}
