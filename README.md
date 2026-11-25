# Concurrent Ticket Reservation System (ConcurRes)

A Java client/server application for reserving seats on trains and airplanes. A multithreaded TCP server owns the booking state; a Swing GUI connects to it over sockets.

## How it works

- `server.ServerMain` opens a `ServerSocket` on port 5000 and submits each accepted connection to a fixed 10-thread `ExecutorService`. Each socket is handled by a `server.ClientHandler` running on a pool thread.
- The client and server exchange serialized `Request` and `Response` objects through `ObjectInputStream` / `ObjectOutputStream`. Request types are `VIEW_VEHICLES`, `BOOK_SEAT`, `CANCEL_RESERVATION`, `VIEW_RESERVATIONS`, and `DISCONNECT`.
- `server.DataManager` is a singleton holding vehicles and reservations in memory. It uses a `ReentrantReadWriteLock`: reads take the shared read lock; booking and cancellation take the exclusive write lock. Those two write operations also rewrite the data files before releasing the lock.
- Vehicles are loaded from `data/vehicles.csv` and reservations from `data/reservations.txt` at startup. The CSV `Type` column selects `Train` or `Airplane`. Seat numbers are checked against the vehicle's booked-seat set and its total capacity; a successful booking creates a `TKT-nnn` ticket, and cancellation restores the seat.
- `shared.models.Vehicle` is abstract and implements `Serializable` and `Reservable`. `Train` and `Airplane` extend it and override `calculateTicketPrice(String)`: both apply a seat-class multiplier, then trains add a 10% rail tax and airplanes add 15% fuel surcharge plus 5% airport tax.
- `server.Logger` is a singleton with synchronized methods that print events to the console and append timestamped lines to `data/server.log`.
- `client.ClientGUI` provides host/port fields (default `localhost` / `5000`) and three tabs: Browse Vehicles, Book Ticket, and My Reservations. The booking form shows a price preview before submitting.

## Run it

Requires a JDK with `javac` and `java` on PATH. The scripts are Windows batch files and the data paths are relative to the project root, so run them from this directory.

```bat
compile.bat
run_server.bat
```

Then, in another terminal:

```bat
run_client.bat
```

The equivalent manual commands are:

```bash
javac -d bin src\shared\models\*.java src\shared\protocol\*.java src\server\*.java src\client\*.java
java -cp bin server.ServerMain
java -cp bin client.ClientGUI
```

Multiple client windows can connect to the same server process.

## Files

| Path | Contents |
| --- | --- |
| `src/shared/models/` | `Reservable`, `Vehicle`, `Train`, `Airplane`, `Ticket`. |
| `src/shared/protocol/` | `Request` and `Response` message classes. |
| `src/server/` | `ServerMain`, `ClientHandler`, `DataManager`, `Logger`. |
| `src/client/` | `ClientGUI`, the Swing client. |
| `data/vehicles.csv` | Vehicle inventory (ID, type, route, seats, price). |
| `data/reservations.txt` | Persisted ticket records. |
| `compile.bat`, `run_server.bat`, `run_client.bat` | Compile and launch scripts. |

Compiled classes in `bin/` and the generated `data/server.log` are not tracked.
