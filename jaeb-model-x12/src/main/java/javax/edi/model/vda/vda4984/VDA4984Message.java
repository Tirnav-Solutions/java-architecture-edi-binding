package javax.edi.model.vda.vda4984;

import java.util.List;

import javax.edi.bind.annotations.VDAMessage;
import javax.edi.model.vda.vda4984.record.*;

/**
 * VDA 4984 — Dispatch Advice (Lieferschein).
 *
 * <p>Fixed-width 128-character record format used in European automotive.
 * Communicates shipping/dispatch details from supplier to buyer.</p>
 *
 * <pre>
 *   721 — Header (delivery note, dates)
 *   722 — Item (part numbers, quantity, UoM)     [repeating]
 *   723 — Packaging (container, weight)           [repeating]
 *   729 — Trailer (totals)
 * </pre>
 */
@VDAMessage(standard = "4984", recordLength = 128)
public class VDA4984Message {

    private DispatchHeader header;
    private List<DispatchItem> items;
    private List<PackagingRecord> packaging;
    private DispatchTrailer trailer;

    public DispatchHeader getHeader() { return header; }
    public void setHeader(DispatchHeader v) { this.header = v; }
    public List<DispatchItem> getItems() { return items; }
    public void setItems(List<DispatchItem> v) { this.items = v; }
    public List<PackagingRecord> getPackaging() { return packaging; }
    public void setPackaging(List<PackagingRecord> v) { this.packaging = v; }
    public DispatchTrailer getTrailer() { return trailer; }
    public void setTrailer(DispatchTrailer v) { this.trailer = v; }
}
