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
 * Represents a trading partner / client that exchanges EDI documents.
 * Each partner belongs to a tenant and has communication channel configs,
 * supported transaction types, and delimiter configuration.
 */
@Entity
@Table(name = "trading_partner", indexes = {
    @Index(name = "idx_tp_tenant", columnList = "tenantId")
})
public class TradingPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tenant this partner belongs to. */
    @Column(nullable = false, length = 50)
    private String tenantId;

    @Column(nullable = false, unique = true, length = 50)
    private String partnerId;

    @Column(nullable = false, length = 200)
    private String partnerName;

    /** ISA Sender/Receiver ID for this partner (ISA06 or ISA08). */
    @Column(length = 15)
    private String isaId;

    /** ISA ID Qualifier (ISA05 or ISA07). */
    @Column(length = 2)
    private String isaIdQualifier;

    /** GS Application Code for this partner. */
    @Column(length = 15)
    private String gsApplicationCode;

    /** Comma-separated list of supported transaction types (e.g. "850,810,856"). */
    @Column(length = 200)
    private String supportedTransactionTypes;

    // --- Delimiter configuration ---
    @Column(length = 1)
    private String elementDelimiter;

    @Column(length = 1)
    private String segmentDelimiter;

    @Column(length = 1)
    private String componentDelimiter;

    // --- SFTP configuration ---
    @Column(length = 200)
    private String sftpHost;

    private Integer sftpPort;

    @Column(length = 100)
    private String sftpUsername;

    @Column(length = 200)
    private String sftpPassword;

    /** Remote directory to poll for inbound EDI files. */
    @Column(length = 500)
    private String sftpInboundDir;

    /** Remote directory to drop outbound EDI files (e.g. 997s). */
    @Column(length = 500)
    private String sftpOutboundDir;

    /** Remote directory to move processed files to. */
    @Column(length = 500)
    private String sftpArchiveDir;

    /** Whether to auto-generate 997 for this partner. */
    private boolean autoAcknowledge = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PartnerStatus status = PartnerStatus.ACTIVE;

    @Column(length = 500)
    private String contactEmail;

    @Column(length = 1000)
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum PartnerStatus {
        ACTIVE, INACTIVE, ONBOARDING, SUSPENDED
    }

    // --- Lifecycle ---

    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public String getIsaId() { return isaId; }
    public void setIsaId(String isaId) { this.isaId = isaId; }

    public String getIsaIdQualifier() { return isaIdQualifier; }
    public void setIsaIdQualifier(String isaIdQualifier) { this.isaIdQualifier = isaIdQualifier; }

    public String getGsApplicationCode() { return gsApplicationCode; }
    public void setGsApplicationCode(String gsApplicationCode) { this.gsApplicationCode = gsApplicationCode; }

    public String getSupportedTransactionTypes() { return supportedTransactionTypes; }
    public void setSupportedTransactionTypes(String supportedTransactionTypes) { this.supportedTransactionTypes = supportedTransactionTypes; }

    public String getElementDelimiter() { return elementDelimiter; }
    public void setElementDelimiter(String elementDelimiter) { this.elementDelimiter = elementDelimiter; }

    public String getSegmentDelimiter() { return segmentDelimiter; }
    public void setSegmentDelimiter(String segmentDelimiter) { this.segmentDelimiter = segmentDelimiter; }

    public String getComponentDelimiter() { return componentDelimiter; }
    public void setComponentDelimiter(String componentDelimiter) { this.componentDelimiter = componentDelimiter; }

    public String getSftpHost() { return sftpHost; }
    public void setSftpHost(String sftpHost) { this.sftpHost = sftpHost; }

    public Integer getSftpPort() { return sftpPort; }
    public void setSftpPort(Integer sftpPort) { this.sftpPort = sftpPort; }

    public String getSftpUsername() { return sftpUsername; }
    public void setSftpUsername(String sftpUsername) { this.sftpUsername = sftpUsername; }

    public String getSftpPassword() { return sftpPassword; }
    public void setSftpPassword(String sftpPassword) { this.sftpPassword = sftpPassword; }

    public String getSftpInboundDir() { return sftpInboundDir; }
    public void setSftpInboundDir(String sftpInboundDir) { this.sftpInboundDir = sftpInboundDir; }

    public String getSftpOutboundDir() { return sftpOutboundDir; }
    public void setSftpOutboundDir(String sftpOutboundDir) { this.sftpOutboundDir = sftpOutboundDir; }

    public String getSftpArchiveDir() { return sftpArchiveDir; }
    public void setSftpArchiveDir(String sftpArchiveDir) { this.sftpArchiveDir = sftpArchiveDir; }

    public boolean isAutoAcknowledge() { return autoAcknowledge; }
    public void setAutoAcknowledge(boolean autoAcknowledge) { this.autoAcknowledge = autoAcknowledge; }

    public PartnerStatus getStatus() { return status; }
    public void setStatus(PartnerStatus status) { this.status = status; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
