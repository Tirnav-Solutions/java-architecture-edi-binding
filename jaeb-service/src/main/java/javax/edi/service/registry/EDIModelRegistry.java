package javax.edi.service.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
//import javax.edi.model.edifact.delfor.d04a.DelforD04A;
//import javax.edi.model.edifact.delfor.d96a.DelforD96A;
//import javax.edi.model.vda.vda4905.VDA4905Message;
//import javax.edi.model.vda.vda4984.VDA4984Message;
import javax.edi.model.x12.edi810.Invoice;
import javax.edi.model.x12.edi832.PriceSalesCatalog;
import javax.edi.model.x12.edi846.InventoryInquery;
import javax.edi.model.x12.edi850.PurchaseOrder;
import javax.edi.model.x12.edi855.POAcknowledgement;
import javax.edi.model.x12.edi856.AdvanceShipmentNotice;
import javax.edi.model.x12.edi997.FunctionalAcknowledgement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry of all known EDI transaction set types.
 * Maps a human-readable key (e.g. "850", "PurchaseOrder") to the corresponding
 * Java model class so the REST endpoints can resolve types dynamically.
 * 
 * To add a new transaction set:
 *   1. Create the model class with @EDIMessage
 *   2. Register it here in the init() method
 */
@Component
public class EDIModelRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(EDIModelRegistry.class);

    private final Map<String, Class<?>> models = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // X12 transaction sets
        register("850", "PurchaseOrder", PurchaseOrder.class);
        register("810", "Invoice", Invoice.class);
        register("855", "POAcknowledgement", POAcknowledgement.class);
        register("856", "AdvanceShipmentNotice", AdvanceShipmentNotice.class);
        register("997", "FunctionalAcknowledgement", FunctionalAcknowledgement.class);
        register("832", "PriceSalesCatalog", PriceSalesCatalog.class);
        register("846", "InventoryInquery", InventoryInquery.class);

        // EDIFACT message types
//        register("DELFOR-D96A", "DelforD96A", DelforD96A.class);
//        register("DELFOR-D04A", "DelforD04A", DelforD04A.class);
//
//        // VDA fixed-width formats (German automotive)
//        register("VDA4905", "VDA4905Message", VDA4905Message.class);
//        register("VDA4984", "VDA4984Message", VDA4984Message.class);

        LOG.info("EDI Model Registry initialized with {} transaction types: {}",
                models.size(), models.keySet());
    }

    /**
     * Register a model class under one or more aliases.
     */
    private void register(String... aliasesAndClass) {
        // Last element is impossible here -- overload below
    }

    private void register(String code, String name, Class<?> clazz) {
        models.put(code.toUpperCase(), clazz);
        models.put(name.toUpperCase(), clazz);
        // Also register by simple class name
        models.put(clazz.getSimpleName().toUpperCase(), clazz);
    }

    /**
     * Resolve a transaction type key to its model class.
     * Accepts "850", "PurchaseOrder", etc. (case-insensitive).
     * 
     * @return the model class, or null if not found
     */
    public Class<?> resolve(String typeKey) {
        if (typeKey == null) return null;
        return models.get(typeKey.trim().toUpperCase());
    }

    /**
     * Returns all registered type keys.
     */
    public Set<String> getRegisteredKeys() {
        return Collections.unmodifiableSet(models.keySet());
    }

    /**
     * Returns a map of unique transaction codes to their class names.
     */
    public Map<String, String> getSupportedTypes() {
        Map<String, String> result = new LinkedHashMap<>();
        // Deduplicate by class
        Map<Class<?>, String> seen = new LinkedHashMap<>();
        for (Map.Entry<String, Class<?>> entry : models.entrySet()) {
            if (!seen.containsKey(entry.getValue())) {
                seen.put(entry.getValue(), entry.getKey());
            }
        }
        for (Map.Entry<Class<?>, String> entry : seen.entrySet()) {
            result.put(entry.getValue(), entry.getKey().getName());
        }
        return result;
    }
}
