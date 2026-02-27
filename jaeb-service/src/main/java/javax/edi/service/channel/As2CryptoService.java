package javax.edi.service.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.edi.service.entity.CommunicationChannel;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AS2 cryptographic operations using Bouncy Castle S/MIME.
 *
 * Handles:
 * <ul>
 *   <li><b>Decryption</b> â decrypt S/MIME enveloped data using our private key</li>
 *   <li><b>Signature verification</b> â verify S/MIME signed data using partner's certificate</li>
 *   <li><b>MIC calculation</b> â compute Message Integrity Check for MDN</li>
 * </ul>
 *
 * Certificates and keys are stored as PEM strings in the {@link CommunicationChannel} entity.
 */
@Service
public class As2CryptoService {

    private static final Logger LOG = LoggerFactory.getLogger(As2CryptoService.class);

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            LOG.info("BouncyCastle security provider registered for AS2 S/MIME");
        }
    }

    // ======================== Decryption ========================

    /**
     * Decrypt an S/MIME encrypted (enveloped) message body.
     *
     * @param encryptedBody   the raw encrypted bytes from the HTTP request
     * @param contentType     the Content-Type header (e.g. "application/pkcs7-mime; smime-type=enveloped-data")
     * @param channel         the AS2 channel containing our private key
     * @return the decrypted payload bytes
     * @throws As2CryptoException if decryption fails or no private key is configured
     */
    public byte[] decrypt(byte[] encryptedBody, String contentType, CommunicationChannel channel)
            throws As2CryptoException {

        if (channel.getAs2LocalPrivateKey() == null || channel.getAs2LocalPrivateKey().isEmpty()) {
            throw new As2CryptoException("Cannot decrypt: no local private key configured for channel "
                    + channel.getChannelName());
        }
        if (channel.getAs2LocalCertificate() == null || channel.getAs2LocalCertificate().isEmpty()) {
            throw new As2CryptoException("Cannot decrypt: no local certificate configured for channel "
                    + channel.getChannelName());
        }

        try {
            PrivateKey privateKey = loadPrivateKey(channel.getAs2LocalPrivateKey());
            X509Certificate localCert = loadCertificate(channel.getAs2LocalCertificate());

            // Build a MimeMessage from the raw body + content-type
            MimeBodyPart encryptedPart = toMimeBodyPart(encryptedBody, contentType);

            SMIMEEnveloped enveloped = new SMIMEEnveloped(encryptedPart);
            RecipientInformationStore recipients = enveloped.getRecipientInfos();
            RecipientInformation recipient = recipients.get(new JceKeyTransRecipientId(localCert));

            if (recipient == null) {
                // Try the first available recipient
                Collection<RecipientInformation> allRecipients = recipients.getRecipients();
                if (allRecipients.isEmpty()) {
                    throw new As2CryptoException("No recipients found in encrypted message");
                }
                recipient = allRecipients.iterator().next();
                LOG.warn("Exact recipient match failed; using first available recipient");
            }

            byte[] decrypted = recipient.getContent(
                    new JceKeyTransEnvelopedRecipient(privateKey).setProvider("BC"));

            LOG.info("AS2 message decrypted successfully ({} bytes -> {} bytes)",
                    encryptedBody.length, decrypted.length);
            return decrypted;

        } catch (As2CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new As2CryptoException("Decryption failed: " + e.getMessage(), e);
        }
    }

    // ======================== Signature Verification ========================

    /**
     * Verify the S/MIME signature on an AS2 message.
     *
     * @param signedBody  the raw signed bytes (multipart/signed or application/pkcs7-mime)
     * @param contentType the Content-Type header
     * @param channel     the AS2 channel containing the partner's certificate
     * @return the result containing the verified payload and MIC info
     * @throws As2CryptoException if verification fails
     */
    public VerifyResult verifySignature(byte[] signedBody, String contentType, CommunicationChannel channel)
            throws As2CryptoException {

        if (channel.getAs2PartnerCertificate() == null || channel.getAs2PartnerCertificate().isEmpty()) {
            throw new As2CryptoException("Cannot verify signature: no partner certificate configured for channel "
                    + channel.getChannelName());
        }

        try {
            X509Certificate partnerCert = loadCertificate(channel.getAs2PartnerCertificate());
            MimeBodyPart signedPart = toMimeBodyPart(signedBody, contentType);

            SMIMESigned signed = new SMIMESigned((MimeMultipart) signedPart.getContent());

            // Verify each signer
            SignerInformationStore signers = signed.getSignerInfos();
            boolean verified = false;
            String micAlgorithm = null;

            for (SignerInformation signer : signers.getSigners()) {
                SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder()
                        .setProvider("BC")
                        .build(partnerCert);

                if (signer.verify(verifier)) {
                    verified = true;
                    micAlgorithm = resolveDigestAlgorithmName(signer.getDigestAlgOID());
                    LOG.info("AS2 signature verified (algorithm: {})", micAlgorithm);
                    break;
                }
            }

            if (!verified) {
                // Try using embedded certificates if the partner cert didn't match
                Store<X509CertificateHolder> certStore = signed.getCertificates();
                for (SignerInformation signer : signers.getSigners()) {
                    Collection<X509CertificateHolder> matches = certStore.getMatches(signer.getSID());
                    for (X509CertificateHolder holder : matches) {
                        X509Certificate embeddedCert = new JcaX509CertificateConverter()
                                .setProvider("BC").getCertificate(holder);
                        SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder()
                                .setProvider("BC")
                                .build(embeddedCert);
                        if (signer.verify(verifier)) {
                            verified = true;
                            micAlgorithm = resolveDigestAlgorithmName(signer.getDigestAlgOID());
                            LOG.warn("AS2 signature verified using embedded cert (not the configured partner cert)");
                            break;
                        }
                    }
                    if (verified) break;
                }
            }

            if (!verified) {
                throw new As2CryptoException("AS2 signature verification failed: "
                        + "signature does not match partner certificate");
            }

            // Extract the signed content
            MimeBodyPart content = signed.getContent();
            byte[] payload = extractBytes(content);

            return new VerifyResult(payload, micAlgorithm, true);

        } catch (As2CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new As2CryptoException("Signature verification failed: " + e.getMessage(), e);
        }
    }

    // ======================== Payload Processing ========================

    /**
     * Process an incoming AS2 message body: decrypt if encrypted, verify if signed,
     * and return the plain EDI payload.
     *
     * @param rawBody     the raw bytes from the HTTP request
     * @param contentType the Content-Type header
     * @param channel     the matched AS2 channel (may be null if no channel matched)
     * @return the processing result containing the EDI payload
     */
    public ProcessResult processInbound(byte[] rawBody, String contentType, CommunicationChannel channel) {
        if (channel == null) {
            // No channel matched â treat as plaintext
            return new ProcessResult(rawBody, false, false, null);
        }

        byte[] currentPayload = rawBody;
        String currentContentType = contentType != null ? contentType : "";
        boolean wasDecrypted = false;
        boolean wasVerified = false;
        String micAlgorithm = null;

        try {
            // Step 1: Decrypt if encrypted
            if (isEncrypted(currentContentType)) {
                LOG.info("AS2 message is encrypted â decrypting");
                currentPayload = decrypt(currentPayload, currentContentType, channel);
                // After decryption, the payload may be signed â detect new content type
                currentContentType = detectContentType(currentPayload);
                wasDecrypted = true;
            }

            // Step 2: Verify signature if signed
            if (isSigned(currentContentType)) {
                LOG.info("AS2 message is signed â verifying");
                VerifyResult vr = verifySignature(currentPayload, currentContentType, channel);
                currentPayload = vr.getPayload();
                wasVerified = vr.isVerified();
                micAlgorithm = vr.getMicAlgorithm();
            }

            return new ProcessResult(currentPayload, wasDecrypted, wasVerified, micAlgorithm);

        } catch (As2CryptoException e) {
            LOG.error("AS2 crypto processing failed: {}", e.getMessage());
            // Return what we have â the controller will decide how to handle
            return new ProcessResult(currentPayload, wasDecrypted, wasVerified, micAlgorithm, e.getMessage());
        }
    }

    // ======================== Certificate / Key loading ========================

    /**
     * Load an X.509 certificate from a PEM string.
     */
    public X509Certificate loadCertificate(String pem) throws As2CryptoException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new As2CryptoException("Failed to load certificate: " + e.getMessage(), e);
        }
    }

    /**
     * Load a private key from a PEM string.
     * Supports PKCS#8 and traditional RSA/EC key formats.
     */
    public PrivateKey loadPrivateKey(String pem) throws As2CryptoException {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (obj instanceof PEMKeyPair) {
                return converter.getKeyPair((PEMKeyPair) obj).getPrivate();
            } else if (obj instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) obj);
            } else {
                throw new As2CryptoException("Unsupported private key format: "
                        + (obj != null ? obj.getClass().getName() : "null"));
            }
        } catch (As2CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new As2CryptoException("Failed to load private key: " + e.getMessage(), e);
        }
    }

    /**
     * Validate that certificate and key are a matching pair.
     *
     * @return true if the certificate's public key matches the private key
     */
    public boolean validateKeyPair(String certPem, String keyPem) {
        try {
            X509Certificate cert = loadCertificate(certPem);
            PrivateKey key = loadPrivateKey(keyPem);

            // Sign and verify to confirm the key pair matches
            java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA", "BC");
            byte[] testData = "AS2-KEY-PAIR-VALIDATION".getBytes(StandardCharsets.UTF_8);

            sig.initSign(key);
            sig.update(testData);
            byte[] signature = sig.sign();

            sig.initVerify(cert.getPublicKey());
            sig.update(testData);
            return sig.verify(signature);

        } catch (Exception e) {
            LOG.warn("Key pair validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ======================== Helpers ========================

    private boolean isEncrypted(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ct.contains("application/pkcs7-mime") || ct.contains("application/x-pkcs7-mime")
                || ct.contains("smime-type=enveloped-data");
    }

    private boolean isSigned(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ct.contains("multipart/signed") || ct.contains("application/pkcs7-signature")
                || ct.contains("smime-type=signed-data");
    }

    private MimeBodyPart toMimeBodyPart(byte[] body, String contentType) throws Exception {
        // Wrap in a full MIME message so javax.mail can parse it
        StringBuilder raw = new StringBuilder();
        raw.append("Content-Type: ").append(contentType).append("\r\n");
        raw.append("\r\n");

        ByteArrayOutputStream full = new ByteArrayOutputStream();
        full.write(raw.toString().getBytes(StandardCharsets.US_ASCII));
        full.write(body);

        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage msg = new MimeMessage(session, new ByteArrayInputStream(full.toByteArray()));

        MimeBodyPart part = new MimeBodyPart();
        part.setContent(msg.getContent(), msg.getContentType());
        part.setHeader("Content-Type", msg.getContentType());
        return part;
    }

    private byte[] extractBytes(MimeBodyPart part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String) {
            return ((String) content).getBytes(StandardCharsets.UTF_8);
        } else if (content instanceof byte[]) {
            return (byte[]) content;
        } else {
            // Fallback: write to stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            part.writeTo(baos);
            return baos.toByteArray();
        }
    }

    private String detectContentType(byte[] payload) {
        // Simple heuristic: check if the decrypted payload starts with MIME headers
        String head = new String(payload, 0, Math.min(payload.length, 256), StandardCharsets.US_ASCII);
        if (head.toLowerCase().contains("content-type:")) {
            int start = head.toLowerCase().indexOf("content-type:");
            int end = head.indexOf("\r\n", start);
            if (end < 0) end = head.indexOf("\n", start);
            if (end > start) {
                return head.substring(start + "content-type:".length(), end).trim();
            }
        }
        return "application/octet-stream";
    }

    private String resolveDigestAlgorithmName(String oid) {
        if (OIWObjectIdentifiers.idSHA1.getId().equals(oid)) return "sha-1";
        if (NISTObjectIdentifiers.id_sha256.getId().equals(oid)) return "sha-256";
        if (NISTObjectIdentifiers.id_sha384.getId().equals(oid)) return "sha-384";
        if (NISTObjectIdentifiers.id_sha512.getId().equals(oid)) return "sha-512";
        return oid; // fallback to raw OID
    }

    // ======================== Result DTOs ========================

    /** Result of signature verification. */
    public static class VerifyResult {
        private final byte[] payload;
        private final String micAlgorithm;
        private final boolean verified;

        public VerifyResult(byte[] payload, String micAlgorithm, boolean verified) {
            this.payload = payload;
            this.micAlgorithm = micAlgorithm;
            this.verified = verified;
        }

        public byte[] getPayload() { return payload; }
        public String getMicAlgorithm() { return micAlgorithm; }
        public boolean isVerified() { return verified; }
    }

    /** Result of full inbound processing (decrypt + verify). */
    public static class ProcessResult {
        private final byte[] payload;
        private final boolean decrypted;
        private final boolean signatureVerified;
        private final String micAlgorithm;
        private final String error;

        public ProcessResult(byte[] payload, boolean decrypted, boolean signatureVerified, String micAlgorithm) {
            this(payload, decrypted, signatureVerified, micAlgorithm, null);
        }

        public ProcessResult(byte[] payload, boolean decrypted, boolean signatureVerified,
                             String micAlgorithm, String error) {
            this.payload = payload;
            this.decrypted = decrypted;
            this.signatureVerified = signatureVerified;
            this.micAlgorithm = micAlgorithm;
            this.error = error;
        }

        public byte[] getPayload() { return payload; }
        public boolean isDecrypted() { return decrypted; }
        public boolean isSignatureVerified() { return signatureVerified; }
        public String getMicAlgorithm() { return micAlgorithm; }
        public String getError() { return error; }
        public boolean hasError() { return error != null; }
    }
}
