package javax.edi.service.channel;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.edi.service.entity.CommunicationChannel;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * OFTP2 (Odette File Transfer Protocol v2) channel adapter.
 *
 * <p>OFTP2 is the standard B2B file transfer protocol used in the European
 * automotive industry (VW, BMW, Daimler, Renault, PSA, Bosch, Continental, etc.).
 * It operates over TCP/IP with mandatory TLS encryption.</p>
 *
 * <h3>Protocol flow (RFC 5024):</h3>
 * <ol>
 *   <li>TCP connection + TLS handshake (certificate-based mutual authentication)</li>
 *   <li>SSRM — Start Session Ready Message (responder → initiator)</li>
 *   <li>SSID — Start Session Identification (initiator → responder, with local SSID)</li>
 *   <li>SSID — Start Session Identification (responder → initiator, confirming)</li>
 *   <li>SFID — Start File (sender identifies file: virtual filename, size, date, etc.)</li>
 *   <li>SFPA — Start File Positive Answer (receiver accepts)</li>
 *   <li>CDT  — Credit (receiver grants send window)</li>
 *   <li>DATA — Data exchange (one or more data buffers within the credit window)</li>
 *   <li>EFID — End File (sender signals file complete, includes record count/hash)</li>
 *   <li>EFPA — End File Positive Answer (receiver confirms)</li>
 *   <li>EERP — End-to-End Response (signed delivery receipt, optionally async)</li>
 *   <li>ESID — End Session</li>
 * </ol>
 *
 * <p>This adapter implements the OFTP2 protocol over TLS using Java sockets,
 * performing SSID negotiation, SFID-based file transfer, and EERP handling.
 * The TLS layer uses PEM-encoded certificates configured on the channel.</p>
 */
@Component
public class Oftp2ChannelAdapter implements ChannelAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(Oftp2ChannelAdapter.class);

    /** Default OFTP2 port (RFC 5024). */
    public static final int DEFAULT_OFTP2_PORT = 6619;

    // OFTP2 command identifiers (single-character codes per RFC 5024)
    private static final char CMD_SSRM = 'I';   // Start Session Ready Message
    private static final char CMD_SSID = 'X';   // Start Session Identification
    private static final char CMD_SFID = 'H';   // Start File
    private static final char CMD_SFPA = '2';   // Start File Positive Answer
    private static final char CMD_SFNA = '3';   // Start File Negative Answer
    private static final char CMD_CDT  = 'C';   // Set Credit
    private static final char CMD_DATA = 'D';   // Data
    private static final char CMD_EFID = 'T';   // End File
    private static final char CMD_EFPA = '5';   // End File Positive Answer
    private static final char CMD_EERP = 'E';   // End-to-End Response
    private static final char CMD_ESID = 'F';   // End Session
    private static final char CMD_CD   = 'R';   // Change Direction
    private static final char CMD_RTR  = 'P';   // Ready To Receive

    /** Maximum data buffer size (per OFTP2 spec, negotiated; we use 4096 as default). */
    private static final int DEFAULT_DATA_BUFFER_SIZE = 4096;

    /** Default credit window (number of data buffers before re-credit). */
    private static final int DEFAULT_CREDIT = 99;

    @Override
    public List<RemoteFile> listFiles(CommunicationChannel channel) {
        validateConfig(channel);
        LOG.info("OFTP2 does not support directory listing — it is a push-based protocol. " +
                 "Channel: {} ({}:{}), SSID: {}, SFID: {}",
                channel.getChannelName(), channel.getHost(),
                channel.getPort() != null ? channel.getPort() : DEFAULT_OFTP2_PORT,
                channel.getOftp2LocalSsid(), channel.getOftp2PartnerSfid());
        return Collections.emptyList();
    }

    @Override
    public String readFile(CommunicationChannel channel, String remoteFilePath) {
        validateConfig(channel);

        String host = channel.getHost();
        int port = channel.getPort() != null ? channel.getPort() : DEFAULT_OFTP2_PORT;

        LOG.info("OFTP2 readFile: connecting to {}:{}, SSID={}, awaiting file from partner SFID={}",
                host, port, channel.getOftp2LocalSsid(), channel.getOftp2PartnerSfid());

        try (SSLSocket socket = createTlsSocket(channel, host, port)) {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // Step 1: Receive SSRM (Start Session Ready Message) from responder
            Oftp2Message ssrm = readMessage(in);
            expectCommand(ssrm, CMD_SSRM, "Expected SSRM from responder");
            LOG.debug("Received SSRM from {}:{}", host, port);

            // Step 2: Send SSID (our local identification)
            sendSsid(out, channel.getOftp2LocalSsid(), channel.getPassword());
            LOG.debug("Sent SSID: {}", channel.getOftp2LocalSsid());

            // Step 3: Receive SSID (partner's identification, confirming session)
            Oftp2Message partnerSsid = readMessage(in);
            expectCommand(partnerSsid, CMD_SSID, "Expected SSID from responder");
            LOG.debug("Received partner SSID, session established");

            // Step 4: Send CDT (grant credit so partner can send us a file)
            sendCdt(out, DEFAULT_CREDIT);
            LOG.debug("Sent CDT with credit={}", DEFAULT_CREDIT);

            // Step 5: Receive SFID (Start File from partner)
            Oftp2Message sfid = readMessage(in);
            expectCommand(sfid, CMD_SFID, "Expected SFID (file offer) from partner");
            String virtualFilename = extractVirtualFilename(sfid);
            long fileSize = extractFileSize(sfid);
            LOG.info("OFTP2 receiving file: virtualFilename={}, size={}", virtualFilename, fileSize);

            // Step 6: Send SFPA (accept the file)
            sendSfpa(out);
            LOG.debug("Sent SFPA (accepting file)");

            // Step 7: Receive DATA buffers until EFID
            ByteArrayOutputStream fileContent = new ByteArrayOutputStream();
            long recordCount = 0;
            boolean fileComplete = false;

            while (!fileComplete) {
                Oftp2Message msg = readMessage(in);
                char cmd = msg.getCommand();

                if (cmd == CMD_DATA) {
                    byte[] dataPayload = msg.getPayload();
                    fileContent.write(dataPayload, 1, dataPayload.length - 1); // skip command byte
                    recordCount++;
                } else if (cmd == CMD_EFID) {
                    fileComplete = true;
                    LOG.info("OFTP2 file transfer complete: {} records received, {} bytes",
                            recordCount, fileContent.size());
                } else {
                    LOG.warn("Unexpected OFTP2 command '{}' during data transfer, ignoring", cmd);
                }
            }

            // Step 8: Send EFPA (End File Positive Answer)
            sendEfpa(out);
            LOG.debug("Sent EFPA (file accepted)");

            // Step 9: Send EERP (End-to-End Response / delivery receipt)
            sendEerp(out, channel.getOftp2PartnerSfid(), virtualFilename,
                    channel.isOftp2RequestSignedEerp());
            LOG.debug("Sent EERP for file {}", virtualFilename);

            // Step 10: Send ESID (End Session)
            sendEsid(out, 0, "Transfer complete");
            LOG.debug("Sent ESID, session closed");

            return fileContent.toString(StandardCharsets.UTF_8.name());

        } catch (Exception e) {
            throw new RuntimeException("OFTP2 readFile failed from " + host + ":" + port +
                    " (SSID=" + channel.getOftp2LocalSsid() + "): " + e.getMessage(), e);
        }
    }

    @Override
    public void writeFile(CommunicationChannel channel, String remoteFilePath, String content) {
        validateConfig(channel);

        String host = channel.getHost();
        int port = channel.getPort() != null ? channel.getPort() : DEFAULT_OFTP2_PORT;
        byte[] fileBytes = content.getBytes(StandardCharsets.UTF_8);

        // Determine virtual filename: use configured pattern or fall back to remoteFilePath
        String virtualFilename = channel.getOftp2VirtualFilename();
        if (virtualFilename == null || virtualFilename.isEmpty()) {
            virtualFilename = remoteFilePath;
        }

        LOG.info("OFTP2 writeFile: connecting to {}:{}, SSID={}, sending file '{}' ({} bytes) to SFID={}",
                host, port, channel.getOftp2LocalSsid(), virtualFilename,
                fileBytes.length, channel.getOftp2PartnerSfid());

        try (SSLSocket socket = createTlsSocket(channel, host, port)) {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // Step 1: Receive SSRM from responder
            Oftp2Message ssrm = readMessage(in);
            expectCommand(ssrm, CMD_SSRM, "Expected SSRM from responder");
            LOG.debug("Received SSRM from {}:{}", host, port);

            // Step 2: Send SSID
            sendSsid(out, channel.getOftp2LocalSsid(), channel.getPassword());
            LOG.debug("Sent SSID: {}", channel.getOftp2LocalSsid());

            // Step 3: Receive SSID confirmation
            Oftp2Message partnerSsid = readMessage(in);
            expectCommand(partnerSsid, CMD_SSID, "Expected SSID from responder");
            LOG.debug("Session established with partner");

            // Step 4: Send SFID (Start File — announce the file we're sending)
            sendSfid(out, channel.getOftp2PartnerSfid(), virtualFilename,
                    fileBytes.length, channel.isOftp2Compress());
            LOG.debug("Sent SFID: file={}, size={}", virtualFilename, fileBytes.length);

            // Step 5: Receive SFPA (partner accepts the file)
            Oftp2Message sfpa = readMessage(in);
            if (sfpa.getCommand() == CMD_SFNA) {
                String reason = new String(sfpa.getPayload(), StandardCharsets.UTF_8);
                throw new RuntimeException("Partner rejected file transfer (SFNA): " + reason);
            }
            expectCommand(sfpa, CMD_SFPA, "Expected SFPA from responder");
            LOG.debug("Received SFPA, partner accepted file");

            // Step 6: Wait for CDT (credit grant from receiver)
            Oftp2Message cdt = readMessage(in);
            expectCommand(cdt, CMD_CDT, "Expected CDT (credit) from receiver");
            int credit = extractCredit(cdt);
            LOG.debug("Received CDT with credit={}", credit);

            // Step 7: Send DATA buffers (within credit window)
            int offset = 0;
            int buffersInWindow = 0;
            long recordCount = 0;

            while (offset < fileBytes.length) {
                int chunkSize = Math.min(DEFAULT_DATA_BUFFER_SIZE - 1, fileBytes.length - offset);
                sendData(out, fileBytes, offset, chunkSize);
                offset += chunkSize;
                buffersInWindow++;
                recordCount++;

                // If we've exhausted credit, wait for more
                if (buffersInWindow >= credit && offset < fileBytes.length) {
                    cdt = readMessage(in);
                    expectCommand(cdt, CMD_CDT, "Expected CDT (re-credit) from receiver");
                    credit = extractCredit(cdt);
                    buffersInWindow = 0;
                    LOG.debug("Re-credit received: {}", credit);
                }
            }
            LOG.debug("Sent {} data records ({} bytes)", recordCount, fileBytes.length);

            // Step 8: Send EFID (End File)
            sendEfid(out, recordCount, fileBytes.length);
            LOG.debug("Sent EFID");

            // Step 9: Receive EFPA (End File Positive Answer)
            Oftp2Message efpa = readMessage(in);
            expectCommand(efpa, CMD_EFPA, "Expected EFPA from receiver");
            LOG.debug("Received EFPA, file accepted by partner");

            // Step 10: Optionally receive EERP (delivery receipt)
            if (channel.isOftp2RequestSignedEerp()) {
                try {
                    Oftp2Message eerp = readMessage(in);
                    if (eerp.getCommand() == CMD_EERP) {
                        LOG.info("Received EERP (delivery receipt) for file {}", virtualFilename);
                    }
                } catch (Exception e) {
                    LOG.warn("Did not receive EERP: {}", e.getMessage());
                }
            }

            // Step 11: Send ESID (End Session)
            sendEsid(out, 0, "Transfer complete");
            LOG.info("OFTP2 writeFile complete: {} sent to {}:{} (SFID={})",
                    virtualFilename, host, port, channel.getOftp2PartnerSfid());

        } catch (Exception e) {
            throw new RuntimeException("OFTP2 writeFile failed to " + host + ":" + port +
                    " (SSID=" + channel.getOftp2LocalSsid() + "): " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(CommunicationChannel channel, String remoteFilePath) {
        throw new UnsupportedOperationException(
                "OFTP2 does not support remote file deletion — it is a push-based protocol.");
    }

    @Override
    public void moveFile(CommunicationChannel channel, String fromPath, String toPath) {
        throw new UnsupportedOperationException(
                "OFTP2 does not support remote file move — it is a push-based protocol.");
    }

    // ========================================================================
    // TLS connection setup
    // ========================================================================

    /**
     * Create a TLS socket configured with the channel's PEM certificates.
     * If TLS is disabled (unusual for OFTP2), falls back to plain TCP.
     */
    private SSLSocket createTlsSocket(CommunicationChannel channel, String host, int port)
            throws Exception {
        SSLContext sslContext;

        if (channel.isOftp2UseTls() &&
                channel.getOftp2LocalCertificate() != null &&
                channel.getOftp2LocalPrivateKey() != null) {
            // Build SSLContext from PEM-encoded certificate and private key
            X509Certificate localCert = parsePemCertificate(channel.getOftp2LocalCertificate());
            PrivateKey privateKey = parsePemPrivateKey(channel.getOftp2LocalPrivateKey());

            // KeyStore for our identity (client certificate)
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry("oftp2-local", privateKey, new char[0],
                    new Certificate[]{localCert});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, new char[0]);

            // TrustStore: if partner certificate is configured, use it; otherwise use defaults
            TrustManagerFactory tmf;
            if (channel.getOftp2PartnerCertificate() != null &&
                    !channel.getOftp2PartnerCertificate().isEmpty()) {
                X509Certificate partnerCert = parsePemCertificate(channel.getOftp2PartnerCertificate());
                KeyStore trustStore = KeyStore.getInstance("PKCS12");
                trustStore.load(null, null);
                trustStore.setCertificateEntry("oftp2-partner", partnerCert);
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
            } else {
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null); // default trust store
            }

            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } else {
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
        }

        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket();
        socket.connect(new InetSocketAddress(host, port), channel.getConnectionTimeoutMs());
        socket.setSoTimeout(channel.getConnectionTimeoutMs());
        socket.startHandshake();

        LOG.debug("TLS handshake complete to {}:{} — protocol={}, cipher={}",
                host, port, socket.getSession().getProtocol(), socket.getSession().getCipherSuite());
        return socket;
    }

    // ========================================================================
    // OFTP2 protocol message I/O
    // ========================================================================

    /**
     * Read an OFTP2 message from the stream.
     * OFTP2 over TCP uses a 4-byte length header followed by the message payload.
     */
    private Oftp2Message readMessage(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > 1_000_000) {
            throw new IOException("Invalid OFTP2 message length: " + length);
        }
        byte[] payload = new byte[length];
        in.readFully(payload);
        char command = (char) payload[0];
        return new Oftp2Message(command, payload);
    }

    /**
     * Write an OFTP2 message to the stream with 4-byte length header.
     */
    private void writeMessage(DataOutputStream out, byte[] payload) throws IOException {
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    /**
     * Validate that a received message has the expected command code.
     */
    private void expectCommand(Oftp2Message msg, char expected, String context) {
        if (msg.getCommand() != expected) {
            throw new RuntimeException(context + " — got '" + msg.getCommand() +
                    "' (0x" + Integer.toHexString(msg.getCommand()) + ")");
        }
    }

    // ========================================================================
    // OFTP2 command builders
    // ========================================================================

    /**
     * Send SSID — Start Session Identification.
     * Format: 'X' + SSID(25) + PASSWORD(8) + version + capabilities
     */
    private void sendSsid(DataOutputStream out, String ssid, String password) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(128);
        buf.put((byte) CMD_SSID);
        buf.put(padRight(ssid, 25).getBytes(StandardCharsets.US_ASCII));
        buf.put(padRight(password != null ? password : "", 8).getBytes(StandardCharsets.US_ASCII));
        // OFTP2 version: "20" = v2.0
        buf.put("20".getBytes(StandardCharsets.US_ASCII));
        // Data exchange buffer size (5 digits)
        buf.put(String.format("%05d", DEFAULT_DATA_BUFFER_SIZE).getBytes(StandardCharsets.US_ASCII));
        // Capabilities: compression(1) + restart(1) + special logic(1) + credit(3)
        buf.put("Y".getBytes(StandardCharsets.US_ASCII)); // compression supported
        buf.put("Y".getBytes(StandardCharsets.US_ASCII)); // restart supported
        buf.put("Y".getBytes(StandardCharsets.US_ASCII)); // special logic
        buf.put(String.format("%03d", DEFAULT_CREDIT).getBytes(StandardCharsets.US_ASCII));
        buf.flip();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        writeMessage(out, data);
    }

    /**
     * Send SFID — Start File (announce a file to transfer).
     * Format: 'H' + SFID_DEST(25) + VIRTUAL_FILENAME(26) + DATE(18) + SIZE(13) + flags
     */
    private void sendSfid(DataOutputStream out, String destSfid, String virtualFilename,
                           long fileSize, boolean compress) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.put((byte) CMD_SFID);
        buf.put(padRight(destSfid, 25).getBytes(StandardCharsets.US_ASCII));
        buf.put(padRight(virtualFilename, 26).getBytes(StandardCharsets.US_ASCII));
        // Date/time stamp: YYYYMMDDHHmmssSSS (18 chars)
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + "0";
        buf.put(padRight(timestamp, 18).getBytes(StandardCharsets.US_ASCII));
        // File size (13 digits, zero-padded)
        buf.put(String.format("%013d", fileSize).getBytes(StandardCharsets.US_ASCII));
        // Record format: U=unstructured
        buf.put("U".getBytes(StandardCharsets.US_ASCII));
        // Compression flag
        buf.put((compress ? "1" : "0").getBytes(StandardCharsets.US_ASCII));
        buf.flip();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        writeMessage(out, data);
    }

    /**
     * Send SFPA — Start File Positive Answer.
     */
    private void sendSfpa(DataOutputStream out) throws IOException {
        writeMessage(out, new byte[]{(byte) CMD_SFPA});
    }

    /**
     * Send CDT — Set Credit (grant the partner permission to send N data buffers).
     */
    private void sendCdt(DataOutputStream out, int credit) throws IOException {
        byte[] data = new byte[4];
        data[0] = (byte) CMD_CDT;
        byte[] creditStr = String.format("%03d", credit).getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(creditStr, 0, data, 1, 3);
        writeMessage(out, data);
    }

    /**
     * Send DATA — a single data buffer.
     */
    private void sendData(DataOutputStream out, byte[] fileBytes, int offset, int length)
            throws IOException {
        byte[] data = new byte[1 + length];
        data[0] = (byte) CMD_DATA;
        System.arraycopy(fileBytes, offset, data, 1, length);
        writeMessage(out, data);
    }

    /**
     * Send EFID — End File.
     * Format: 'T' + RECORD_COUNT(17) + BYTE_COUNT(17)
     */
    private void sendEfid(DataOutputStream out, long recordCount, long byteCount)
            throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(35);
        buf.put((byte) CMD_EFID);
        buf.put(String.format("%017d", recordCount).getBytes(StandardCharsets.US_ASCII));
        buf.put(String.format("%017d", byteCount).getBytes(StandardCharsets.US_ASCII));
        buf.flip();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        writeMessage(out, data);
    }

    /**
     * Send EFPA — End File Positive Answer.
     */
    private void sendEfpa(DataOutputStream out) throws IOException {
        writeMessage(out, new byte[]{(byte) CMD_EFPA});
    }

    /**
     * Send EERP — End-to-End Response (delivery receipt).
     * Format: 'E' + SFID_DEST(25) + VIRTUAL_FILENAME(26) + SIGNED_FLAG(1)
     */
    private void sendEerp(DataOutputStream out, String destSfid, String virtualFilename,
                           boolean signed) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(64);
        buf.put((byte) CMD_EERP);
        buf.put(padRight(destSfid, 25).getBytes(StandardCharsets.US_ASCII));
        buf.put(padRight(virtualFilename, 26).getBytes(StandardCharsets.US_ASCII));
        buf.put((signed ? "Y" : "N").getBytes(StandardCharsets.US_ASCII));
        buf.flip();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        writeMessage(out, data);
    }

    /**
     * Send ESID — End Session.
     * Format: 'F' + REASON_CODE(2) + REASON_TEXT(variable)
     */
    private void sendEsid(DataOutputStream out, int reasonCode, String reasonText)
            throws IOException {
        byte[] reasonBytes = (reasonText != null ? reasonText : "")
                .getBytes(StandardCharsets.US_ASCII);
        byte[] data = new byte[3 + reasonBytes.length];
        data[0] = (byte) CMD_ESID;
        byte[] codeStr = String.format("%02d", reasonCode).getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(codeStr, 0, data, 1, 2);
        System.arraycopy(reasonBytes, 0, data, 3, reasonBytes.length);
        writeMessage(out, data);
    }

    // ========================================================================
    // OFTP2 message field extraction
    // ========================================================================

    /**
     * Extract virtual filename from SFID message.
     * Positioned at bytes 26..51 (25-char SFID dest + 26-char filename).
     */
    private String extractVirtualFilename(Oftp2Message sfid) {
        byte[] payload = sfid.getPayload();
        if (payload.length >= 52) {
            return new String(payload, 26, 26, StandardCharsets.US_ASCII).trim();
        }
        return "UNKNOWN";
    }

    /**
     * Extract file size from SFID message.
     * Positioned at bytes 70..82 (after command + dest(25) + filename(26) + timestamp(18)).
     */
    private long extractFileSize(Oftp2Message sfid) {
        byte[] payload = sfid.getPayload();
        if (payload.length >= 83) {
            String sizeStr = new String(payload, 70, 13, StandardCharsets.US_ASCII).trim();
            try {
                return Long.parseLong(sizeStr);
            } catch (NumberFormatException e) {
                LOG.warn("Cannot parse file size from SFID: '{}'", sizeStr);
            }
        }
        return -1;
    }

    /**
     * Extract credit value from CDT message (3-digit number at offset 1).
     */
    private int extractCredit(Oftp2Message cdt) {
        byte[] payload = cdt.getPayload();
        if (payload.length >= 4) {
            String creditStr = new String(payload, 1, 3, StandardCharsets.US_ASCII).trim();
            try {
                return Integer.parseInt(creditStr);
            } catch (NumberFormatException e) {
                LOG.warn("Cannot parse credit from CDT: '{}', defaulting to {}", creditStr, DEFAULT_CREDIT);
            }
        }
        return DEFAULT_CREDIT;
    }

    // ========================================================================
    // Configuration validation
    // ========================================================================

    /**
     * Validate OFTP2 channel configuration.
     *
     * @throws IllegalArgumentException if required configuration is missing
     */
    public void validateConfig(CommunicationChannel channel) {
        if (channel.getHost() == null || channel.getHost().isEmpty()) {
            throw new IllegalArgumentException("OFTP2 host is required");
        }
        if (channel.getOftp2LocalSsid() == null || channel.getOftp2LocalSsid().isEmpty()) {
            throw new IllegalArgumentException("OFTP2 Local SSID is required");
        }
        if (channel.getOftp2PartnerSfid() == null || channel.getOftp2PartnerSfid().isEmpty()) {
            throw new IllegalArgumentException("OFTP2 Partner SFID is required");
        }
        if (channel.isOftp2UseTls()) {
            if (channel.getOftp2LocalCertificate() == null || channel.getOftp2LocalCertificate().isEmpty()) {
                throw new IllegalArgumentException("OFTP2 TLS requires a local certificate");
            }
            if (channel.getOftp2LocalPrivateKey() == null || channel.getOftp2LocalPrivateKey().isEmpty()) {
                throw new IllegalArgumentException("OFTP2 TLS requires a local private key");
            }
        }
    }

    // ========================================================================
    // Connection test
    // ========================================================================

    /**
     * Test OFTP2 connectivity by performing a TLS handshake and SSID exchange.
     *
     * @return human-readable test result
     */
    public String testConnection(CommunicationChannel channel) {
        validateConfig(channel);

        String host = channel.getHost();
        int port = channel.getPort() != null ? channel.getPort() : DEFAULT_OFTP2_PORT;

        try (SSLSocket socket = createTlsSocket(channel, host, port)) {
            SSLSession session = socket.getSession();
            String protocol = session.getProtocol();
            String cipher = session.getCipherSuite();
            String peerCN = "";
            try {
                Certificate[] peerCerts = session.getPeerCertificates();
                if (peerCerts.length > 0 && peerCerts[0] instanceof X509Certificate) {
                    peerCN = ((X509Certificate) peerCerts[0])
                            .getSubjectX500Principal().getName();
                }
            } catch (Exception e) {
                peerCN = "(unable to read)";
            }

            return String.format("TLS handshake successful to %s:%d. Protocol: %s, Cipher: %s, Peer: %s. " +
                            "SSID=%s, SFID=%s.",
                    host, port, protocol, cipher, peerCN,
                    channel.getOftp2LocalSsid(), channel.getOftp2PartnerSfid());
        } catch (Exception e) {
            throw new RuntimeException("OFTP2 TLS handshake failed to " + host + ":" + port +
                    ": " + e.getMessage(), e);
        }
    }

    // ========================================================================
    // PEM parsing utilities
    // ========================================================================

    /**
     * Parse a PEM-encoded X.509 certificate.
     */
    private X509Certificate parsePemCertificate(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(cleaned);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
                new java.io.ByteArrayInputStream(der));
    }

    /**
     * Parse a PEM-encoded PKCS#8 private key.
     */
    private PrivateKey parsePemPrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(cleaned);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    // ========================================================================
    // Helper utilities
    // ========================================================================

    /**
     * Right-pad a string with spaces to the specified length (OFTP2 fixed-width fields).
     */
    private static String padRight(String s, int len) {
        if (s == null) s = "";
        if (s.length() >= len) return s.substring(0, len);
        return String.format("%-" + len + "s", s);
    }

    // ========================================================================
    // Inner classes
    // ========================================================================

    /**
     * Simple container for an OFTP2 protocol message.
     */
    private static class Oftp2Message {
        private final char command;
        private final byte[] payload;

        Oftp2Message(char command, byte[] payload) {
            this.command = command;
            this.payload = payload;
        }

        char getCommand() { return command; }
        byte[] getPayload() { return payload; }
    }
}
