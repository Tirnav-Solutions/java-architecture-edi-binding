package javax.edi.service.channel;

/**
 * Exception thrown when AS2 cryptographic operations fail
 * (decryption, signature verification, certificate loading).
 */
public class As2CryptoException extends Exception {

    public As2CryptoException(String message) {
        super(message);
    }

    public As2CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
