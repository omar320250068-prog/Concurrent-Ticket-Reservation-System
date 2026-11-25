package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  ServerMain — Entry Point for the Ticket Reservation Server
 * ═══════════════════════════════════════════════════════════════════
 *  ARCHITECTURE: Multi-threaded TCP Server
 *
 *  This class starts the server and listens for incoming client
 *  connections on a specified port. It uses an ExecutorService
 *  (thread pool) to handle multiple clients concurrently.
 *
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │                    SERVER ARCHITECTURE                       │
 *  │                                                             │
 *  │  ┌──────────────┐                                           │
 *  │  │ ServerSocket  │ ← Listens on port 5000                   │
 *  │  │  (accept())   │                                           │
 *  │  └──────┬───────┘                                           │
 *  │         │ new connection                                     │
 *  │         ▼                                                    │
 *  │  ┌──────────────────┐                                       │
 *  │  │  ExecutorService  │ ← Thread Pool (10 threads max)       │
 *  │  │  (Thread Pool)    │                                       │
 *  │  └──────┬───────────┘                                       │
 *  │         │ submit(ClientHandler)                               │
 *  │         ▼                                                    │
 *  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │
 *  │  │ Handler #1   │ │ Handler #2   │ │ Handler #3   │  ...   │
 *  │  │ (Thread 1)   │ │ (Thread 2)   │ │ (Thread 3)   │        │
 *  │  └──────────────┘ └──────────────┘ └──────────────┘        │
 *  │         │                │                │                  │
 *  │         └────────────────┴────────────────┘                  │
 *  │                          │                                   │
 *  │                          ▼                                   │
 *  │                 ┌──────────────┐                             │
 *  │                 │  DataManager  │ ← Shared, Thread-Safe     │
 *  │                 │  (Singleton)  │                             │
 *  │                 └──────────────┘                             │
 *  │                          │                                   │
 *  │              ┌───────────┴───────────┐                      │
 *  │              ▼                       ▼                       │
 *  │     ┌──────────────┐      ┌──────────────────┐             │
 *  │     │ vehicles.csv │      │ reservations.txt  │             │
 *  │     └──────────────┘      └──────────────────┘             │
 *  └─────────────────────────────────────────────────────────────┘
 *
 *  Why ExecutorService (Thread Pool)?
 *  → Controls the number of active threads (prevents resource exhaustion)
 *  → Reuses threads instead of creating new ones for each client
 *  → Graceful shutdown with shutdownNow()
 * ═══════════════════════════════════════════════════════════════════
 */
public class ServerMain {

    // Server configuration
    private static final int PORT = 5000;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        Logger logger = Logger.getInstance();

        logger.logSeparator();
        logger.info("╔═══════════════════════════════════════════════════╗");
        logger.info("║   CONCURRENT TICKET RESERVATION SYSTEM - SERVER  ║");
        logger.info("║   Train & Airplane Booking Platform              ║");
        logger.info("╚═══════════════════════════════════════════════════╝");
        logger.logSeparator();

        // Initialize the DataManager (loads data from files)
        DataManager dataManager = DataManager.getInstance();
        logger.info("DataManager initialized. Data loaded from files.");

        // Create the thread pool for handling concurrent clients
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        logger.info("Thread pool created with " + THREAD_POOL_SIZE + " threads.");

        // Client counter for logging
        int clientCounter = 0;

        // Start listening for connections
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started. Listening on port " + PORT + "...");
            logger.info("Waiting for client connections...");
            logger.logSeparator();

            // Main accept loop — runs indefinitely
            while (true) {
                try {
                    // accept() blocks until a new client connects
                    Socket clientSocket = serverSocket.accept();
                    clientCounter++;

                    logger.info("New connection accepted! Assigning Client #" + clientCounter);

                    // Create a handler for this client and submit to thread pool
                    ClientHandler handler = new ClientHandler(clientSocket, clientCounter);
                    threadPool.submit(handler);

                } catch (IOException e) {
                    logger.error("Failed to accept client connection", (Exception) e);
                }
            }

        } catch (IOException e) {
            logger.error("Server failed to start on port " + PORT, (Exception) e);
            System.err.println("ERROR: Could not start server on port " + PORT);
            System.err.println("Make sure the port is not already in use.");
        } finally {
            // Shutdown the thread pool gracefully
            threadPool.shutdownNow();
            logger.info("Server shut down. Thread pool terminated.");
        }
    }
}
