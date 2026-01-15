package at.htlle.reap.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

@Service
public class QRCodeService {

    // Secret key for HMAC signing (in production, load from environment variable!)
    private static final String SECRET_KEY = "reap-secret-key-change-in-production";

    // QR code dimensions
    private static final int QR_WIDTH = 300;
    private static final int QR_HEIGHT = 300;

    // Token validity period in days
    private static final int TOKEN_VALIDITY_DAYS = 7;

    /**
     * Generate a secure token for check-in
     * Format: Base64(reservationId:guestId:expiryTimestamp:signature)
     */
    public String generateCheckinToken(Long reservationId, Long guestId) {
        try {
            // Calculate expiry timestamp (7 days from now)
            long expiryTimestamp = LocalDateTime.now()
                    .plusDays(TOKEN_VALIDITY_DAYS)
                    .toEpochSecond(ZoneOffset.UTC);

            // Create payload
            String payload = reservationId + ":" +
                           (guestId != null ? guestId : "0") + ":" +
                           expiryTimestamp;

            // Generate HMAC signature
            String signature = generateHmacSignature(payload);

            // Combine payload and signature
            String tokenData = payload + ":" + signature;

            // Encode to Base64
            return Base64.getUrlEncoder().encodeToString(tokenData.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    /**
     * Validate a token and extract reservation ID
     */
    public ValidationResult validateToken(String token) {
        try {
            // Decode from Base64
            String tokenData = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);

            // Split into parts
            String[] parts = tokenData.split(":");
            if (parts.length != 4) {
                return new ValidationResult(false, null, null, "Invalid token format");
            }

            Long reservationId = Long.parseLong(parts[0]);
            Long guestId = parts[1].equals("0") ? null : Long.parseLong(parts[1]);
            long expiryTimestamp = Long.parseLong(parts[2]);
            String providedSignature = parts[3];

            // Check expiry
            long currentTimestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            if (currentTimestamp > expiryTimestamp) {
                return new ValidationResult(false, null, null, "Token expired");
            }

            // Verify signature
            String payload = parts[0] + ":" + parts[1] + ":" + parts[2];
            String expectedSignature = generateHmacSignature(payload);

            if (!expectedSignature.equals(providedSignature)) {
                return new ValidationResult(false, null, null, "Invalid signature");
            }

            // Token is valid
            return new ValidationResult(true, reservationId, guestId, "Valid");

        } catch (Exception e) {
            return new ValidationResult(false, null, null, "Token validation failed: " + e.getMessage());
        }
    }

    /**
     * Generate HMAC-SHA256 signature
     */
    private String generateHmacSignature(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKeySpec);
        byte[] signatureBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().encodeToString(signatureBytes);
    }

    /**
     * Generate QR code image as Base64 string
     * @param url The URL to encode (e.g., https://reap-app.com/checkin?token=xyz)
     * @return Base64-encoded PNG image
     */
    public String generateQRCodeImage(String url) {
        try {
            // Create QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT);

            // Convert to PNG image
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            // Encode to Base64
            return Base64.getEncoder().encodeToString(imageBytes);

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }

    /**
     * Generate complete check-in URL with QR code
     * @param baseUrl Base URL of the application (e.g., http://localhost:8080)
     * @param reservationId Reservation ID
     * @param guestId Guest ID (null for single reservation)
     * @return Check-in URL
     */
    public String generateCheckinUrl(String baseUrl, Long reservationId, Long guestId) {
        String token = generateCheckinToken(reservationId, guestId);
        return baseUrl + "/checkin?token=" + token;
    }

    /**
     * Generate QR code for check-in (URL + image)
     * @param baseUrl Base URL
     * @param reservationId Reservation ID
     * @param guestId Guest ID (null for single reservation)
     * @return QRCodeData object with URL and Base64 image
     */
    public QRCodeData generateCheckinQRCode(String baseUrl, Long reservationId, Long guestId) {
        String url = generateCheckinUrl(baseUrl, reservationId, guestId);
        String imageBase64 = generateQRCodeImage(url);
        String token = generateCheckinToken(reservationId, guestId);

        return new QRCodeData(url, imageBase64, token);
    }

    /**
     * Result of token validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final Long reservationId;
        private final Long guestId;
        private final String message;

        public ValidationResult(boolean valid, Long reservationId, Long guestId, String message) {
            this.valid = valid;
            this.reservationId = reservationId;
            this.guestId = guestId;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public Long getReservationId() {
            return reservationId;
        }

        public Long getGuestId() {
            return guestId;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * QR code data container
     */
    public static class QRCodeData {
        private final String url;
        private final String imageBase64;
        private final String token;

        public QRCodeData(String url, String imageBase64, String token) {
            this.url = url;
            this.imageBase64 = imageBase64;
            this.token = token;
        }

        public String getUrl() {
            return url;
        }

        public String getImageBase64() {
            return imageBase64;
        }

        public String getToken() {
            return token;
        }

        public String getDataUrl() {
            return "data:image/png;base64," + imageBase64;
        }
    }
}
