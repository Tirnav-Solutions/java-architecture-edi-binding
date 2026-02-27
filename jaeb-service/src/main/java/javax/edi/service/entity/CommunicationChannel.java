package javax.edi.service.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * Communication channel configuration for a trading partner.
 * Each partner can have multiple channels (e.g. SFTP for inbound, AS2 for outbound).
 * Supports SFTP, FTP, AS2, and HTTP protocols.
 */
@Entity
@Table(name = "communication_channel", indexes = {
    @Index(name = "idx_channel_partner", columnList = "partnerId"),
    @Index(name = "idx_channel_tenant", columnList = "tenantId")
})
public class CommunicationChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant scope. */
    @Column(nullable = false, length = 50)
    private String tenantId;

    /** The partner this channel belongs to. */
    @Column(nullable = false, length = 50)
    private String partnerId;

    /** Human-readable label (e.g. "Walmart SFTP Inbound", "Target AS2"). */
    @Column(nullable = false, length = 200)
    private String channelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Protocol protocol;

    /** Whether this is for INBOUND (polling/receiving) or OUTBOUND (delivering). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChannelStatus status = ChannelStatus.ACTIVE;

    // ===== Connection (shared across protocols) =====

    @Column(length = 200)
    private String host;

    private Integer port;

    @Column(length = 100)
    private String username;

    @Column(length = 200)
    private String password;

    // ===== SFTP / FTP specific =====

    /** Remote directory to read from (inbound) or write to (outbound). */
    @Column(length = 500)
    private String remoteDirectory;

    /** Directory to move processed files to. */
    @Column(length = 500)
    private String archiveDirectory;

    /** File pattern to match (e.g. "*.edi", "PO_*.x12"). */
    @Column(length = 100)
    private String filePattern;

    /** Whether to use passive mode (FTP only). */
    private boolean passiveMode;

    /** Whether to use implicit TLS (FTPS). */
    private boolean useTls;

    // ===== AS2 specific =====

    /** Local AS2 identifier. */
    @Column(length = 100)
    private String as2LocalId;

    /** Partner's AS2 identifier. */
    @Column(length = 100)
    private String as2PartnerId;

    /** AS2 endpoint URL for sending. */
    @Column(length = 500)
    private String as2Url;

    /** MDN (Message Disposition Notification) request type: SYNC, ASYNC, NONE. */
    @Column(length = 10)
    private String as2MdnMode;

    /** MIC algorithm: SHA-1, SHA-256, etc. */
    @Column(length = 20)
    private String as2MicAlgorithm;

    /** Encryption algorithm: 3DES, AES128, AES256, NONE. */
    @Column(length = 20)
    private String as2EncryptionAlgorithm;

    /** Whether to sign outbound AS2 messages. */
    private boolean as2SignMessages;

    /** Whether to encrypt outbound AS2 messages. */
    private boolean as2EncryptMessages;

    /** Whether to request signed MDN. */
    private boolean as2RequestSignedMdn;

    /** PEM-encoded local certificate for signing (AS2). */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String as2LocalCertificate;

    /** PEM-encoded local private key for decryption and MDN signing (AS2). */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String as2LocalPrivateKey;

    /** PEM-encoded partner certificate for encryption and signature verification (AS2). */
    @Lob
    @Column(columnDefinition = "CLOB")
    private String as2PartnerCertificate;

    // ===== HTTP specific =====

    /** HTTP endpoint URL (webhook-style). */
    @Column(length = 500)
    private String httpUrl;

    /** HTTP method: POST, PUT. */
    @Column(length = 10)
    private String httpMethod;

    /** HTTP Content-Type header. */
    @Column(length = 100)
    private String httpContentType;

    /** Authorization header value (e.g. "Bearer xxx" or "Basic xxx"). */
    @Column(length = 500)
    private String httpAuthHeader;

    /** Custom headers as JSON: {"X-Custom": "value"}. */
    @Column(length = 1000)
    private String httpCustomHeaders;

    // ===== Scheduling =====

    /** Poll interval in milliseconds (for inbound channels). 0 = manual only. */
    private long pollIntervalMs;

    /** Connection timeout in milliseconds. */
    private int connectionTimeoutMs = 10000;

    // ===== Audit =====

    private LocalDateTime lastPolledAt;
    private LocalDateTime lastSuccessAt;
    private LocalDateTime lastErrorAt;

    @Column(length = 500)
    private String lastErrorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== Enums =====

    public enum Protocol { SFTP, FTP, AS2, HTTP }
    public enum Direction { INBOUND, OUTBOUND }
    public enum ChannelStatus { ACTIVE, DISABLED, ERROR }

    // ===== Getters / Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public Protocol getProtocol() { return protocol; }
    public void setProtocol(Protocol protocol) { this.protocol = protocol; }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }

    public ChannelStatus getStatus() { return status; }
    public void setStatus(ChannelStatus status) { this.status = status; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRemoteDirectory() { return remoteDirectory; }
    public void setRemoteDirectory(String remoteDirectory) { this.remoteDirectory = remoteDirectory; }

    public String getArchiveDirectory() { return archiveDirectory; }
    public void setArchiveDirectory(String archiveDirectory) { this.archiveDirectory = archiveDirectory; }

    public String getFilePattern() { return filePattern; }
    public void setFilePattern(String filePattern) { this.filePattern = filePattern; }

    public boolean isPassiveMode() { return passiveMode; }
    public void setPassiveMode(boolean passiveMode) { this.passiveMode = passiveMode; }

    public boolean isUseTls() { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }

    public String getAs2LocalId() { return as2LocalId; }
    public void setAs2LocalId(String as2LocalId) { this.as2LocalId = as2LocalId; }

    public String getAs2PartnerId() { return as2PartnerId; }
    public void setAs2PartnerId(String as2PartnerId) { this.as2PartnerId = as2PartnerId; }

    public String getAs2Url() { return as2Url; }
    public void setAs2Url(String as2Url) { this.as2Url = as2Url; }

    public String getAs2MdnMode() { return as2MdnMode; }
    public void setAs2MdnMode(String as2MdnMode) { this.as2MdnMode = as2MdnMode; }

    public String getAs2MicAlgorithm() { return as2MicAlgorithm; }
    public void setAs2MicAlgorithm(String as2MicAlgorithm) { this.as2MicAlgorithm = as2MicAlgorithm; }

    public String getAs2EncryptionAlgorithm() { return as2EncryptionAlgorithm; }
    public void setAs2EncryptionAlgorithm(String a) { this.as2EncryptionAlgorithm = a; }

    public boolean isAs2SignMessages() { return as2SignMessages; }
    public void setAs2SignMessages(boolean v) { this.as2SignMessages = v; }

    public boolean isAs2EncryptMessages() { return as2EncryptMessages; }
    public void setAs2EncryptMessages(boolean v) { this.as2EncryptMessages = v; }

    public boolean isAs2RequestSignedMdn() { return as2RequestSignedMdn; }
    public void setAs2RequestSignedMdn(boolean v) { this.as2RequestSignedMdn = v; }

    public String getAs2LocalCertificate() { return as2LocalCertificate; }
    public void setAs2LocalCertificate(String v) { this.as2LocalCertificate = v; }

    public String getAs2LocalPrivateKey() { return as2LocalPrivateKey; }
    public void setAs2LocalPrivateKey(String v) { this.as2LocalPrivateKey = v; }

    public String getAs2PartnerCertificate() { return as2PartnerCertificate; }
    public void setAs2PartnerCertificate(String v) { this.as2PartnerCertificate = v; }

    public String getHttpUrl() { return httpUrl; }
    public void setHttpUrl(String httpUrl) { this.httpUrl = httpUrl; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getHttpContentType() { return httpContentType; }
    public void setHttpContentType(String v) { this.httpContentType = v; }

    public String getHttpAuthHeader() { return httpAuthHeader; }
    public void setHttpAuthHeader(String v) { this.httpAuthHeader = v; }

    public String getHttpCustomHeaders() { return httpCustomHeaders; }
    public void setHttpCustomHeaders(String v) { this.httpCustomHeaders = v; }

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(int v) { this.connectionTimeoutMs = v; }

    public LocalDateTime getLastPolledAt() { return lastPolledAt; }
    public void setLastPolledAt(LocalDateTime v) { this.lastPolledAt = v; }

    public LocalDateTime getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(LocalDateTime v) { this.lastSuccessAt = v; }

    public LocalDateTime getLastErrorAt() { return lastErrorAt; }
    public void setLastErrorAt(LocalDateTime v) { this.lastErrorAt = v; }

    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String v) { this.lastErrorMessage = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
