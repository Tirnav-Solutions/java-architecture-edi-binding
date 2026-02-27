package javax.edi.service.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Represents an organization / tenant in the SaaS platform.
 * All data (partners, transactions, users) is scoped to a tenant.
 */
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique short code (URL-safe). e.g. "acme", "walmart-edi" */
    @Column(nullable = false, unique = true, length = 50)
    private String tenantId;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantPlan plan = TenantPlan.STARTER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;

    /** Max partners allowed on this plan. */
    private int maxPartners = 5;

    /** Max transactions per month on this plan. */
    private int maxTransactionsPerMonth = 1000;

    @Column(length = 500)
    private String contactEmail;

    @Column(length = 100)
    private String contactPhone;

    @Column(length = 1000)
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum TenantPlan { STARTER, PROFESSIONAL, ENTERPRISE }
    public enum TenantStatus { ACTIVE, SUSPENDED, CANCELLED, TRIAL }

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public TenantPlan getPlan() { return plan; }
    public void setPlan(TenantPlan plan) { this.plan = plan; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public int getMaxPartners() { return maxPartners; }
    public void setMaxPartners(int maxPartners) { this.maxPartners = maxPartners; }

    public int getMaxTransactionsPerMonth() { return maxTransactionsPerMonth; }
    public void setMaxTransactionsPerMonth(int maxTransactionsPerMonth) { this.maxTransactionsPerMonth = maxTransactionsPerMonth; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
