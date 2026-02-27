package javax.edi.bind.schema;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.edi.bind.annotations.EDICollectionType;
import javax.edi.bind.annotations.EDIComponent;
import javax.edi.bind.annotations.EDIElement;
import javax.edi.bind.annotations.EDIHierarchicalIdentifier;
import javax.edi.bind.annotations.EDIHierarchicalParentReference;
import javax.edi.bind.annotations.EDIMessage;
import javax.edi.bind.annotations.EDISegment;
import javax.edi.bind.annotations.EDISegmentGroup;
import javax.edi.bind.annotations.elements.EDIElementFormat;
import javax.edi.bind.annotations.elements.EDIElementFormats;
import javax.edi.bind.annotations.elements.EDILocale;
import javax.edi.bind.annotations.elements.EDITimezone;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Auto-generates a human-readable EDI Implementation Guide from model classes.
 * 
 * Produces a structured document listing all segments, elements, positions,
 * data types, constraints, and whether each field is required or optional.
 * Supports nested segment groups, hierarchical links, composite elements,
 * multiple output formats, and summary statistics.
 * 
 * Use cases:
 * - Share EDI specs with trading partners without writing docs manually
 * - Onboard new developers -- they can see the full structure at a glance
 * - Audit model annotations -- quickly spot missing constraints
 * - Compliance documentation
 * 
 * Usage:
 *   // Plain text output (default)
 *   String doc = EDIDocGenerator.generate(PurchaseOrder.class);
 *   
 *   // Markdown output
 *   String md = EDIDocGenerator.generate(PurchaseOrder.class, Format.MARKDOWN);
 */
public class EDIDocGenerator {

    /** Output format for the generated document. */
    public enum Format {
        TEXT,
        MARKDOWN,
        HTML
    }

    private EDIDocGenerator() {
        // seal
    }

    // ========== Statistics tracking ==========

    private static class Stats {
        int totalSegments;
        int totalElements;
        int requiredElements;
        int optionalElements;
        int totalLoops;
        int totalGroups;
    }

    // ========== Public API ==========

    /**
     * Generates a human-readable EDI implementation guide in plain text format.
     *
     * @param clazz the EDI message class (annotated with @EDIMessage)
     * @return formatted implementation guide as a string
     */
    public static String generate(Class<?> clazz) {
        return generate(clazz, Format.TEXT);
    }

    /**
     * Generates a human-readable EDI implementation guide in the specified format.
     *
     * @param clazz  the EDI message class (annotated with @EDIMessage)
     * @param format the output format (TEXT, MARKDOWN, or HTML)
     * @return formatted implementation guide as a string
     */
    public static String generate(Class<?> clazz, Format format) {
        if (format == Format.MARKDOWN) {
            return generateMarkdown(clazz);
        }
        if (format == Format.HTML) {
            return generateHtml(clazz, null);
        }
        return generateText(clazz);
    }

    /**
     * Generates a human-readable EDI implementation guide in HTML format
     * with an optional embedded view URL for linking back to the service.
     *
     * @param clazz   the EDI message class (annotated with @EDIMessage)
     * @param viewUrl optional URL to embed as a permalink (can be null)
     * @return self-contained HTML document as a string
     */
    public static String generateHtml(Class<?> clazz, String viewUrl) {
        String markdown = generateMarkdown(clazz);
        return wrapMarkdownInHtml(clazz.getSimpleName(), markdown, viewUrl);
    }

    // ========== TEXT Format ==========

    private static String generateText(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        Set<Class<?>> visited = new HashSet<>();
        Stats stats = new Stats();

        // --- Table of Contents pass ---
        List<String> toc = new ArrayList<>();
        collectToc(clazz, toc, 0, new HashSet<Class<?>>());

        sb.append("================================================================\n");
        sb.append("  EDI IMPLEMENTATION GUIDE\n");
        sb.append("  Generated from: ").append(clazz.getSimpleName()).append("\n");

        if (clazz.isAnnotationPresent(EDIMessage.class)) {
            EDIMessage msg = clazz.getAnnotation(EDIMessage.class);
            sb.append("  Element Delimiter : '").append(msg.elementDelimiter()).append("'\n");
            sb.append("  Segment Delimiter : '").append(msg.segmentDelimiter()).append("'\n");
            sb.append("  Component Delimiter: '").append(msg.componentDelimiter()).append("'\n");
        }

        sb.append("================================================================\n\n");

        // Table of Contents
        if (!toc.isEmpty()) {
            sb.append("TABLE OF CONTENTS\n");
            sb.append("----------------------------------------------------------------\n");
            for (String entry : toc) {
                sb.append(entry).append("\n");
            }
            sb.append("----------------------------------------------------------------\n\n");
        }

        // Body
        generateClassDocText(clazz, sb, 0, visited, stats);

        // Summary
        sb.append("\n");
        sb.append("================================================================\n");
        sb.append("  SUMMARY\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append("  Total Segments : ").append(stats.totalSegments).append("\n");
        sb.append("  Total Loops    : ").append(stats.totalLoops).append("\n");
        sb.append("  Total Groups   : ").append(stats.totalGroups).append("\n");
        sb.append("  Total Elements : ").append(stats.totalElements).append("\n");
        sb.append("  Required       : ").append(stats.requiredElements).append("\n");
        sb.append("  Optional       : ").append(stats.optionalElements).append("\n");
        sb.append("================================================================\n");
        sb.append("  END OF IMPLEMENTATION GUIDE\n");
        sb.append("================================================================\n");

        return sb.toString();
    }

    /** Collects a flat table-of-contents list for all segments/groups. */
    private static void collectToc(Class<?> clazz, List<String> toc, int depth, Set<Class<?>> visited) {
        if (visited.contains(clazz)) {
            return;
        }
        visited.add(clazz);
        String indent = repeat("  ", depth);

        for (Field field : getAllFields(clazz)) {
            if (field.isSynthetic()) continue;
            Class<?> fieldType = field.getType();

            if (Collection.class.isAssignableFrom(fieldType)) {
                Class<?> itemType = getCollectionItemType(field);
                if (itemType != null) {
                    if (itemType.isAnnotationPresent(EDISegment.class)) {
                        EDISegment seg = itemType.getAnnotation(EDISegment.class);
                        toc.add(indent + "LOOP " + seg.tag() + " (" + itemType.getSimpleName() + ")");
                    } else if (itemType.isAnnotationPresent(EDISegmentGroup.class)) {
                        EDISegmentGroup grp = itemType.getAnnotation(EDISegmentGroup.class);
                        String header = grp.header().isEmpty() ? "" : " [" + grp.header() + "]";
                        toc.add(indent + "LOOP GROUP" + header + " (" + itemType.getSimpleName() + ")");
                        collectToc(itemType, toc, depth + 1, new HashSet<>(visited));
                    }
                }
            } else if (fieldType.isAnnotationPresent(EDISegment.class)) {
                EDISegment seg = fieldType.getAnnotation(EDISegment.class);
                toc.add(indent + "Segment " + seg.tag() + " (" + fieldType.getSimpleName() + ")");
            } else if (fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                EDISegmentGroup grp = fieldType.getAnnotation(EDISegmentGroup.class);
                String header = grp.header().isEmpty() ? "" : " [" + grp.header() + "]";
                toc.add(indent + "Group" + header + " (" + fieldType.getSimpleName() + ")");
                collectToc(fieldType, toc, depth + 1, new HashSet<>(visited));
            }
        }
    }

    private static void generateClassDocText(Class<?> clazz, StringBuilder sb, int depth,
                                             Set<Class<?>> visited, Stats stats) {
        if (visited.contains(clazz)) {
            return;
        }
        visited.add(clazz);

        String indent = repeat("  ", depth);

        for (Field field : getAllFields(clazz)) {
            if (field.isSynthetic()) continue;

            Class<?> fieldType = field.getType();

            // Collection field
            if (Collection.class.isAssignableFrom(fieldType)) {
                Class<?> itemType = getCollectionItemType(field);

                if (itemType != null) {
                    String sizeInfo = "";
                    if (field.isAnnotationPresent(Size.class)) {
                        Size size = field.getAnnotation(Size.class);
                        sizeInfo = " [max repeat: " + (size.max() < Integer.MAX_VALUE ? size.max() : "unbounded") + "]";
                    }

                    boolean required = field.isAnnotationPresent(NotNull.class);

                    if (itemType.isAnnotationPresent(EDISegment.class)) {
                        EDISegment seg = itemType.getAnnotation(EDISegment.class);
                        sb.append(indent).append(required ? "[R] " : "[O] ");
                        sb.append("LOOP ").append(seg.tag()).append(sizeInfo);
                        sb.append(" (").append(itemType.getSimpleName()).append(")\n");
                        stats.totalLoops++;
                        generateSegmentDocText(itemType, sb, depth + 1, stats);
                        sb.append("\n");
                    } else if (itemType.isAnnotationPresent(EDISegmentGroup.class)) {
                        EDISegmentGroup grp = itemType.getAnnotation(EDISegmentGroup.class);
                        String header = grp.header().isEmpty() ? "" : " header=" + grp.header();
                        String footer = grp.footer().isEmpty() ? "" : " footer=" + grp.footer();
                        sb.append(indent).append(required ? "[R] " : "[O] ");
                        sb.append("LOOP GROUP").append(header).append(footer).append(sizeInfo);
                        sb.append(" (").append(itemType.getSimpleName()).append(")\n");
                        stats.totalLoops++;
                        stats.totalGroups++;
                        Set<Class<?>> branchVisited = new HashSet<>(visited);
                        generateClassDocText(itemType, sb, depth + 1, branchVisited, stats);
                        sb.append("\n");
                    }
                }
            }
            // Segment field
            else if (fieldType.isAnnotationPresent(EDISegment.class)) {
                boolean required = field.isAnnotationPresent(NotNull.class);
                EDISegment seg = fieldType.getAnnotation(EDISegment.class);
                sb.append(indent).append(required ? "[R] " : "[O] ");
                sb.append("Segment ").append(seg.tag());
                sb.append(" (").append(fieldType.getSimpleName()).append(")\n");
                generateSegmentDocText(fieldType, sb, depth + 1, stats);
                sb.append("\n");
            }
            // Segment Group field
            else if (fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                boolean required = field.isAnnotationPresent(NotNull.class);
                EDISegmentGroup grp = fieldType.getAnnotation(EDISegmentGroup.class);
                String header = grp.header().isEmpty() ? "" : " header=" + grp.header();
                String footer = grp.footer().isEmpty() ? "" : " footer=" + grp.footer();
                sb.append(indent).append(required ? "[R] " : "[O] ");
                sb.append("Group").append(header).append(footer);
                sb.append(" (").append(fieldType.getSimpleName()).append(")\n");
                stats.totalGroups++;
                Set<Class<?>> branchVisited = new HashSet<>(visited);
                generateClassDocText(fieldType, sb, depth + 1, branchVisited, stats);
                sb.append("\n");
            }
        }
    }

    private static void generateSegmentDocText(Class<?> segmentClass, StringBuilder sb, int depth, Stats stats) {
        String indent = repeat("  ", depth);

        EDISegment seg = segmentClass.getAnnotation(EDISegment.class);
        if (seg == null) return;

        stats.totalSegments++;

        sb.append(indent).append("+------+----------+-----------+------+----------+-------------------------------\n");
        sb.append(indent).append("| Segment: ").append(seg.tag()).append("\n");
        sb.append(indent).append("+------+----------+-----------+------+----------+-------------------------------\n");
        sb.append(indent).append("| Pos  | Element  | Field     | Type | Required | Constraints\n");
        sb.append(indent).append("+------+----------+-----------+------+----------+-------------------------------\n");

        int pos = 1;
        for (Field field : getAllFields(segmentClass)) {
            if (field.isSynthetic()) continue;

            Class<?> fieldType = field.getType();

            // Composite element (Collection with @EDIComponent)
            if (Collection.class.isAssignableFrom(fieldType) && field.isAnnotationPresent(EDIComponent.class)) {
                EDIComponent comp = field.getAnnotation(EDIComponent.class);
                Class<?> compType = getCollectionItemType(field);
                String compTypeName = compType != null ? getTypeLabel(compType) : "AN";
                char delim = comp.delimiter();
                String delimInfo = delim != 0 ? " delim='" + delim + "'" : "";

                boolean required = isFieldRequired(field);
                String posStr = String.format("%02d", pos);
                String elemId = getElementId(field);
                String desc = getElementDescription(field);
                String constraints = buildConstraints(field);

                sb.append(indent).append("| ")
                  .append(padRight(posStr, 4)).append(" | ")
                  .append(padRight(elemId, 8)).append(" | ")
                  .append(padRight(truncate(field.getName(), 9), 9)).append(" | ")
                  .append(padRight(compTypeName, 4)).append(" | ")
                  .append(padRight(required ? "YES" : "no", 8)).append(" | ")
                  .append("COMPOSITE").append(delimInfo);
                if (!constraints.isEmpty()) {
                    sb.append(" ").append(constraints);
                }
                if (!desc.isEmpty()) {
                    sb.append(" -- ").append(desc);
                }
                sb.append("\n");

                stats.totalElements++;
                if (required) stats.requiredElements++; else stats.optionalElements++;
                pos++;
                continue;
            }

            // Skip non-element collections (loops handled at class level)
            if (Collection.class.isAssignableFrom(fieldType)) {
                pos++;
                continue;
            }

            // Nested segment/group references — skip (handled at class level)
            if (fieldType.isAnnotationPresent(EDISegment.class) || fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                pos++;
                continue;
            }

            String posStr = String.format("%02d", pos);
            String elemId = getElementId(field);
            String fieldName = field.getName();
            String type = getTypeLabel(fieldType);
            boolean required = isFieldRequired(field);
            String constraints = buildConstraints(field);
            String desc = getElementDescription(field);

            // Hierarchical markers
            String hlMarker = "";
            if (field.isAnnotationPresent(EDIHierarchicalIdentifier.class)) {
                hlMarker = " [HL-ID]";
            } else if (field.isAnnotationPresent(EDIHierarchicalParentReference.class)) {
                hlMarker = " [HL-PARENT]";
            }

            sb.append(indent).append("| ")
              .append(padRight(posStr, 4)).append(" | ")
              .append(padRight(elemId, 8)).append(" | ")
              .append(padRight(truncate(fieldName, 9), 9)).append(" | ")
              .append(padRight(type, 4)).append(" | ")
              .append(padRight(required ? "YES" : "no", 8)).append(" | ")
              .append(constraints).append(hlMarker);
            if (!desc.isEmpty()) {
                sb.append(" -- ").append(desc);
            }
            sb.append("\n");

            stats.totalElements++;
            if (required) stats.requiredElements++; else stats.optionalElements++;
            pos++;
        }

        sb.append(indent).append("+------+----------+-----------+------+----------+-------------------------------\n");
    }

    // ========== MARKDOWN Format ==========

    private static String generateMarkdown(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        Set<Class<?>> visited = new HashSet<>();
        Stats stats = new Stats();

        sb.append("# EDI Implementation Guide\n\n");
        sb.append("**Generated from:** `").append(clazz.getSimpleName()).append("`\n\n");

        if (clazz.isAnnotationPresent(EDIMessage.class)) {
            EDIMessage msg = clazz.getAnnotation(EDIMessage.class);
            sb.append("| Property | Value |\n");
            sb.append("|----------|-------|\n");
            sb.append("| Element Delimiter | `").append(msg.elementDelimiter()).append("` |\n");
            sb.append("| Segment Delimiter | `").append(msg.segmentDelimiter()).append("` |\n");
            sb.append("| Component Delimiter | `").append(msg.componentDelimiter()).append("` |\n");
            sb.append("\n");
        }

        // Table of Contents
        List<String> toc = new ArrayList<>();
        collectTocMd(clazz, toc, 0, new HashSet<Class<?>>());
        if (!toc.isEmpty()) {
            sb.append("## Table of Contents\n\n");
            for (String entry : toc) {
                sb.append(entry).append("\n");
            }
            sb.append("\n---\n\n");
        }

        sb.append("## Structure\n\n");
        generateClassDocMd(clazz, sb, 0, visited, stats);

        // Summary
        sb.append("---\n\n");
        sb.append("## Summary\n\n");
        sb.append("| Metric | Count |\n");
        sb.append("|--------|-------|\n");
        sb.append("| Total Segments | ").append(stats.totalSegments).append(" |\n");
        sb.append("| Total Loops | ").append(stats.totalLoops).append(" |\n");
        sb.append("| Total Groups | ").append(stats.totalGroups).append(" |\n");
        sb.append("| Total Elements | ").append(stats.totalElements).append(" |\n");
        sb.append("| Required | ").append(stats.requiredElements).append(" |\n");
        sb.append("| Optional | ").append(stats.optionalElements).append(" |\n");

        return sb.toString();
    }

    private static void collectTocMd(Class<?> clazz, List<String> toc, int depth, Set<Class<?>> visited) {
        if (visited.contains(clazz)) return;
        visited.add(clazz);
        String bullet = repeat("  ", depth) + "- ";

        for (Field field : getAllFields(clazz)) {
            if (field.isSynthetic()) continue;
            Class<?> fieldType = field.getType();

            if (Collection.class.isAssignableFrom(fieldType)) {
                Class<?> itemType = getCollectionItemType(field);
                if (itemType != null) {
                    if (itemType.isAnnotationPresent(EDISegment.class)) {
                        EDISegment seg = itemType.getAnnotation(EDISegment.class);
                        toc.add(bullet + "**LOOP " + seg.tag() + "** (`" + itemType.getSimpleName() + "`)");
                    } else if (itemType.isAnnotationPresent(EDISegmentGroup.class)) {
                        EDISegmentGroup grp = itemType.getAnnotation(EDISegmentGroup.class);
                        String header = grp.header().isEmpty() ? "" : " [" + grp.header() + "]";
                        toc.add(bullet + "**LOOP GROUP" + header + "** (`" + itemType.getSimpleName() + "`)");
                        collectTocMd(itemType, toc, depth + 1, new HashSet<>(visited));
                    }
                }
            } else if (fieldType.isAnnotationPresent(EDISegment.class)) {
                EDISegment seg = fieldType.getAnnotation(EDISegment.class);
                toc.add(bullet + "**Segment " + seg.tag() + "** (`" + fieldType.getSimpleName() + "`)");
            } else if (fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                EDISegmentGroup grp = fieldType.getAnnotation(EDISegmentGroup.class);
                String header = grp.header().isEmpty() ? "" : " [" + grp.header() + "]";
                toc.add(bullet + "**Group" + header + "** (`" + fieldType.getSimpleName() + "`)");
                collectTocMd(fieldType, toc, depth + 1, new HashSet<>(visited));
            }
        }
    }

    private static void generateClassDocMd(Class<?> clazz, StringBuilder sb, int depth,
                                           Set<Class<?>> visited, Stats stats) {
        if (visited.contains(clazz)) return;
        visited.add(clazz);

        int headingLevel = Math.min(depth + 3, 6); // h3..h6
        String heading = repeat("#", headingLevel) + " ";

        for (Field field : getAllFields(clazz)) {
            if (field.isSynthetic()) continue;
            Class<?> fieldType = field.getType();

            if (Collection.class.isAssignableFrom(fieldType)) {
                Class<?> itemType = getCollectionItemType(field);
                if (itemType != null) {
                    String sizeInfo = "";
                    if (field.isAnnotationPresent(Size.class)) {
                        Size size = field.getAnnotation(Size.class);
                        sizeInfo = " *(max repeat: " + (size.max() < Integer.MAX_VALUE ? size.max() : "unbounded") + ")*";
                    }
                    boolean required = field.isAnnotationPresent(NotNull.class);
                    String req = required ? " `[R]`" : " `[O]`";

                    if (itemType.isAnnotationPresent(EDISegment.class)) {
                        EDISegment seg = itemType.getAnnotation(EDISegment.class);
                        sb.append(heading).append("LOOP ").append(seg.tag())
                          .append(" (`").append(itemType.getSimpleName()).append("`)").append(req).append(sizeInfo).append("\n\n");
                        stats.totalLoops++;
                        generateSegmentDocMd(itemType, sb, stats);
                        sb.append("\n");
                    } else if (itemType.isAnnotationPresent(EDISegmentGroup.class)) {
                        EDISegmentGroup grp = itemType.getAnnotation(EDISegmentGroup.class);
                        String header = grp.header().isEmpty() ? "" : " [" + grp.header() + "]";
                        String footer = grp.footer().isEmpty() ? "" : " footer=" + grp.footer();
                        sb.append(heading).append("LOOP GROUP").append(header).append(footer)
                          .append(" (`").append(itemType.getSimpleName()).append("`)").append(req).append(sizeInfo).append("\n\n");
                        stats.totalLoops++;
                        stats.totalGroups++;
                        Set<Class<?>> branchVisited = new HashSet<>(visited);
                        generateClassDocMd(itemType, sb, depth + 1, branchVisited, stats);
                    }
                }
            } else if (fieldType.isAnnotationPresent(EDISegment.class)) {
                boolean required = field.isAnnotationPresent(NotNull.class);
                EDISegment seg = fieldType.getAnnotation(EDISegment.class);
                String req = required ? " `[R]`" : " `[O]`";
                sb.append(heading).append("Segment ").append(seg.tag())
                  .append(" (`").append(fieldType.getSimpleName()).append("`)").append(req).append("\n\n");
                generateSegmentDocMd(fieldType, sb, stats);
                sb.append("\n");
            } else if (fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                boolean required = field.isAnnotationPresent(NotNull.class);
                EDISegmentGroup grp = fieldType.getAnnotation(EDISegmentGroup.class);
                String header = grp.header().isEmpty() ? "" : " header=" + grp.header();
                String footer = grp.footer().isEmpty() ? "" : " footer=" + grp.footer();
                String req = required ? " `[R]`" : " `[O]`";
                sb.append(heading).append("Group").append(header).append(footer)
                  .append(" (`").append(fieldType.getSimpleName()).append("`)").append(req).append("\n\n");
                stats.totalGroups++;
                Set<Class<?>> branchVisited = new HashSet<>(visited);
                generateClassDocMd(fieldType, sb, depth + 1, branchVisited, stats);
            }
        }
    }

    private static void generateSegmentDocMd(Class<?> segmentClass, StringBuilder sb, Stats stats) {
        EDISegment seg = segmentClass.getAnnotation(EDISegment.class);
        if (seg == null) return;

        stats.totalSegments++;

        sb.append("| Pos | Element | Field | Type | Required | Constraints | Description |\n");
        sb.append("|-----|---------|-------|------|----------|-------------|-------------|\n");

        int pos = 1;
        for (Field field : getAllFields(segmentClass)) {
            if (field.isSynthetic()) continue;
            Class<?> fieldType = field.getType();

            // Composite element
            if (Collection.class.isAssignableFrom(fieldType) && field.isAnnotationPresent(EDIComponent.class)) {
                EDIComponent comp = field.getAnnotation(EDIComponent.class);
                Class<?> compType = getCollectionItemType(field);
                String compTypeName = compType != null ? getTypeLabel(compType) : "AN";
                char delim = comp.delimiter();
                String delimInfo = delim != 0 ? " delim=`" + delim + "`" : "";

                boolean required = isFieldRequired(field);
                String constraints = buildConstraints(field);

                sb.append("| ").append(String.format("%02d", pos))
                  .append(" | ").append(getElementId(field))
                  .append(" | ").append(field.getName())
                  .append(" | ").append(compTypeName)
                  .append(" | ").append(required ? "**YES**" : "no")
                  .append(" | COMPOSITE").append(delimInfo)
                  .append(!constraints.isEmpty() ? " " + constraints : "")
                  .append(" | ").append(getElementDescription(field))
                  .append(" |\n");

                stats.totalElements++;
                if (required) stats.requiredElements++; else stats.optionalElements++;
                pos++;
                continue;
            }

            if (Collection.class.isAssignableFrom(fieldType)
                    || fieldType.isAnnotationPresent(EDISegment.class)
                    || fieldType.isAnnotationPresent(EDISegmentGroup.class)) {
                pos++;
                continue;
            }

            boolean required = isFieldRequired(field);
            String constraints = buildConstraints(field);
            String hlMarker = "";
            if (field.isAnnotationPresent(EDIHierarchicalIdentifier.class)) {
                hlMarker = " `[HL-ID]`";
            } else if (field.isAnnotationPresent(EDIHierarchicalParentReference.class)) {
                hlMarker = " `[HL-PARENT]`";
            }

            sb.append("| ").append(String.format("%02d", pos))
              .append(" | ").append(getElementId(field))
              .append(" | ").append(field.getName())
              .append(" | ").append(getTypeLabel(fieldType))
              .append(" | ").append(required ? "**YES**" : "no")
              .append(" | ").append(constraints).append(hlMarker)
              .append(" | ").append(getElementDescription(field))
              .append(" |\n");

            stats.totalElements++;
            if (required) stats.requiredElements++; else stats.optionalElements++;
            pos++;
        }

        sb.append("\n");
    }

    // ========== Shared helpers ==========

    /** Returns all declared fields including those from superclasses (bottom-up). */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            // Prepend superclass fields so they appear first
            Field[] declared = current.getDeclaredFields();
            for (int i = declared.length - 1; i >= 0; i--) {
                fields.add(0, declared[i]);
            }
            current = current.getSuperclass();
        }
        // Remove duplicates (child hides parent) — keep last occurrence (child wins)
        Set<String> seen = new HashSet<>();
        List<Field> unique = new ArrayList<>();
        // Walk from end to start so child fields win
        for (int i = fields.size() - 1; i >= 0; i--) {
            if (seen.add(fields.get(i).getName())) {
                unique.add(0, fields.get(i));
            }
        }
        return unique;
    }

    /** Extracts the collection item type from @EDICollectionType. */
    private static Class<?> getCollectionItemType(Field field) {
        if (field.isAnnotationPresent(EDICollectionType.class)) {
            return field.getAnnotation(EDICollectionType.class).value();
        }
        return null;
    }

    /** Determines if a field is required via @NotNull or @EDIElement(required=true). */
    private static boolean isFieldRequired(Field field) {
        if (field.isAnnotationPresent(NotNull.class)) {
            return true;
        }
        if (field.isAnnotationPresent(EDIElement.class)) {
            return field.getAnnotation(EDIElement.class).required();
        }
        return false;
    }

    /** Gets the element ID from @EDIElement.fieldName(). */
    private static String getElementId(Field field) {
        if (field.isAnnotationPresent(EDIElement.class)) {
            EDIElement elem = field.getAnnotation(EDIElement.class);
            if (!elem.fieldName().isEmpty()) return elem.fieldName();
        }
        return "";
    }

    /** Gets the description from @EDIElement.description(). */
    private static String getElementDescription(Field field) {
        if (field.isAnnotationPresent(EDIElement.class)) {
            EDIElement elem = field.getAnnotation(EDIElement.class);
            if (!elem.description().isEmpty()) return elem.description();
        }
        return "";
    }

    private static String buildConstraints(Field field) {
        StringBuilder c = new StringBuilder();

        if (field.isAnnotationPresent(Size.class)) {
            Size size = field.getAnnotation(Size.class);
            c.append("len[").append(size.min()).append("-").append(size.max()).append("] ");
        }
        if (field.isAnnotationPresent(Min.class)) {
            c.append("min=").append(field.getAnnotation(Min.class).value()).append(" ");
        }
        if (field.isAnnotationPresent(Max.class)) {
            c.append("max=").append(field.getAnnotation(Max.class).value()).append(" ");
        }
        if (field.isAnnotationPresent(DecimalMin.class)) {
            c.append("min=").append(field.getAnnotation(DecimalMin.class).value()).append(" ");
        }
        if (field.isAnnotationPresent(DecimalMax.class)) {
            c.append("max=").append(field.getAnnotation(DecimalMax.class).value()).append(" ");
        }

        // Single format
        if (field.isAnnotationPresent(EDIElementFormat.class)) {
            c.append("fmt=").append(field.getAnnotation(EDIElementFormat.class).value()).append(" ");
        }
        // Multiple formats
        if (field.isAnnotationPresent(EDIElementFormats.class)) {
            EDIElementFormat[] formats = field.getAnnotation(EDIElementFormats.class).value();
            for (EDIElementFormat fmt : formats) {
                c.append("fmt=").append(fmt.value()).append(" ");
            }
        }

        // Conditional flag
        if (field.isAnnotationPresent(EDIElement.class) && field.getAnnotation(EDIElement.class).conditional()) {
            c.append("CONDITIONAL ");
        }

        // Data element reference
        if (field.isAnnotationPresent(EDIElement.class)) {
            String de = field.getAnnotation(EDIElement.class).dataElement();
            if (!de.isEmpty()) {
                c.append("DE=").append(de).append(" ");
            }
        }

        // Locale
        if (field.isAnnotationPresent(EDILocale.class)) {
            EDILocale loc = field.getAnnotation(EDILocale.class);
            c.append("locale=").append(loc.language()).append("_").append(loc.region()).append(" ");
        }

        // Timezone
        if (field.isAnnotationPresent(EDITimezone.class)) {
            c.append("tz=").append(field.getAnnotation(EDITimezone.class).timezone()).append(" ");
        }

        return c.toString().trim();
    }

    private static String getTypeLabel(Class<?> type) {
        if (String.class.isAssignableFrom(type)) return "AN";
        if (java.util.Date.class.isAssignableFrom(type)) return "DT";
        if (java.math.BigDecimal.class.isAssignableFrom(type)) return "R";
        if (java.math.BigInteger.class.isAssignableFrom(type)) return "N0";
        if (int.class == type || Integer.class.isAssignableFrom(type)) return "N0";
        if (long.class == type || Long.class.isAssignableFrom(type)) return "N0";
        if (float.class == type || Float.class.isAssignableFrom(type)) return "R";
        if (double.class == type || Double.class.isAssignableFrom(type)) return "R";
        if (boolean.class == type || Boolean.class.isAssignableFrom(type)) return "ID";
        if (Collection.class.isAssignableFrom(type)) return "CMP";
        return "AN";
    }

    private static String padRight(String str, int len) {
        if (str.length() >= len) return str.substring(0, len);
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
    }

    private static String truncate(String str, int len) {
        return str.length() <= len ? str : str.substring(0, len);
    }

    private static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(str);
        return sb.toString();
    }

    // ========== HTML wrapper ==========

    /**
     * Wraps markdown content in a self-contained HTML page.
     * Uses a lightweight client-side markdown renderer (marked.js via CDN)
     * so the HTML file works standalone in any browser.
     */
    private static String wrapMarkdownInHtml(String title, String markdown, String viewUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>EDI Implementation Guide - ").append(escapeHtml(title)).append("</title>\n");
        html.append("  <style>\n");
        html.append("    :root { --bg: #ffffff; --fg: #1a1a2e; --accent: #0f3460; --border: #d1d5db; --row-alt: #f8f9fa; --tag: #e2e8f0; }\n");
        html.append("    * { box-sizing: border-box; margin: 0; padding: 0; }\n");
        html.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n");
        html.append("           line-height: 1.6; color: var(--fg); background: var(--bg); max-width: 1100px;\n");
        html.append("           margin: 0 auto; padding: 2rem 1.5rem; }\n");
        html.append("    h1 { font-size: 1.8rem; border-bottom: 3px solid var(--accent); padding-bottom: 0.5rem; margin-bottom: 1.5rem; color: var(--accent); }\n");
        html.append("    h2 { font-size: 1.4rem; margin: 2rem 0 0.8rem; color: var(--accent); border-bottom: 1px solid var(--border); padding-bottom: 0.3rem; }\n");
        html.append("    h3, h4, h5, h6 { margin: 1.5rem 0 0.5rem; color: #16213e; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; margin: 0.8rem 0 1.5rem; font-size: 0.88rem; }\n");
        html.append("    th { background: var(--accent); color: #fff; text-align: left; padding: 0.5rem 0.75rem; font-weight: 600; }\n");
        html.append("    td { padding: 0.4rem 0.75rem; border-bottom: 1px solid var(--border); }\n");
        html.append("    tr:nth-child(even) td { background: var(--row-alt); }\n");
        html.append("    code { background: var(--tag); padding: 0.15rem 0.4rem; border-radius: 3px; font-size: 0.85em; }\n");
        html.append("    p { margin: 0.5rem 0; }\n");
        html.append("    ul { margin: 0.5rem 0 0.5rem 1.5rem; }\n");
        html.append("    hr { border: none; border-top: 1px solid var(--border); margin: 2rem 0; }\n");
        html.append("    strong { font-weight: 700; }\n");
        html.append("    .permalink { background: var(--tag); padding: 0.4rem 0.8rem; border-radius: 4px;\n");
        html.append("                 font-size: 0.82rem; display: inline-block; margin-bottom: 1.2rem; word-break: break-all; }\n");
        html.append("    .permalink a { color: var(--accent); text-decoration: none; }\n");
        html.append("    .permalink a:hover { text-decoration: underline; }\n");
        html.append("    .generated-at { color: #6b7280; font-size: 0.8rem; margin-top: 2rem; }\n");
        html.append("    @media (max-width: 768px) { body { padding: 1rem; } table { font-size: 0.78rem; } }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Permalink banner
        if (viewUrl != null && !viewUrl.isEmpty()) {
            html.append("  <div class=\"permalink\">\n");
            html.append("    Permalink: <a href=\"").append(escapeHtml(viewUrl)).append("\">").append(escapeHtml(viewUrl)).append("</a>\n");
            html.append("  </div>\n");
        }

        // Markdown content rendered by marked.js
        html.append("  <div id=\"content\"></div>\n");
        html.append("  <script id=\"md\" type=\"text/markdown\">\n");
        // Escape </script> inside markdown to prevent premature closing
        html.append(markdown.replace("</script>", "<\\/script>"));
        html.append("\n  </script>\n");

        // marked.js CDN - lightweight markdown parser
        html.append("  <script src=\"https://cdn.jsdelivr.net/npm/marked/marked.min.js\"></script>\n");
        html.append("  <script>\n");
        html.append("    var md = document.getElementById('md').textContent;\n");
        html.append("    document.getElementById('content').innerHTML = marked.parse(md);\n");
        html.append("  </script>\n");

        // Timestamp
        html.append("  <p class=\"generated-at\">Generated: ").append(new java.util.Date().toString()).append("</p>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private static String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;");
    }
}
