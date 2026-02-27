package javax.edi.service.controller;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.edi.bind.EDIMarshaller;
import javax.edi.bind.EDIMarshalResult;
import javax.edi.configuration.EDIMessageConfiguration;
import javax.edi.service.dto.ApiResponse;
import javax.edi.service.dto.GenerateRequest;
import javax.edi.service.registry.EDIModelRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * POST /api/edi/generate
 * 
 * Accepts a JSON payload + transaction type, converts it to an EDI message
 * object, marshals it into raw EDI text, and returns the result.
 */
@RestController
@RequestMapping("/api/edi")
@Tag(name = "Generate", description = "Generate raw EDI from JSON payload")
public class GenerateController {

    private static final Logger LOG = LoggerFactory.getLogger(GenerateController.class);

    @Autowired
    private EDIModelRegistry registry;

    @Autowired
    private ObjectMapper objectMapper;

    @Operation(summary = "Generate EDI from JSON",
               description = "Accepts a JSON payload representing an EDI message, "
                           + "validates it, and generates the raw EDI string output.")
    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> generate(@RequestBody GenerateRequest request) {

        // --- Validate input ---
        if (request.getTransactionType() == null || request.getTransactionType().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("transactionType is required (e.g. '850', 'PurchaseOrder')"));
        }
        if (request.getPayload() == null || request.getPayload().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("payload is required (JSON object representing the EDI message)"));
        }

        Class<?> modelClass = registry.resolve(request.getTransactionType());
        if (modelClass == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown transactionType: '" + request.getTransactionType()
                            + "'. Supported types: " + registry.getSupportedTypes().keySet()));
        }

        try {
            // Deserialize JSON payload into the model class
            Object ediObject = objectMapper.convertValue(request.getPayload(), modelClass);

            // Marshal with or without custom delimiters
            EDIMarshalResult result;
            if (hasCustomDelimiters(request)) {
                char elemDelim = request.getElementDelimiter() != null ? request.getElementDelimiter() : '*';
                char segDelim = request.getSegmentDelimiter() != null ? request.getSegmentDelimiter() : '~';
                char compDelim = request.getComponentDelimiter() != null ? request.getComponentDelimiter() : '>';
                EDIMessageConfiguration config = new EDIMessageConfiguration(elemDelim, compDelim, segDelim);
                result = EDIMarshaller.marshalResult(ediObject, config);
            } else {
                result = EDIMarshaller.marshalResult(ediObject);
            }

            // Build metadata
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("transactionType", request.getTransactionType());
            metadata.put("modelClass", modelClass.getName());
            metadata.put("success", result.isSuccess());
            metadata.put("segmentCount", result.getSegmentCount());

            if (result.isSuccess()) {
                // Return raw EDI in data
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("ediOutput", result.getEdiOutput());
                data.put("segmentCount", result.getSegmentCount());

                ApiResponse response = ApiResponse.ok(data, metadata);
                response.setMessage("EDI generated successfully");
                return ResponseEntity.ok(response);
            } else {
                // Return errors
                String errSummary = result.getErrorSummary();
                return ResponseEntity.ok(ApiResponse.error("Generation failed: " + errSummary));
            }

        } catch (IllegalArgumentException e) {
            LOG.error("JSON deserialization failed for type {}: {}", request.getTransactionType(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid payload for type " + request.getTransactionType()
                            + ": " + e.getMessage()));
        } catch (Exception e) {
            LOG.error("Generate failed for type {}: {}", request.getTransactionType(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Generate failed: " + e.getMessage()));
        }
    }

    private boolean hasCustomDelimiters(GenerateRequest req) {
        return req.getElementDelimiter() != null
            || req.getSegmentDelimiter() != null
            || req.getComponentDelimiter() != null;
    }
}
