package javax.edi.service.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.edi.service.dto.ApiResponse;
import javax.edi.service.entity.TradingPartner;
import javax.edi.service.service.TradingPartnerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Trading Partner management API.
 * Full CRUD for onboarding, configuring, and managing trading partners.
 */
@RestController
@RequestMapping("/api/partners")
@Tag(name = "Trading Partners", description = "Onboard and manage trading partners")
public class TradingPartnerController {

    private static final Logger LOG = LoggerFactory.getLogger(TradingPartnerController.class);

    @Autowired
    private TradingPartnerService partnerService;

    @Operation(summary = "List all trading partners")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> listAll() {
        List<TradingPartner> partners = partnerService.getAllPartners();
        return ResponseEntity.ok(ApiResponse.ok(partners,
                partners.size() + " trading partner(s)"));
    }

    @Operation(summary = "List active trading partners")
    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> listActive() {
        List<TradingPartner> partners = partnerService.getActivePartners();
        return ResponseEntity.ok(ApiResponse.ok(partners,
                partners.size() + " active trading partner(s)"));
    }

    @Operation(summary = "Get a trading partner by ID")
    @GetMapping(value = "/{partnerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> getPartner(@PathVariable String partnerId) {
        return partnerService.getPartner(partnerId)
                .map(p -> ResponseEntity.ok(ApiResponse.ok(p)))
                .orElse(ResponseEntity.status(404)
                        .body(ApiResponse.error("Partner not found: " + partnerId)));
    }

    @Operation(summary = "Onboard a new trading partner",
               description = "Creates a new trading partner with EDI configuration, "
                           + "SFTP credentials, and supported transaction types. "
                           + "Partner starts in ONBOARDING status.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> createPartner(@RequestBody TradingPartner partner) {
        if (partner.getPartnerId() == null || partner.getPartnerId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("partnerId is required"));
        }
        if (partner.getPartnerName() == null || partner.getPartnerName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("partnerName is required"));
        }
        try {
            TradingPartner created = partnerService.createPartner(partner);
            return ResponseEntity.status(201)
                    .body(ApiResponse.ok(created, "Trading partner created: " + created.getPartnerId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Update a trading partner",
               description = "Updates the trading partner's configuration. "
                           + "Only non-null fields in the request body are updated.")
    @PutMapping(value = "/{partnerId}", consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> updatePartner(@PathVariable String partnerId,
                                                     @RequestBody TradingPartner updates) {
        try {
            TradingPartner updated = partnerService.updatePartner(partnerId, updates);
            return ResponseEntity.ok(ApiResponse.ok(updated, "Partner updated: " + partnerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Activate a trading partner")
    @PostMapping(value = "/{partnerId}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> activate(@PathVariable String partnerId) {
        try {
            partnerService.activatePartner(partnerId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Partner activated: " + partnerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Suspend a trading partner")
    @PostMapping(value = "/{partnerId}/suspend", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> suspend(@PathVariable String partnerId) {
        try {
            partnerService.suspendPartner(partnerId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Partner suspended: " + partnerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Deactivate a trading partner")
    @PostMapping(value = "/{partnerId}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> deactivate(@PathVariable String partnerId) {
        try {
            partnerService.deactivatePartner(partnerId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Partner deactivated: " + partnerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }
}
