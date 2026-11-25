package shared.models;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Train Class
 * ═══════════════════════════════════════════════════════════════════
 *  OOP Concepts: INHERITANCE, POLYMORPHISM
 *
 *  Train extends the abstract Vehicle class and inherits the
 *  Reservable interface implementation. It provides its own
 *  pricing logic via calculateTicketPrice() — demonstrating
 *  POLYMORPHISM (runtime method dispatch).
 *
 *  Train Pricing Rules:
 *  ┌──────────┬──────────────┬─────────────┐
 *  │  Class   │  Multiplier  │    Tax      │
 *  ├──────────┼──────────────┼─────────────┤
 *  │ Economy  │    1.0x      │  10% rail   │
 *  │ Business │    1.5x      │  10% rail   │
 *  │ First    │    2.0x      │  10% rail   │
 *  └──────────┴──────────────┴─────────────┘
 * ═══════════════════════════════════════════════════════════════════
 */
public class Train extends Vehicle {

    private static final long serialVersionUID = 2L;

    // Train-specific constants
    private static final double RAIL_TAX_RATE = 0.10;           // 10% railway tax
    private static final double ECONOMY_MULTIPLIER = 1.0;
    private static final double BUSINESS_MULTIPLIER = 1.5;
    private static final double FIRST_CLASS_MULTIPLIER = 2.0;

    /**
     * Default constructor.
     */
    public Train() {
        super();
    }

    /**
     * Parameterized constructor.
     *
     * @param vehicleId      unique identifier
     * @param route          travel route
     * @param totalSeats     total seat capacity
     * @param availableSeats available seats
     * @param basePrice      base ticket price
     */
    public Train(String vehicleId, String route,
                 int totalSeats, int availableSeats, double basePrice) {
        super(vehicleId, "Train", route, totalSeats, availableSeats, basePrice);
    }

    /**
     * POLYMORPHISM: Train-specific ticket pricing.
     *
     * The price is calculated as:
     *   finalPrice = basePrice × classMultiplier × (1 + railTax)
     *
     * Trains have a simpler tax structure compared to Airplanes,
     * applying only a 10% railway service tax.
     *
     * @param seatClass "Economy", "Business", or "First"
     * @return the calculated ticket price
     */
    @Override
    public double calculateTicketPrice(String seatClass) {
        double price = getBasePrice();

        // Apply seat class multiplier
        switch (seatClass) {
            case "First":
                price *= FIRST_CLASS_MULTIPLIER;
                break;
            case "Business":
                price *= BUSINESS_MULTIPLIER;
                break;
            case "Economy":
            default:
                price *= ECONOMY_MULTIPLIER;
                break;
        }

        // Apply 10% railway tax
        price *= (1 + RAIL_TAX_RATE);

        // Round to 2 decimal places
        return Math.round(price * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return "🚆 " + super.toString();
    }
}
