package com.dexm.personajes.adapter.out.persistence.firestore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** One-shot migration for documents written before fields were materialized at top level. */
@Component
@ConditionalOnProperty(name = "app.firestore.backfill.enabled", havingValue = "true")
public class FirestoreIndexBackfill {
    private static final Logger log = LoggerFactory.getLogger(FirestoreIndexBackfill.class);
    private final Firestore firestore;
    private final ObjectMapper mapper;

    public FirestoreIndexBackfill(Firestore firestore, ObjectMapper mapper) {
        this.firestore = firestore;
        this.mapper = mapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void run() throws Exception {
        long[] updated = {0};
        for (var collection : firestore.listCollections()) {
            for (var document : collection.get().get().getDocuments()) {
                String json = document.getString("json");
                if (json == null || json.isBlank()) continue;
                Map<String, Object> entity = mapper.readValue(json, new TypeReference<>() {});
                Map<String, Object> indexed = new HashMap<>();
                indexed.put("id", document.getId());
                entity.forEach((key, value) -> {
                    if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean)
                        indexed.put(key, value);
                });
                document.getReference().set(indexed, SetOptions.merge()).get();
                updated[0]++;
            }
        }
        log.info("Firestore index backfill completed; documentsUpdated={}", updated[0]);
    }
}
