package javax.edi.service.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.edi.service.dto.ApiResponse;
import javax.edi.service.dto.CompareRequest;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * POST /api/edi/compare
 * 
 * Compares two EDI documents segment by segment and reports differences.
 * Useful for regression testing, partner onboarding, and troubleshooting.
 */
@RestController
@RequestMapping("/api/edi")
@Tag(name = "Compare", description = "Compare two EDI documents segment-by-segment")
public class CompareController {

    private static final Logger LOG = LoggerFactory.getLogger(CompareController.class);

    @Autowired
    private EDIModelRegistry registry;

    @Operation(summary = "Compare two EDI documents",
               description = "Splits both EDI documents into segments and compares them "
                           + "line by line. Reports added, removed, and modified segments. "
                           + "Useful for regression testing and troubleshooting.")
    @PostMapping(value = "/compare", consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse> compare(@RequestBody CompareRequest request) {

        if (request.getEdiContentA() == null || request.getEdiContentA().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ediContentA is required"));
        }
        if (request.getEdiContentB() == null || request.getEdiContentB().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ediContentB is required"));
        }

        try {
            char segDelim = request.getSegmentDelimiter() != null ? request.getSegmentDelimiter() : '~';
            char elemDelim = request.getElementDelimiter() != null ? request.getElementDelimiter() : '*';

            String[] segmentsA = splitSegments(request.getEdiContentA(), segDelim);
            String[] segmentsB = splitSegments(request.getEdiContentB(), segDelim);

            List<Map<String, Object>> differences = new ArrayList<>();
            int maxLen = Math.max(segmentsA.length, segmentsB.length);
            int matchCount = 0;
            int modifiedCount = 0;
            int addedCount = 0;
            int removedCount = 0;

            for (int i = 0; i < maxLen; i++) {
                String segA = i < segmentsA.length ? segmentsA[i].trim() : null;
                String segB = i < segmentsB.length ? segmentsB[i].trim() : null;

                if (segA != null && segA.isEmpty()) segA = null;
                if (segB != null && segB.isEmpty()) segB = null;

                if (segA == null && segB == null) continue;

                Map<String, Object> diff = new LinkedHashMap<>();
                diff.put("position", i + 1);

                if (segA == null) {
                    // Added in B
                    diff.put("status", "ADDED");
                    diff.put("segmentTag", extractTag(segB, elemDelim));
                    diff.put(request.getLabelA(), null);
                    diff.put(request.getLabelB(), segB);
                    differences.add(diff);
                    addedCount++;
                } else if (segB == null) {
                    // Removed from A
                    diff.put("status", "REMOVED");
                    diff.put("segmentTag", extractTag(segA, elemDelim));
                    diff.put(request.getLabelA(), segA);
                    diff.put(request.getLabelB(), null);
                    differences.add(diff);
                    removedCount++;
                } else if (!segA.equals(segB)) {
                    // Modified
                    diff.put("status", "MODIFIED");
                    diff.put("segmentTag", extractTag(segA, elemDelim));
                    diff.put(request.getLabelA(), segA);
                    diff.put(request.getLabelB(), segB);

                    // Element-level diff
                    List<Map<String, Object>> elementDiffs = diffElements(segA, segB, elemDelim);
                    if (!elementDiffs.isEmpty()) {
                        diff.put("elementDifferences", elementDiffs);
                    }

                    differences.add(diff);
                    modifiedCount++;
                } else {
                    matchCount++;
                }
            }

            Map<String, Object> data = new LinkedHashMap<>();

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("segmentsInA", segmentsA.length);
            summary.put("segmentsInB", segmentsB.length);
            summary.put("identical", differences.isEmpty());
            summary.put("matchingSegments", matchCount);
            summary.put("modifiedSegments", modifiedCount);
            summary.put("addedSegments", addedCount);
            summary.put("removedSegments", removedCount);
            summary.put("totalDifferences", differences.size());
            data.put("summary", summary);

            data.put("differences", differences);

            ApiResponse response = ApiResponse.ok(data);
            if (differences.isEmpty()) {
                response.setMessage("Documents are identical (" + matchCount + " segments match)");
            } else {
                response.setMessage(differences.size() + " difference(s) found: "
                        + modifiedCount + " modified, " + addedCount + " added, " + removedCount + " removed");
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LOG.error("Compare failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Compare failed: " + e.getMessage()));
        }
    }

    private String[] splitSegments(String edi, char segDelim) {
        // Split on segment delimiter, filter empties
        String[] raw = edi.split(java.util.regex.Pattern.quote(String.valueOf(segDelim)));
        List<String> segments = new ArrayList<>();
        for (String s : raw) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                segments.add(trimmed);
            }
        }
        return segments.toArray(new String[0]);
    }

    private String extractTag(String segment, char elemDelim) {
        if (segment == null) return "";
        int idx = segment.indexOf(elemDelim);
        return idx > 0 ? segment.substring(0, idx) : segment;
    }

    /**
     * Compares elements within two segments that share the same tag.
     */
    private List<Map<String, Object>> diffElements(String segA, String segB, char elemDelim) {
        List<Map<String, Object>> diffs = new ArrayList<>();
        String delimRegex = java.util.regex.Pattern.quote(String.valueOf(elemDelim));
        String[] elemsA = segA.split(delimRegex, -1);
        String[] elemsB = segB.split(delimRegex, -1);

        int maxElems = Math.max(elemsA.length, elemsB.length);
        for (int i = 1; i < maxElems; i++) { // skip index 0 (segment tag)
            String valA = i < elemsA.length ? elemsA[i] : "";
            String valB = i < elemsB.length ? elemsB[i] : "";

            if (!valA.equals(valB)) {
                Map<String, Object> elemDiff = new LinkedHashMap<>();
                String tag = (elemsA.length > 0 ? elemsA[0] : "");
                elemDiff.put("element", tag + String.format("%02d", i));
                elemDiff.put("position", i);
                elemDiff.put("valueA", valA);
                elemDiff.put("valueB", valB);
                diffs.add(elemDiff);
            }
        }
        return diffs;
    }
}
