package javax.edi.service.channel;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.edi.service.entity.CommunicationChannel;
import javax.edi.service.repository.CommunicationChannelRepository;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Tests connectivity for communication channels.
 * 
 * <ul>
 *   <li><b>SFTP</b> — connects, authenticates, lists remote directory</li>
 *   <li><b>FTP</b>  — connects, authenticates, lists remote directory</li>
 *   <li><b>AS2</b>  — validates certificates are parseable + key-pair matches</li>
 *   <li><b>HTTP</b> — sends a HEAD request to the endpoint URL</li>
 * </ul>
 */
@Service
public class ChannelTestService {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelTestService.class);

    @Autowired
    private CommunicationChannelRepository channelRepo;

    @Autowired(required = false)
    private As2CryptoService as2CryptoService;

    @Autowired(required = false)
    private Oftp2ChannelAdapter oftp2Adapter;

    /**
     * Test a channel's connectivity and return a result map.
     */
    public TestResult test(CommunicationChannel channel) {
        long start = System.currentTimeMillis();
        TestResult result;
        try {
            switch (channel.getProtocol()) {
                case SFTP: result = testSftp(channel, start); break;
                case FTP:  result = testFtp(channel, start); break;
                case AS2:  result = testAs2(channel, start); break;
                case HTTP: result = testHttp(channel, start); break;
                case OFTP2: result = testOftp2(channel, start); break;
                default:   result = TestResult.failure("Unsupported protocol: " + channel.getProtocol(),
                                   System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            LOG.error("Channel test failed for {}: {}", channel.getChannelName(), e.getMessage(), e);
            result = TestResult.failure("Connection failed: " + e.getMessage(),
                    System.currentTimeMillis() - start);
        }

        // Record error on channel if test failed
        if (!result.isSuccess()) {
            updateLastError(channel, result.getMessage());
        }
        return result;
    }

    // ======================== SFTP ========================

    private TestResult testSftp(CommunicationChannel channel, long start) throws Exception {
        if (channel.getHost() == null || channel.getHost().isEmpty()) {
            return TestResult.failure("SFTP host is not configured", 0);
        }

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftp = null;
        try {
            int port = channel.getPort() != null ? channel.getPort() : 22;
            session = jsch.getSession(channel.getUsername(), channel.getHost(), port);
            session.setPassword(channel.getPassword());

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setServerAliveInterval(5000);
            int timeout = channel.getConnectionTimeoutMs() > 0 ? channel.getConnectionTimeoutMs() : 10000;
            session.setTimeout(timeout);
            session.connect(timeout);

            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(timeout);

            // List remote directory
            String dir = channel.getRemoteDirectory();
            if (dir == null || dir.isEmpty()) dir = "/";
            Vector<?> files = sftp.ls(dir);
            int fileCount = files != null ? files.size() : 0;

            long elapsed = System.currentTimeMillis() - start;
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("host", channel.getHost() + ":" + port);
            details.put("directory", dir);
            details.put("filesFound", fileCount);
            details.put("serverVersion", session.getServerVersion());

            updateLastSuccess(channel);
            return TestResult.success("SFTP connection successful. Found " + fileCount + " entries in " + dir,
                    elapsed, details);

        } finally {
            if (sftp != null && sftp.isConnected()) sftp.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    // ======================== FTP ========================

    private TestResult testFtp(CommunicationChannel channel, long start) throws Exception {
        if (channel.getHost() == null || channel.getHost().isEmpty()) {
            return TestResult.failure("FTP host is not configured", 0);
        }

        FTPClient ftp = channel.isUseTls() ? new FTPSClient() : new FTPClient();
        try {
            int port = channel.getPort() != null ? channel.getPort() : 21;
            int timeout = channel.getConnectionTimeoutMs() > 0 ? channel.getConnectionTimeoutMs() : 10000;
            ftp.setConnectTimeout(timeout);
            ftp.connect(channel.getHost(), port);

            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                return TestResult.failure("FTP server rejected connection: " + reply,
                        System.currentTimeMillis() - start);
            }

            boolean loggedIn = ftp.login(channel.getUsername(), channel.getPassword());
            if (!loggedIn) {
                return TestResult.failure("FTP login failed (bad username/password)",
                        System.currentTimeMillis() - start);
            }

            if (channel.isPassiveMode()) {
                ftp.enterLocalPassiveMode();
            }

            String dir = channel.getRemoteDirectory();
            if (dir == null || dir.isEmpty()) dir = "/";
            String[] files = ftp.listNames(dir);
            int fileCount = files != null ? files.length : 0;

            long elapsed = System.currentTimeMillis() - start;
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("host", channel.getHost() + ":" + port);
            details.put("directory", dir);
            details.put("filesFound", fileCount);
            details.put("systemType", ftp.getSystemType());

            updateLastSuccess(channel);
            return TestResult.success("FTP connection successful. Found " + fileCount + " files in " + dir,
                    elapsed, details);

        } finally {
            if (ftp.isConnected()) {
                try { ftp.disconnect(); } catch (Exception ignore) {}
            }
        }
    }

    // ======================== AS2 ========================

    private TestResult testAs2(CommunicationChannel channel, long start) {
        Map<String, Object> details = new LinkedHashMap<>();
        int checks = 0;
        int passed = 0;

        // Check local certificate
        if (channel.getAs2LocalCertificate() != null && !channel.getAs2LocalCertificate().trim().isEmpty()) {
            checks++;
            try {
                X509Certificate cert = as2CryptoService.loadCertificate(channel.getAs2LocalCertificate());
                details.put("localCertSubject", cert.getSubjectX500Principal().getName());
                details.put("localCertExpiry", cert.getNotAfter().toString());

                // Check if expired
                cert.checkValidity();
                details.put("localCertValid", true);
                passed++;
            } catch (Exception e) {
                details.put("localCertValid", false);
                details.put("localCertError", e.getMessage());
            }
        } else {
            details.put("localCertificate", "Not configured");
        }

        // Check private key
        if (channel.getAs2LocalPrivateKey() != null && !channel.getAs2LocalPrivateKey().trim().isEmpty()) {
            checks++;
            try {
                as2CryptoService.loadPrivateKey(channel.getAs2LocalPrivateKey());
                details.put("privateKeyValid", true);
                passed++;
            } catch (Exception e) {
                details.put("privateKeyValid", false);
                details.put("privateKeyError", e.getMessage());
            }
        } else {
            details.put("privateKey", "Not configured");
        }

        // Check partner certificate
        if (channel.getAs2PartnerCertificate() != null && !channel.getAs2PartnerCertificate().trim().isEmpty()) {
            checks++;
            try {
                X509Certificate cert = as2CryptoService.loadCertificate(channel.getAs2PartnerCertificate());
                details.put("partnerCertSubject", cert.getSubjectX500Principal().getName());
                details.put("partnerCertExpiry", cert.getNotAfter().toString());
                cert.checkValidity();
                details.put("partnerCertValid", true);
                passed++;
            } catch (Exception e) {
                details.put("partnerCertValid", false);
                details.put("partnerCertError", e.getMessage());
            }
        } else {
            details.put("partnerCertificate", "Not configured");
        }

        // Validate key pair match
        if (channel.getAs2LocalCertificate() != null && channel.getAs2LocalPrivateKey() != null
                && !channel.getAs2LocalCertificate().trim().isEmpty()
                && !channel.getAs2LocalPrivateKey().trim().isEmpty()) {
            checks++;
            boolean match = as2CryptoService.validateKeyPair(
                    channel.getAs2LocalCertificate(), channel.getAs2LocalPrivateKey());
            details.put("keyPairMatch", match);
            if (match) passed++;
        }

        // Check AS2 IDs
        details.put("localAs2Id", channel.getAs2LocalId() != null ? channel.getAs2LocalId() : "Not configured");
        details.put("partnerAs2Id", channel.getAs2PartnerId() != null ? channel.getAs2PartnerId() : "Not configured");
        details.put("as2Url", channel.getAs2Url() != null ? channel.getAs2Url() : "Not configured");

        long elapsed = System.currentTimeMillis() - start;

        if (checks == 0) {
            return TestResult.failure("No AS2 certificates or keys configured", elapsed, details);
        } else if (passed == checks) {
            updateLastSuccess(channel);
            return TestResult.success("AS2 configuration valid. " + passed + "/" + checks + " checks passed.",
                    elapsed, details);
        } else {
            return TestResult.failure("AS2 validation: " + passed + "/" + checks + " checks passed.",
                    elapsed, details);
        }
    }

    // ======================== HTTP ========================

    private TestResult testHttp(CommunicationChannel channel, long start) throws Exception {
        String urlStr = channel.getHttpUrl();
        if (urlStr == null || urlStr.isEmpty()) {
            return TestResult.failure("HTTP URL is not configured", 0);
        }

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("HEAD");
            int timeout = channel.getConnectionTimeoutMs() > 0 ? channel.getConnectionTimeoutMs() : 10000;
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);

            if (channel.getHttpAuthHeader() != null && !channel.getHttpAuthHeader().isEmpty()) {
                conn.setRequestProperty("Authorization", channel.getHttpAuthHeader());
            }

            int status = conn.getResponseCode();
            long elapsed = System.currentTimeMillis() - start;

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("url", urlStr);
            details.put("httpStatus", status);
            details.put("contentType", conn.getContentType());

            if (status >= 200 && status < 400) {
                updateLastSuccess(channel);
                return TestResult.success("HTTP endpoint reachable. Status: " + status, elapsed, details);
            } else {
                return TestResult.failure("HTTP endpoint returned status " + status, elapsed, details);
            }

        } finally {
            conn.disconnect();
        }
    }

    // ======================== OFTP2 ========================

    private TestResult testOftp2(CommunicationChannel channel, long start) {
        if (oftp2Adapter == null) {
            return TestResult.failure("OFTP2 adapter is not available", 0);
        }
        try {
            String result = oftp2Adapter.testConnection(channel);
            long elapsed = System.currentTimeMillis() - start;

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("host", channel.getHost());
            details.put("port", channel.getPort() != null ? channel.getPort() : Oftp2ChannelAdapter.DEFAULT_OFTP2_PORT);
            details.put("localSsid", channel.getOftp2LocalSsid());
            details.put("partnerSfid", channel.getOftp2PartnerSfid());
            details.put("tls", channel.isOftp2UseTls());
            details.put("compression", channel.isOftp2Compress());
            details.put("signedEerp", channel.isOftp2RequestSignedEerp());

            updateLastSuccess(channel);
            return TestResult.success(result, elapsed, details);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return TestResult.failure("OFTP2 test failed: " + e.getMessage(), elapsed);
        }
    }

    // ======================== Helpers ========================

    private void updateLastSuccess(CommunicationChannel channel) {
        try {
            channel.setLastSuccessAt(LocalDateTime.now());
            channel.setUpdatedAt(LocalDateTime.now());
            channel.setLastErrorMessage(null);
            channelRepo.save(channel);
        } catch (Exception e) {
            LOG.warn("Failed to update channel last success: {}", e.getMessage());
        }
    }

    private void updateLastError(CommunicationChannel channel, String errorMessage) {
        try {
            channel.setLastErrorAt(LocalDateTime.now());
            channel.setLastErrorMessage(errorMessage);
            channel.setUpdatedAt(LocalDateTime.now());
            channelRepo.save(channel);
        } catch (Exception e) {
            LOG.warn("Failed to update channel last error: {}", e.getMessage());
        }
    }

    // ======================== Result DTO ========================

    public static class TestResult {
        private final boolean success;
        private final String message;
        private final long elapsedMs;
        private final Map<String, Object> details;

        private TestResult(boolean success, String message, long elapsedMs, Map<String, Object> details) {
            this.success = success;
            this.message = message;
            this.elapsedMs = elapsedMs;
            this.details = details != null ? details : new LinkedHashMap<>();
        }

        public static TestResult success(String message, long elapsedMs) {
            return new TestResult(true, message, elapsedMs, null);
        }

        public static TestResult success(String message, long elapsedMs, Map<String, Object> details) {
            return new TestResult(true, message, elapsedMs, details);
        }

        public static TestResult failure(String message, long elapsedMs) {
            return new TestResult(false, message, elapsedMs, null);
        }

        public static TestResult failure(String message, long elapsedMs, Map<String, Object> details) {
            return new TestResult(false, message, elapsedMs, details);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getElapsedMs() { return elapsedMs; }
        public Map<String, Object> getDetails() { return details; }
    }
}
