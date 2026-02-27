package javax.edi.service.controller;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.edi.bind.EDIUnmarshaller;
import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.util.EDIValidationError;
import javax.edi.configuration.EDIMessageConfiguration;
import javax.edi.service.dto.ApiResponse;
import javax.edi.service.dto.ParseRequest;
import javax.edi.service.entity.EDITransactionLog;
import javax.edi.service.registry.EDIModelRegistry;
import javax.edi.service.service.TransactionLoggingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * POST /api/edi/parse
 * 
 * Accepts raw EDI text + transaction type, parses it into a Java object,
 * and returns the result as JSON.
 */
@RestController
@RequestMapping("/api/edi")
@Tag(name = "Parse", description = "Parse raw EDI content into JSON")
public class ParseController {

    private static final Logger LOG = LoggerFactory.getLogger(ParseController.class);

    @Autowired
    private EDIModelRegistry registry;

    @Autowired
    private TransactionLoggingService txnLoggingService;

    @Operation(summary = "Parse raw EDI into JSON",
               description = "Accepts raw EDI text and a transaction type (e.g. '850'), "
                           + "parses it using the JAEB library, and returns the structured "
                           + "JSON representation along with validation results.")
    @PostMapping(value = "/parse", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> parse(@RequestBody ParseRequest request) {

        // --- Validate input ---
        if (request.getEdiContent() == null || request.getEdiContent().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ediContent is required"));
        }
        if (request.getTransactionType() == null || request.getTransactionType().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("transactionType is required (e.g. '850', 'PurchaseOrder')"));
        }

        Class<?> modelClass = registry.resolve(request.getTransactionType());
        if (modelClass == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown transactionType: '" + request.getTransactionType()
                            + "'. Supported types: " + registry.getSupportedTypes().keySet()));
        }

        try {
            StringReader reader = new StringReader(request.getEdiContent());
            EDIUnmarshalResult<?> result;

            // Use custom delimiters if provided
            if (hasCustomDelimiters(request)) {
                char elemDelim = request.getElementDelimiter() != null ? request.getElementDelimiter() : '*';
                char segDelim = request.getSegmentDelimiter() != null ? request.getSegmentDelimiter() : '~';
                char compDelim = request.getComponentDelimiter() != null ? request.getComponentDelimiter() : '>';
                EDIMessageConfiguration config = new EDIMessageConfiguration(elemDelim, compDelim, segDelim);
                result = EDIUnmarshaller.unmarshalResult(modelClass, reader, config);
            } else {
                result = EDIUnmarshaller.unmarshalResult(modelClass, reader);
            }

            // Log to database
            EDITransactionLog txnLog = txnLoggingService.logInboundTransaction(
                    request.getEdiContent(), request.getTransactionType(),
                    result, null, null,
                    EDITransactionLog.Source.API, null);

            // Build metadata
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("transactionId", txnLog.getTransactionId());
            metadata.put("transactionType", request.getTransactionType());
            metadata.put("modelClass", modelClass.getName());
            metadata.put("parsed", result.isParsed());
            metadata.put("valid", result.isValid());
            metadata.put("segmentCount", result.getSegmentCount());
            metadata.put("parseTimeMs", result.getParseTimeMillis());
            if (!result.getSegmentStats().isEmpty()) {
                metadata.put("segmentStats", result.getSegmentStats());
            }

            if (result.getParseError() != null) {
                metadata.put("parseError", result.getParseError());
            }

            // Collect validation errors
            if (result.hasErrors()) {
                List<String> errors = new ArrayList<>();
                for (EDIValidationError err : result.getErrors()) {
                    errors.add(err.toString());
                }
                metadata.put("validationErrors", errors);
            }

            if (!result.isParsed() && result.getParseError() != null) {
                return ResponseEntity.ok(ApiResponse.error("Parse failed: " + result.getParseError()));
            }

            ApiResponse response = ApiResponse.ok(result.getData(), metadata);
            response.setMessage(result.isValid() ? "Parsed successfully" : "Parsed with validation errors");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LOG.error("Parse failed for type {}: {}", request.getTransactionType(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Parse failed: " + e.getMessage()));
        }
    }

    private boolean hasCustomDelimiters(ParseRequest req) {
        return req.getElementDelimiter() != null
            || req.getSegmentDelimiter() != null
            || req.getComponentDelimiter() != null;
    }
}
