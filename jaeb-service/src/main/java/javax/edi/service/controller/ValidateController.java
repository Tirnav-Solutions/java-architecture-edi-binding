package javax.edi.service.controller;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.edi.bind.EDIUnmarshaller;
import javax.edi.bind.EDIUnmarshalResult;
import javax.edi.bind.util.EDIValidationError;
import javax.edi.bind.util.EDIValidationUtil;
import javax.edi.configuration.EDIMessageConfiguration;
import javax.edi.service.dto.ApiResponse;
import javax.edi.service.dto.ValidateRequest;
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
 * POST /api/edi/validate
 * 
 * Validates either raw EDI content or a JSON payload against
 * the model's annotation-based constraints (@NotNull, @Size, etc.).
 */
@RestController
@RequestMapping("/api/edi")
@Tag(name = "Validate", description = "Validate EDI content or JSON payload")
public class ValidateController {

    private static final Logger LOG = LoggerFactory.getLogger(ValidateController.class);

    @Autowired
    private EDIModelRegistry registry;

    @Autowired
    private ObjectMapper objectMapper;

    @Operation(summary = "Validate EDI or JSON",
               description = "Validates raw EDI text or a JSON payload against the "
                           + "model's validation annotations. Returns a list of all "
                           + "validation errors found, or confirms the input is valid.")
    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> validate(@RequestBody ValidateRequest request) {

        // --- Validate input ---
        if (request.getTransactionType() == null || request.getTransactionType().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("transactionType is required"));
        }
        if ((request.getEdiContent() == null || request.getEdiContent().trim().isEmpty())
                && (request.getPayload() == null || request.getPayload().isEmpty())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Either ediContent or payload is required"));
        }

        Class<?> modelClass = registry.resolve(request.getTransactionType());
        if (modelClass == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Unknown transactionType: '" + request.getTransactionType()
                            + "'. Supported types: " + registry.getSupportedTypes().keySet()));
        }

        try {
            Map<String, Object> data = new LinkedHashMap<>();
            List<String> errorList = new ArrayList<>();

            if (request.getEdiContent() != null && !request.getEdiContent().trim().isEmpty()) {
                // --- Validate raw EDI (parse + validate) ---
                data.put("inputType", "EDI");
                StringReader reader = new StringReader(request.getEdiContent());

                EDIUnmarshalResult<?> result;
                if (hasCustomDelimiters(request)) {
                    char elemDelim = request.getElementDelimiter() != null ? request.getElementDelimiter() : '*';
                    char segDelim = request.getSegmentDelimiter() != null ? request.getSegmentDelimiter() : '~';
                    char compDelim = request.getComponentDelimiter() != null ? request.getComponentDelimiter() : '>';
                    EDIMessageConfiguration config = new EDIMessageConfiguration(elemDelim, compDelim, segDelim);
                    result = EDIUnmarshaller.unmarshalResult(modelClass, reader, config);
                } else {
                    result = EDIUnmarshaller.unmarshalResult(modelClass, reader);
                }

                data.put("parsed", result.isParsed());
                data.put("segmentCount", result.getSegmentCount());

                if (result.getParseError() != null) {
                    errorList.add("PARSE ERROR: " + result.getParseError());
                }
                if (result.hasErrors()) {
                    for (EDIValidationError err : result.getErrors()) {
                        errorList.add(err.toString());
                    }
                }

            } else {
                // --- Validate JSON payload (deserialize + validate) ---
                data.put("inputType", "JSON");

                Object ediObject = objectMapper.convertValue(request.getPayload(), modelClass);

                EDIValidationError.ValidationResult valResult =
                        EDIValidationUtil.validateDeepAndCollectErrors(ediObject);

                if (valResult.hasErrors()) {
                    for (EDIValidationError err : valResult.getErrors()) {
                        errorList.add(err.toString());
                    }
                }
            }

            boolean valid = errorList.isEmpty();
            data.put("valid", valid);
            data.put("errorCount", errorList.size());
            data.put("errors", errorList);

            ApiResponse response = ApiResponse.ok(data);
            response.setMessage(valid ? "Validation passed - no errors found"
                                      : "Validation failed - " + errorList.size() + " error(s) found");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            LOG.error("Validation deserialization failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid payload for type " + request.getTransactionType()
                            + ": " + e.getMessage()));
        } catch (Exception e) {
            LOG.error("Validation failed for type {}: {}", request.getTransactionType(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        }
    }

    private boolean hasCustomDelimiters(ValidateRequest req) {
        return req.getElementDelimiter() != null
            || req.getSegmentDelimiter() != null
            || req.getComponentDelimiter() != null;
    }
}
