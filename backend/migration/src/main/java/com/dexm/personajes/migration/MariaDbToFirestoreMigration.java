package com.dexm.personajes.migration;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One-shot, idempotent export. ADC is used for Firestore; database credentials stay in environment variables. */
public final class MariaDbToFirestoreMigration {
    private static final List<Table> TABLES = List.of(
            new Table("users", "users"), new Table("campaigns", "campaigns"), new Table("campaign_invitations", "campaignInvitations"),
            new Table("characters", "characters"), new Table("character_milestones", "milestones"), new Table("character_attribute_modifiers", "attributeModifiers"),
            new Table("character_minor_attribute_values", "minorAttributeValues"), new Table("minor_attribute_definitions", "minorAttributeDefinitions"),
            new Table("training_activities", "trainingActivities"), new Table("other_inventory_items", "otherInventoryItems"), new Table("ammunition_inventory", "ammunition"),
            new Table("weapon_inventory", "weapons"), new Table("armor_inventory", "armors"), new Table("shield_inventory", "shields"),
            new Table("physical_shield_inventory", "physicalShields"), new Table("abilities", "abilities"), new Table("weapon_catalog", "weaponCatalog"),
            new Table("armor_catalog", "armorCatalog"), new Table("shield_catalog", "shieldCatalog"), new Table("physical_shield_catalog", "physicalShieldCatalog"));

    public static void main(String[] arguments) throws Exception {
        String url = required("DB_URL"), username = required("DB_USERNAME"), password = required("DB_PASSWORD");
        try (Firestore firestore = FirestoreOptions.getDefaultInstance().getService()) {
            for (Table table : TABLES) migrate(firestore, url, username, password, table);
        }
    }

    private static void migrate(Firestore firestore, String url, String username, String password, Table table) throws Exception {
        long count = 0;
        try (var connection = DriverManager.getConnection(url, username, password); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM " + table.source())) {
            while (rows.next()) {
                Map<String, Object> payload = row(rows);
                String id = String.valueOf(payload.get("id"));
                if (id.isBlank() || "null".equals(id)) throw new IllegalStateException(table.source() + " row has no id");
                // Deterministic collection/id + set() means a rerun overwrites, never duplicates.
                firestore.collection(table.target()).document(id).set(Map.of("payload", payload)).get();
                count++;
            }
        }
        System.out.printf("%s -> %s: %d documents%n", table.source(), table.target(), count);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " must be set");
        return value;
    }

    private static Map<String, Object> row(ResultSet rows) throws Exception {
        ResultSetMetaData metadata = rows.getMetaData(); Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            Object value = rows.getObject(i);
            if (value instanceof java.sql.Timestamp timestamp) value = timestamp.toInstant().toString();
            if (value instanceof java.sql.Date date) value = date.toLocalDate().toString();
            if (value instanceof BigDecimal decimal) value = decimal.toPlainString();
            result.put(metadata.getColumnLabel(i), value);
        }
        return result;
    }

    private record Table(String source, String target) { }
}
