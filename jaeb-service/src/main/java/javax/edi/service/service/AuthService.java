package javax.edi.service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.edi.service.entity.ClientUser;
import javax.edi.service.entity.ClientUser.UserRole;
import javax.edi.service.entity.ClientUser.UserStatus;
import javax.edi.service.entity.Tenant;
import javax.edi.service.entity.Tenant.TenantPlan;
import javax.edi.service.entity.Tenant.TenantStatus;
import javax.edi.service.repository.ClientUserRepository;
import javax.edi.service.repository.TenantRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Authentication and user management service.
 * Uses SHA-256 hashing (no external security dependency needed).
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private ClientUserRepository userRepo;

    @Autowired
    private TenantRepository tenantRepo;

    /**
     * Authenticate a user by email and password.
     * @return the authenticated user, or empty if credentials are invalid.
     */
    public Optional<ClientUser> authenticate(String email, String password) {
        if (email == null || password == null) return Optional.empty();

        Optional<ClientUser> userOpt = userRepo.findByEmail(email.trim().toLowerCase());
        if (!userOpt.isPresent()) {
            LOG.warn("Login failed: user not found — {}", email);
            return Optional.empty();
        }

        ClientUser user = userOpt.get();

        if (user.getStatus() != UserStatus.ACTIVE) {
            LOG.warn("Login failed: user disabled — {}", email);
            return Optional.empty();
        }

        if (!hashPassword(password).equals(user.getPasswordHash())) {
            LOG.warn("Login failed: bad password — {}", email);
            return Optional.empty();
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepo.save(user);

        LOG.info("Login successful: {} (tenant={})", email, user.getTenantId());
        return Optional.of(user);
    }

    /**
     * Register a new tenant and its admin user in one call.
     */
    public ClientUser registerTenant(String tenantId, String companyName,
                                     String adminEmail, String adminName,
                                     String password) {
        if (tenantRepo.existsByTenantId(tenantId)) {
            throw new IllegalArgumentException("Tenant ID already exists: " + tenantId);
        }
        if (userRepo.existsByEmail(adminEmail.toLowerCase())) {
            throw new IllegalArgumentException("Email already registered: " + adminEmail);
        }

        // Create tenant
        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setCompanyName(companyName);
        tenant.setPlan(TenantPlan.STARTER);
        tenant.setStatus(TenantStatus.TRIAL);
        tenant.setContactEmail(adminEmail);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepo.save(tenant);

        // Create admin user
        ClientUser admin = new ClientUser();
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setFullName(adminName);
        admin.setPasswordHash(hashPassword(password));
        admin.setTenantId(tenantId);
        admin.setRole(UserRole.TENANT_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userRepo.save(admin);

        LOG.info("Tenant registered: {} ({}) with admin {}", tenantId, companyName, adminEmail);
        return admin;
    }

    /**
     * Create a user within a tenant (called by a TENANT_ADMIN).
     */
    public ClientUser createUser(String tenantId, String email, String fullName,
                                 String password, UserRole role) {
        if (userRepo.existsByEmail(email.toLowerCase())) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        ClientUser user = new ClientUser();
        user.setEmail(email.trim().toLowerCase());
        user.setFullName(fullName);
        user.setPasswordHash(hashPassword(password));
        user.setTenantId(tenantId);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepo.save(user);
    }

    /**
     * Ensures a default super-admin exists (for first-time setup).
     */
    public void ensureDefaultAdmin() {
        if (!userRepo.existsByEmail("admin@jaeb.local")) {
            // Create default tenant
            if (!tenantRepo.existsByTenantId("system")) {
                Tenant sys = new Tenant();
                sys.setTenantId("system");
                sys.setCompanyName("JAEB Platform");
                sys.setPlan(TenantPlan.ENTERPRISE);
                sys.setStatus(TenantStatus.ACTIVE);
                sys.setMaxPartners(999);
                sys.setMaxTransactionsPerMonth(999999);
                sys.setCreatedAt(LocalDateTime.now());
                sys.setUpdatedAt(LocalDateTime.now());
                tenantRepo.save(sys);
            }

            ClientUser admin = new ClientUser();
            admin.setEmail("admin@jaeb.local");
            admin.setFullName("Platform Admin");
            admin.setPasswordHash(hashPassword("admin"));
            admin.setTenantId("system");
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            userRepo.save(admin);
            LOG.info("Default super-admin created: admin@jaeb.local / admin");
        }
    }

    /** Simple SHA-256 hash (no salt — suitable for demo; use BCrypt in production). */
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash failed", e);
        }
    }
}
