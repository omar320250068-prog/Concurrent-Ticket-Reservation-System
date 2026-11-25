package shared.models;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Airplane Class
 * ═══════════════════════════════════════════════════════════════════
 *  OOP Concepts: INHERITANCE, POLYMORPHISM
 *
 *  Airplane extends the abstract Vehicle class and inherits the
 *  Reservable interface implementation. It provides its own
 *  pricing logic via calculateTicketPrice() — demonstrating
 *  POLYMORPHISM (runtime method dispatch).
 *
 *  Airplane Pricing Rules:
 *  ┌──────────┬──────────────┬──────────────────────────────┐
 *  │  Class   │  Multiplier  │         Taxes/Fees           │
 *  ├──────────┼──────────────┼──────────────────────────────┤
 *  │ Economy  │    1.0x      │  15% fuel + 5% airport = 20%│
 *  │ Business │    2.0x      │  15% fuel + 5% airport = 20%│
 *  │ First    │    3.0x      │  15% fuel + 5% airport = 20%│
 *  └──────────┴──────────────┴──────────────────────────────┘
 *
 *  Notice how Airplane has HIGHER multipliers and a DIFFERENT
 *  tax structure compared to Train — this is the essence of
 *  polymorphism: same method, different behavior.
 * ═══════════════════════════════════════════════════════════════════
 */
public class Airplane extends Vehicle {

    private static final long serialVersionUID = 3L;

    // Airplane-specific constants
    private static final double FUEL_SURCHARGE_RATE = 0.15;     // 15% fuel surcharge
    private static final double AIRPORT_TAX_RATE = 0.05;        // 5% airport tax
    private static final double TOTAL_TAX_RATE = FUEL_SURCHARGE_RATE + AIRPORT_TAX_RATE; // 20%
    private static final double ECONOMY_MULTIPLIER = 1.0;
    private static final double BUSINESS_MULTIPLIER = 2.0;
    private static final double FIRST_CLASS_MULTIPLIER = 3.0;

    /**
     * Default constructor.
     */
    public Airplane() {
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
    public Airplane(String vehicleId, String route,
                    int totalSeats, int availableSeats, double basePrice) {
        super(vehicleId, "Airplane", route, totalSeats, availableSeats, basePrice);
    }

    /**
     * POLYMORPHISM: Airplane-specific ticket pricing.
     *
     * The price is calculated as:
     *   finalPrice = basePrice × classMultiplier × (1 + fuelSurcharge + airportTax)
     *
     * Airplanes have higher class multipliers and an additional
     * fuel surcharge (15%) on top of the airport tax (5%),
     * resulting in a total 20% surcharge.
     *
     * @param seatClass "Economy", "Business", or "First"
     * @return the calculated ticket price
     */
    @Override
    public double calculateTicketPrice(String seatClass) {
        double price = getBasePrice();

        // Apply seat class multiplier (higher than Train for premium classes)
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

        // Apply 20% total surcharge (15% fuel + 5% airport tax)
        price *= (1 + TOTAL_TAX_RATE);

        // Round to 2 decimal places
        return Math.round(price * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return "✈️ " + super.toString();
    }
}
