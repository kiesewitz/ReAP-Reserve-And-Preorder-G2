package at.htlle.reap.enums;

public enum ReservationStatus {
    PENDING,         // Reservierung erstellt, wartet auf Bestätigung
    CONFIRMED,       // Reservierung bestätigt
    CHECKED_IN,      // Gast ist angekommen und eingecheckt
    COMPLETED,       // Besuch beendet
    CANCELLED,       // Storniert durch Kunde
    NO_SHOW,         // Kunde nicht erschienen
    TIMEOUT_WARNING  // 2 Stunden überschritten
}
