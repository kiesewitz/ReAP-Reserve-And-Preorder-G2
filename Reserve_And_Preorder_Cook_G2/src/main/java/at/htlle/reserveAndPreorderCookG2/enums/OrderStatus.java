package at.htlle.reserveAndPreorderCookG2.enums;

public enum OrderStatus {
    PENDING,     // Neu erstellt, wartet
    IN_KITCHEN,  // In der Küche, wird zubereitet
    READY,       // Fertig, bereit zum Servieren
    SERVED,      // Serviert
    CANCELLED    // Storniert
}
