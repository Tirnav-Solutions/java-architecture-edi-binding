package javax.edi.service.channel;

import java.util.regex.Pattern;

import javax.edi.configuration.EDIMessageConfiguration;
import javax.edi.service.entity.TradingPartner;

/**
 * Resolves EDI X12 delimiters using a priority chain:
 * 
 * <ol>
 *   <li><b>Trading Partner config</b> – if the partner entity has explicit
 *       delimiter overrides (elementDelimiter, segmentDelimiter, componentDelimiter),
 *       those take precedence.</li>
 *   <li><b>ISA header auto-detection</b> – per X12 spec, the ISA segment is always
 *       exactly 106 characters:
 *       <ul>
 *         <li>Position 3 (0-based): element separator (commonly {@code *})</li>
 *         <li>Position 104: sub-element/component separator (commonly {@code :})</li>
 *         <li>Position 105: segment terminator (commonly {@code ~})</li>
 *       </ul>
 *   </li>
 *   <li><b>Defaults</b> – {@code *} / {@code ~} / {@code :}</li>
 * </ol>
 */
public final class EDIDelimiterDetector {

    /** Default element separator used in most X12 messages. */
    public static final char DEFAULT_ELEMENT_SEPARATOR = '*';

    /** Default segment terminator used in most X12 messages. */
    public static final char DEFAULT_SEGMENT_TERMINATOR = '~';

    /** Default component (sub-element) separator. */
    public static final char DEFAULT_COMPONENT_SEPARATOR = ':';

    private final char elementSeparator;
    private final char segmentTerminator;
    private final char componentSeparator;

    private EDIDelimiterDetector(char elementSeparator, char segmentTerminator, char componentSeparator) {
        this.elementSeparator = elementSeparator;
        this.segmentTerminator = segmentTerminator;
        this.componentSeparator = componentSeparator;
    }

    // ======================== Factory methods ========================

    /**
     * Detect delimiters from the EDI content by inspecting the ISA header.
     * Falls back to defaults if the ISA header is absent or too short.
     *
     * @param ediContent the raw EDI string
     * @return an EDIDelimiterDetector with the detected (or default) delimiters
     */
    public static EDIDelimiterDetector detect(String ediContent) {
        return detect(ediContent, null);
    }

    /**
     * Resolve delimiters using the priority chain:
     * Partner config &rarr; ISA header &rarr; defaults.
     *
     * @param ediContent the raw EDI string (may be {@code null})
     * @param partner    the trading partner (may be {@code null})
     * @return an EDIDelimiterDetector with the resolved delimiters
     */
    public static EDIDelimiterDetector detect(String ediContent, TradingPartner partner) {
        // 1. Start with defaults
        char elemSep = DEFAULT_ELEMENT_SEPARATOR;
        char segTerm = DEFAULT_SEGMENT_TERMINATOR;
        char compSep = DEFAULT_COMPONENT_SEPARATOR;

        // 2. Try ISA header auto-detection
        if (ediContent != null && !ediContent.isEmpty()) {
            String trimmed = ediContent.stripLeading();
            if (trimmed.length() >= 106 && trimmed.startsWith("ISA")) {
                elemSep = trimmed.charAt(3);
                compSep = trimmed.charAt(104);
                segTerm = trimmed.charAt(105);
            }
        }

        // 3. Partner config overrides (highest priority)
        if (partner != null) {
            if (partner.getElementDelimiter() != null && !partner.getElementDelimiter().isEmpty()) {
                elemSep = partner.getElementDelimiter().charAt(0);
            }
            if (partner.getSegmentDelimiter() != null && !partner.getSegmentDelimiter().isEmpty()) {
                segTerm = partner.getSegmentDelimiter().charAt(0);
            }
            if (partner.getComponentDelimiter() != null && !partner.getComponentDelimiter().isEmpty()) {
                compSep = partner.getComponentDelimiter().charAt(0);
            }
        }

        return new EDIDelimiterDetector(elemSep, segTerm, compSep);
    }

    /**
     * Returns a detector with the standard default delimiters (* / ~ / :).
     */
    public static EDIDelimiterDetector defaults() {
        return new EDIDelimiterDetector(DEFAULT_ELEMENT_SEPARATOR, DEFAULT_SEGMENT_TERMINATOR, DEFAULT_COMPONENT_SEPARATOR);
    }

    // ======================== Accessors ========================

    public char getElementSeparator() {
        return elementSeparator;
    }

    public char getSegmentTerminator() {
        return segmentTerminator;
    }

    public char getComponentSeparator() {
        return componentSeparator;
    }

    // ======================== Convenience ========================

    /**
     * Converts this detector's delimiters into an {@link EDIMessageConfiguration}
     * for use with {@link javax.edi.bind.EDIUnmarshaller}.
     */
    public EDIMessageConfiguration toMessageConfiguration() {
        return new EDIMessageConfiguration(elementSeparator, componentSeparator, segmentTerminator);
    }

    /**
     * Splits the EDI content into segments using the detected segment terminator.
     */
    public String[] splitSegments(String ediContent) {
        return ediContent.split(Pattern.quote(String.valueOf(segmentTerminator)));
    }

    /**
     * Splits a segment into elements using the detected element separator.
     */
    public String[] splitElements(String segment) {
        return segment.split(Pattern.quote(String.valueOf(elementSeparator)));
    }

    /**
     * Detects the transaction type (ST01) from the EDI content using the
     * resolved delimiters. Falls back to GS01 mapping if ST is not found.
     *
     * @param ediContent the raw EDI string
     * @return the transaction type code (e.g. "850", "997"), or "UNKNOWN"
     */
    public String detectTransactionType(String ediContent) {
        String[] segments = splitSegments(ediContent);
        String stPrefix = "ST" + elementSeparator;
        for (String seg : segments) {
            String t = seg.trim();
            if (t.startsWith(stPrefix)) {
                String[] elems = splitElements(t);
                if (elems.length >= 2) {
                    return elems[1].trim();
                }
            }
        }

        // Fallback: try GS01 functional identifier mapping
        String gsPrefix = "GS" + elementSeparator;
        for (String seg : segments) {
            String t = seg.trim();
            if (t.startsWith(gsPrefix)) {
                String[] elems = splitElements(t);
                if (elems.length >= 2) {
                    String funcId = elems[1].trim();
                    switch (funcId) {
                        case "PO": return "850";
                        case "IN": return "810";
                        case "SH": return "856";
                        case "PR": return "855";
                        case "FA": return "997";
                        default:   return funcId;
                    }
                }
            }
        }

        return "UNKNOWN";
    }
}
