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
import javax.persistence.Table;

/**
 * Portal user. Each user belongs to a tenant.
 * Supports ADMIN (platform-level) and TENANT_ADMIN / OPERATOR / VIEWER roles.
 */
@Entity
@Table(name = "client_user", indexes = {
    @Index(name = "idx_user_tenant", columnList = "tenantId"),
    @Index(name = "idx_user_email", columnList = "email")
})
public class ClientUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 200)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String fullName;

    /** Tenant this user belongs to. Null = platform super-admin. */
    @Column(length = 50)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.OPERATOR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum UserRole {
        /** Platform super-admin — sees all tenants. */
        ADMIN,
        /** Tenant administrator — full access within their tenant. */
        TENANT_ADMIN,
        /** Operator — can view and manage transactions/partners within tenant. */
        OPERATOR,
        /** Read-only viewer. */
        VIEWER
    }

    public enum UserStatus { ACTIVE, DISABLED, LOCKED }

    /** @return true if this is a platform super-admin (no tenant scope). */
    public boolean isSuperAdmin() {
        return role == UserRole.ADMIN;
    }

    /** @return true if this user can manage partners and settings. */
    public boolean canManage() {
        return role == UserRole.ADMIN || role == UserRole.TENANT_ADMIN || role == UserRole.OPERATOR;
    }

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
