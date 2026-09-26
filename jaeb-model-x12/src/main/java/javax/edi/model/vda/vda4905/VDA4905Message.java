package javax.edi.model.vda.vda4905;

import java.util.List;

import javax.edi.bind.annotations.VDAMessage;
import javax.edi.model.vda.vda4905.record.*;

/**
 * VDA 4905 — Delivery Schedule (Lieferabruf).
 *
 * <p>Fixed-width 128-character record format used in European automotive.
 * Communicates delivery call-off requirements from buyer to supplier.</p>
 *
 * <pre>
 *   511 — Header (customer, supplier, transfer date)
 *   512 — Material (part numbers, cumulative qty)  [repeating]
 *   513 — Delivery Call (date, qty, firm/planning) [repeating]
 *   519 — Trailer (record count)
 * </pre>
 */
@VDAMessage(standard = "4905", recordLength = 128)
public class VDA4905Message {

    private HeaderRecord header;
    private List<MaterialRecord> materials;
    private List<DeliveryCallRecord> deliveryCalls;
    private TrailerRecord trailer;

    public HeaderRecord getHeader() { return header; }
    public void setHeader(HeaderRecord v) { this.header = v; }
    public List<MaterialRecord> getMaterials() { return materials; }
    public void setMaterials(List<MaterialRecord> v) { this.materials = v; }
    public List<DeliveryCallRecord> getDeliveryCalls() { return deliveryCalls; }
    public void setDeliveryCalls(List<DeliveryCallRecord> v) { this.deliveryCalls = v; }
    public TrailerRecord getTrailer() { return trailer; }
    public void setTrailer(TrailerRecord v) { this.trailer = v; }
}
