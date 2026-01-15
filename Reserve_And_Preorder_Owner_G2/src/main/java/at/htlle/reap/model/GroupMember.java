package at.htlle.reap.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@jakarta.persistence.Table(name = "group_members")
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "guest_name", nullable = false, length = 200)
    private String guestName;

    @Column(name = "guest_email", length = 255)
    private String guestEmail;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "has_checked_in")
    private boolean hasCheckedIn = false;

    // No-arg constructor for JPA
    public GroupMember() {
    }

    // Constructor with essential fields
    public GroupMember(String guestName, String guestEmail) {
        this.guestName = guestName;
        this.guestEmail = guestEmail;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public boolean isHasCheckedIn() {
        return hasCheckedIn;
    }

    public void setHasCheckedIn(boolean hasCheckedIn) {
        this.hasCheckedIn = hasCheckedIn;
    }

    @Override
    public String toString() {
        return "GroupMember{" +
                "id=" + id +
                ", guestName='" + guestName + '\'' +
                ", guestEmail='" + guestEmail + '\'' +
                ", hasCheckedIn=" + hasCheckedIn +
                '}';
    }
}
