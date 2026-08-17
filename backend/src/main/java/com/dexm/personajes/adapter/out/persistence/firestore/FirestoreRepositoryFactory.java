package com.dexm.personajes.adapter.out.persistence.firestore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.SetOptions;
import com.dexm.personajes.adapter.out.persistence.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Maps the existing repository method vocabulary to Firestore documents. */
@Component
@Profile({"firestore", "local-firestore", "test"})
public class FirestoreRepositoryFactory {
    private static final Logger log = LoggerFactory.getLogger(FirestoreRepositoryFactory.class);
    private final Firestore firestore;
    private final ObjectMapper mapper;

    public FirestoreRepositoryFactory(Firestore firestore, ObjectMapper mapper) {
        this.firestore = firestore;
        this.mapper = mapper;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> type, Class<?> entityType, String collection) {
        InvocationHandler handler = new Handler(entityType, collection);
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private final class Handler implements InvocationHandler {
        private final Class<?> type;
        private final String collection;
        private final Field id;

        Handler(Class<?> type, String collection) {
            this.type = type; this.collection = collection;
            this.id = field(type, "id"); this.id.setAccessible(true);
        }

        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName(); args = args == null ? new Object[0] : args;
            if (name.equals("toString")) return "FirestoreRepository(" + collection + ")";
            if (isAggregateRoot()) return invokeAggregateRoot(name, args);
            if (aggregateField() != null) return invokeAggregate(method, name, args);
            if (name.equals("save")) return save(args[0]);
            if (name.equals("saveAndFlush")) return save(args[0]);
            if (name.equals("flush")) return null;
            if (name.equals("saveAll")) { List<Object> saved = new ArrayList<>(); for (Object item : (Iterable<?>) args[0]) saved.add(save(item)); return saved; }
            if (name.equals("consumeOneGrenade")) return consumeOneGrenade(String.valueOf(args[0]), String.valueOf(args[1]));
            if (name.equals("findById")) return find(String.valueOf(args[0]), name);
            if (name.equals("existsById")) return find(String.valueOf(args[0]), name).isPresent();
            if (name.equals("findAll")) return all(name);
            if (name.equals("count")) { long count = firestore.collection(collection).count().get().get().getCount(); recordRead(collection, "count", 1); return count; }
            if (name.equals("deleteById")) { String key = String.valueOf(args[0]); firestore.collection(collection).document(key).delete().get(); recordDelete(collection, key, 1); return null; }
            if (name.equals("delete")) { String key = String.valueOf(id.get(args[0])); firestore.collection(collection).document(key).delete().get(); recordDelete(collection, key, 1); return null; }
            if (name.equals("deleteAll") && args.length == 0) { for (Object item : all(name)) { String key = String.valueOf(id.get(item)); firestore.collection(collection).document(key).delete().get(); recordDelete(collection, key, 1); } return null; }
            if (name.equals("deleteAll")) { for (Object item : (Iterable<?>) args[0]) { String key = String.valueOf(id.get(item)); firestore.collection(collection).document(key).delete().get(); recordDelete(collection, key, 1); } return null; }
            if (name.startsWith("deleteBy")) { for (Object item : query(name.substring(8), args, false)) { String key = String.valueOf(id.get(item)); firestore.collection(collection).document(key).delete().get(); recordDelete(collection, key, 1); } return null; }
            if (name.startsWith("existsBy")) return !query(name.substring(8), args, true).isEmpty();
            if (name.startsWith("findBy")) {
                List<Object> results = query(name.substring(6), args, false);
                if (Optional.class.isAssignableFrom(method.getReturnType())) return results.stream().findFirst();
                return results;
            }
            throw new UnsupportedOperationException("Firestore repository method not supported: " + method);
        }

        private boolean isAggregateRoot() {
            return collection.equals("characterInventories") || collection.equals("characterActivities");
        }

        private Object invokeAggregateRoot(String name, Object[] args) throws Exception {
            if (name.equals("save") || name.equals("saveAndFlush")) return aggregateRootSave(args[0]);
            if (name.equals("saveAll")) {
                List<Object> saved = new ArrayList<>();
                for (Object item : (Iterable<?>) args[0]) saved.add(aggregateRootSave(item));
                return saved;
            }
            if (name.equals("findById") || name.equals("existsById")) {
                var result = aggregateRootDocument(String.valueOf(args[0]));
                return name.equals("existsById") ? result.isPresent() : result;
            }
            if (name.equals("findAll")) return aggregateRootDocuments();
            if (name.equals("count")) return (long) aggregateRootDocuments().size();
            throw new UnsupportedOperationException("Aggregate root repository method not supported: " + name);
        }

        private Object aggregateRootSave(Object entity) throws Exception {
            String entityId = String.valueOf(id.get(entity));
            String characterId = String.valueOf(field(type, "characterId").get(entity));
            if (!entityId.equals(characterId)) throw new IllegalArgumentException("El agregado requiere id y characterId coincidentes");

            String cacheKey = "aggregate-document:" + collection + ":" + characterId;
            Map<String, Object> document = mapper.convertValue(entity, Map.class);
            document.put("id", characterId);
            document.put("characterId", characterId);
            firestore.collection(collection).document(characterId).set(document).get();
            recordWrite(collection, characterId, 1);
            cacheAggregate(cacheKey, document);
            return entity;
        }

        private Optional<Object> aggregateRootDocument(String entityId) throws Exception {
            String cacheKey = "aggregate-document:" + collection + ":" + entityId;
            Map<String, Object> data = cachedAggregate(cacheKey);
            if (data == null) {
                var snapshot = firestore.collection(collection).document(entityId).get().get();
                recordRead(collection, entityId, 1);
                if (!snapshot.exists() || snapshot.getData() == null) return Optional.empty();
                data = new java.util.HashMap<>(snapshot.getData());
                cacheAggregate(cacheKey, data);
            }
            return Optional.of(convertAggregateRoot(data));
        }

        private List<Object> aggregateRootDocuments() throws Exception {
            var documents = firestore.collection(collection).get().get().getDocuments();
            recordRead(collection, "collection", Math.max(1, documents.size()));
            List<Object> result = new ArrayList<>();
            for (DocumentSnapshot document : documents) {
                if (document.exists() && document.getData() != null) result.add(convertAggregateRoot(document.getData()));
            }
            return result;
        }

        private Object convertAggregateRoot(Object data) throws Exception {
            return mapper.readerFor(type)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(mapper.writeValueAsBytes(data));
        }

        private String aggregateField() {
            return switch (collection) {
                case "weapons" -> "weapons";
                case "ammunition" -> "ammunition";
                case "armors" -> "armors";
                case "shields" -> "shields";
                case "physicalShields" -> "physicalShields";
                case "otherInventoryItems" -> "otherInventoryItems";
                case "trainingActivities" -> "activities";
                case "attributeModifiers" -> "__character.modifiers";
                case "minorAttributeValues" -> "__character.minorAttributeValues";
                default -> null;
            };
        }

        private Object invokeAggregate(Method method, String name, Object[] args) throws Exception {
            if (aggregateField().startsWith("__character.")) return invokeEmbeddedCharacter(method, name, args);
            if (name.equals("save") || name.equals("saveAndFlush")) return aggregateSave(args[0]);
            if (name.equals("saveAll")) { List<Object> result = new ArrayList<>(); for (Object item : (Iterable<?>) args[0]) result.add(aggregateSave(item)); return result; }
            if (name.equals("findById") || name.equals("existsById")) {
                var item = aggregateDocument(String.valueOf(args[0]));
                return name.equals("existsById") ? item.isPresent() : item;
            }
            if (name.equals("findAll")) return aggregateItems(null);
            if (name.equals("count")) return (long) aggregateItems(null).size();
            if (name.equals("deleteById")) throw new UnsupportedOperationException("Los agregados requieren delete(entity) para conocer characterId");
            if (name.equals("delete")) { aggregateDelete(args[0]); return null; }
            if (name.equals("deleteAll") && args.length == 0) {
                String aggregateCollection = collection.equals("trainingActivities") ? "characterActivities" : "characterInventories";
                var documents = firestore.collection(aggregateCollection).get().get().getDocuments();
                recordRead(aggregateCollection, "collection", Math.max(1, documents.size()));
                for (DocumentSnapshot document : documents) { document.getReference().delete().get(); recordDelete(aggregateCollection, document.getId(), 1); }
                return null;
            }
            if (name.equals("deleteAll")) { for (Object item : (Iterable<?>) args[0]) aggregateDelete(item); return null; }
            if (name.startsWith("findBy") || name.startsWith("existsBy") || name.startsWith("deleteBy")) {
                boolean exists = name.startsWith("existsBy"); boolean delete = name.startsWith("deleteBy");
                String expression = name.substring(exists ? 8 : delete ? 8 : 6);
                List<Object> result = aggregateFilter(expression, args);
                if (delete) { result.forEach(item -> { try { aggregateDelete(item); } catch (Exception e) { throw new RuntimeException(e); } }); return null; }
                if (exists) return !result.isEmpty();
                if (Optional.class.isAssignableFrom(method.getReturnType())) return result.stream().findFirst();
                return result;
            }
            throw new UnsupportedOperationException("Aggregate repository method not supported: " + name);
        }

        private Object invokeEmbeddedCharacter(Method method, String name, Object[] args) throws Exception {
            // Embedded Firestore state is written immediately. Keep the
            // JpaRepository contract compatible for callers that used flush()
            // to force ordering between deletes and inserts.
            if (name.equals("flush")) return null;
            String expression = name.startsWith("findBy") ? name.substring(6) : name.startsWith("existsBy") ? name.substring(8) : name.startsWith("deleteBy") ? name.substring(8) : name;
            String characterId = expression.startsWith("CharacterId") && args.length > 0 ? String.valueOf(args[0]) : expression.contains("CharacterId") && args.length > 1 ? String.valueOf(args[1]) : null;
            if (characterId == null && name.equals("delete") && args.length > 0) {
                characterId = String.valueOf(field(type, "characterId").get(args[0]));
            }
            if (name.equals("save") || name.equals("saveAndFlush")) { saveEmbedded(args[0]); return args[0]; }
            if (name.equals("findByCharacterId") || name.equals("findByCharacterIdAndAttributeKey")) {
                List<Object> rows = embeddedRows(characterId);
                if (name.endsWith("AndAttributeKey")) rows = rows.stream().filter(row -> propertyValue(row, "attributeKey", args[1])).toList();
                return rows;
            }
            if (name.equals("findByCharacterIdAndDefinitionId")) return embeddedRows(characterId).stream().filter(row -> propertyValue(row, "definitionId", args[1])).findFirst();
            if (name.equals("deleteByCharacterId")) { replaceEmbedded(characterId, List.of()); return null; }
            if (name.equals("deleteAll")) { deleteEmbeddedRows((Iterable<?>) args[0]); return null; }
            if (name.equals("deleteByCharacterIdAndAttributeKey")) { replaceEmbedded(characterId, embeddedRows(characterId).stream().filter(row -> !propertyValue(row, "attributeKey", args[1])).toList()); return null; }
            if (name.equals("delete")) { String idValue = String.valueOf(id.get(args[0])); var row = embeddedRows(characterId).stream().filter(item -> sameId(item, idValue)).findFirst(); if (row.isPresent()) { var rows = new ArrayList<>(embeddedRows(characterId)); rows.removeIf(item -> sameId(item, idValue)); replaceEmbedded(characterId, rows); } return null; }
            throw new UnsupportedOperationException("Embedded character repository method not supported: " + name);
        }

        private void deleteEmbeddedRows(Iterable<?> values) throws Exception {
            var iterator = values.iterator();
            if (!iterator.hasNext()) return;
            Object first = iterator.next();
            String characterId = String.valueOf(field(type, "characterId").get(first));
            replaceEmbedded(characterId, List.of());
        }

        private List<Object> embeddedRows(String characterId) throws Exception {
            if (characterId == null) return new ArrayList<>();
            CharacterEntity character = readCharacter(characterId);
            if (character == null) return new ArrayList<>();
            Object value = aggregateField().endsWith("modifiers") ? character.getModifiers() : character.getMinorAttributeValues();
            List<Object> rows = new ArrayList<>(); for (Object row : (List<?>) value) rows.add(mapper.convertValue(row, type)); return rows;
        }
        private void saveEmbedded(Object entity) throws Exception {
            String characterId = String.valueOf(field(type, "characterId").get(entity));
            var documentRef = firestore.collection("characters").document(characterId);
            CharacterEntity character = readCharacter(characterId);
            if (character == null) throw new IllegalArgumentException("Personaje no encontrado");
            character.setAggregateVersion(1);
            String entityId = String.valueOf(id.get(entity));
            List<Object> rows = new ArrayList<>(embeddedRows(characterId)); rows.removeIf(row -> sameId(row, entityId)); rows.add(entity);
            if (aggregateField().endsWith("modifiers")) character.setModifiers(rows.stream().map(row -> mapper.convertValue(row, CharacterAttributeModifierEntity.class)).toList());
            else character.setMinorAttributeValues(rows.stream().map(row -> mapper.convertValue(row, CharacterMinorAttributeValueEntity.class)).toList());
            saveCharacter(documentRef, character);
        }
        private void replaceEmbedded(String characterId, List<Object> rows) throws Exception {
            var ref = firestore.collection("characters").document(characterId);
            CharacterEntity character = readCharacter(characterId);
            if (character == null) return;
            character.setAggregateVersion(1);
            if (aggregateField().endsWith("modifiers")) character.setModifiers(rows.stream().map(row -> mapper.convertValue(row, CharacterAttributeModifierEntity.class)).toList());
            else character.setMinorAttributeValues(rows.stream().map(row -> mapper.convertValue(row, CharacterMinorAttributeValueEntity.class)).toList());
            saveCharacter(ref, character);
        }
        private void saveCharacter(com.google.cloud.firestore.DocumentReference ref, CharacterEntity character) throws Exception {
            Map<String,Object> document = mapper.convertValue(character, Map.class); document.put("id", character.getId()); document.put("json", mapper.writeValueAsString(character)); ref.set(document).get();
            recordWrite("characters", character.getId(), 1);
            requestCache().put("characters:" + character.getId(), Optional.of(character));
        }
        private CharacterEntity readCharacter(String characterId) throws Exception {
            String cacheKey = "characters:" + characterId;
            var cache = requestCache();
            var cached = cache.get(cacheKey);
            if (cached instanceof Optional<?> optional) return optional.orElse(null) instanceof CharacterEntity c ? c : null;
            var document = firestore.collection("characters").document(characterId).get().get();
            recordRead("characters", characterId, 1);
            if (!document.exists()) { cache.put(cacheKey, Optional.empty()); return null; }
            var character = mapper.readValue(document.getString("json"), CharacterEntity.class);
            cache.put(cacheKey, Optional.of(character)); return character;
        }

        private Object aggregateSave(Object entity) throws Exception {
            String entityId = String.valueOf(id.get(entity));
            String characterId = String.valueOf(field(type, "characterId").get(entity));
            List<Object> items = aggregateItems(characterId);
            for (var iterator = items.iterator(); iterator.hasNext();) {
                if (entityId.equals(String.valueOf(id.get(iterator.next())))) iterator.remove();
            }
            items.add(entity);
            aggregateWrite(items);
            return entity;
        }

        private void aggregateDelete(Object entity) throws Exception {
            String entityId = String.valueOf(id.get(entity));
            String characterId = String.valueOf(field(type, "characterId").get(entity));
            List<Object> items = aggregateItems(characterId);
            if (!items.removeIf(item -> sameId(item, entityId))) return;
            aggregateWriteForCharacter(characterId, items);
        }

        private boolean sameId(Object item, String entityId) { try { return entityId.equals(String.valueOf(id.get(item))); } catch (IllegalAccessException e) { return false; } }

        private List<Object> aggregateFilter(String expression, Object[] values) throws Exception {
            String order = null; int orderIndex = expression.indexOf("OrderBy");
            if (orderIndex >= 0) { order = expression.substring(orderIndex + 7); expression = expression.substring(0, orderIndex); }
            expression = expression.replace("ForUpdate", "");
            String[] parts = expression.split("And"); int valueIndex = 0;
            String characterId = expression.startsWith("CharacterId") && values.length > 0 ? String.valueOf(values[0])
                    : expression.contains("CharacterId") && values.length > 1 ? String.valueOf(values[1]) : null;
            List<Object> result = aggregateItems(characterId);
            for (String part : parts) {
                boolean trueValue = part.endsWith("True"), nullValue = part.endsWith("IsNull");
                String property = decap(part.replace("True", "").replace("IsNull", ""));
                Object wanted = trueValue ? Boolean.TRUE : nullValue ? null : values[valueIndex++];
                result = result.stream().filter(item -> propertyValue(item, property, wanted)).toList();
            }
            if (order != null && order.contains("StartAgeAsc")) result = result.stream().sorted(Comparator.comparing(item -> numberProperty(item, "startAge"))).toList();
            if (order != null && order.contains("PriorityAsc")) result = result.stream().sorted(Comparator.comparing(item -> numberProperty(item, "priority"))).toList();
            return result;
        }

        private boolean propertyValue(Object item, String property, Object wanted) {
            try { Object actual = field(type, property).get(item); return java.util.Objects.equals(actual, wanted); }
            catch (Exception ignored) { return false; }
        }
        private Integer numberProperty(Object item, String property) { try { return ((Number) field(type, property).get(item)).intValue(); } catch (Exception e) { return 0; } }

        private List<Object> aggregateItems(String characterId) throws Exception {
            if (characterId == null) return new ArrayList<>();
            String aggregateCollection = collection.equals("trainingActivities") ? "characterActivities" : "characterInventories";
            String cacheKey = "aggregate-document:" + aggregateCollection + ":" + characterId;
            Map<String, Object> document = cachedAggregate(cacheKey);
            if (document == null) {
                var snapshot = firestore.collection(aggregateCollection).document(characterId).get().get();
                recordRead(aggregateCollection, characterId, 1);
                if (!snapshot.exists()) {
                    cacheAggregate(cacheKey, Map.of());
                    return new ArrayList<>();
                }
                document = snapshot.getData() == null ? new java.util.HashMap<>() : new java.util.HashMap<>(snapshot.getData());
                cacheAggregate(cacheKey, document);
            }
            Object raw = document.get(aggregateField());
            if (!(raw instanceof List<?> list)) return new ArrayList<>();
            List<Object> result = new ArrayList<>(); for (Object item : list) result.add(convertAggregateItem(item));
            return result;
        }

        private void aggregateWrite(List<Object> items) throws Exception {
            String characterId = items.isEmpty() ? null : String.valueOf(field(type, "characterId").get(items.get(0)));
            if (characterId == null || "null".equals(characterId)) throw new IllegalArgumentException("El agregado requiere characterId");
            aggregateWriteForCharacter(characterId, items);
        }

        private void aggregateWriteForCharacter(String characterId, List<Object> items) throws Exception {
            String aggregateCollection = collection.equals("trainingActivities") ? "characterActivities" : "characterInventories";
            var ref = firestore.collection(aggregateCollection).document(characterId);
            String cacheKey = "aggregate-document:" + aggregateCollection + ":" + characterId;
            Map<String, Object> cached = cachedAggregate(cacheKey);
            Map<String, Object> document;
            if (cached != null) {
                document = new java.util.HashMap<>(cached);
            } else {
                var snapshot = ref.get().get();
                recordRead(aggregateCollection, characterId, 1);
                document = snapshot.exists() && snapshot.getData() != null
                        ? new java.util.HashMap<>(snapshot.getData())
                        : new java.util.HashMap<>();
            }
            document.put("id", characterId);
            document.put("characterId", characterId);
            // Firestore accepts maps/lists of supported values here, but not
            // arbitrary entity instances nested inside a list. Reads decode
            // aggregate items into entities, so convert them back before the
            // write (otherwise delete/update can fail with gRPC INVALID_ARGUMENT).
            document.put(aggregateField(), mapper.convertValue(items, List.class));
            // Only merge the canonical aggregate fields. Rewriting the entire
            // legacy document also re-sends its redundant `json` snapshot and
            // can make Firestore reject an otherwise valid activity deletion.
            Map<String, Object> payload = Map.of(
                    "id", characterId,
                    "characterId", characterId,
                    aggregateField(), document.get(aggregateField()));
            ref.set(payload, SetOptions.merge()).get();
            recordWrite(aggregateCollection, characterId, 1);
            cacheAggregate(cacheKey, document);
        }

        /**
         * Aggregate documents outlive application revisions. A nested item may
         * therefore contain fields unknown to the current entity class. That
         * schema drift must not make an otherwise valid level-up fail while the
         * item is being read to create a history snapshot.
         */
        private Object convertAggregateItem(Object item) throws Exception {
            return mapper.readerFor(type)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(mapper.writeValueAsBytes(item));
        }

        private Optional<Object> aggregateDocument(String entityId) throws Exception {
            String aggregateCollection = collection.equals("trainingActivities") ? "characterActivities" : "characterInventories";
            String cacheKey = "aggregate-document:" + aggregateCollection + ":" + entityId;
            Map<String, Object> cached = cachedAggregate(cacheKey);
            var document = cached == null ? firestore.collection(aggregateCollection).document(entityId).get().get() : null;
            if (cached == null) recordRead(aggregateCollection, entityId, 1);
            Map<String, Object> data = cached != null ? cached : (document.exists() ? document.getData() : null);
            if (data == null) return Optional.empty();
            cacheAggregate(cacheKey, data);
            Object raw = data.get(aggregateField());
            if (!(raw instanceof List<?> list)) return Optional.empty();
            for (Object item : list) {
                Object decoded = convertAggregateItem(item);
                if (entityId.equals(String.valueOf(id.get(decoded)))) return Optional.of(decoded);
            }
            return Optional.empty();
        }

        private Object save(Object entity) throws Exception {
            String entityId = String.valueOf(id.get(entity));
            Map<String, Object> document = new java.util.HashMap<>();
            document.put("json", mapper.writeValueAsString(entity));
            document.put("id", entityId);
            Map<String, Object> fields = mapper.convertValue(entity, Map.class);
            fields.forEach((key, value) -> { if (value == null || isFirestoreScalar(value)) document.put(key, value); });
            firestore.collection(collection).document(entityId).set(document).get();
            recordWrite(collection, entityId, 1);
            return entity;
        }
        private Object consumeOneGrenade(String entityId, String characterId) throws Exception {
            var reference = firestore.collection(collection).document(entityId);
            return firestore.runTransaction(transaction -> {
                var document = transaction.get(reference).get();
                recordRead(collection, entityId, 1);
                if (!document.exists()) return null;
                var entity = mapper.readValue(document.getString("json"), type);
                var character = field(type, "characterId").get(entity);
                var ammunitionType = field(type, "type").get(entity);
                var grenadeCatalogId = field(type, "grenadeCatalogId").get(entity);
                var quantity = ((Number) field(type, "quantity").get(entity)).intValue();
                if (!characterId.equals(character) || !"GRENADE".equals(ammunitionType) || quantity < 1 || !isHandGrenade(transaction, String.valueOf(grenadeCatalogId))) return null;
                field(type, "quantity").set(entity, quantity - 1);
                if (quantity == 1) { transaction.delete(reference); recordDelete(collection, entityId, 1); }
                else { transaction.set(reference, Map.of("json", mapper.writeValueAsString(entity), "id", entityId)); recordWrite(collection, entityId, 1); }
                return entity;
            }).get();
        }
        private boolean isHandGrenade(com.google.cloud.firestore.Transaction transaction, String grenadeCatalogId) throws Exception {
            if (grenadeCatalogId == null || "null".equals(grenadeCatalogId)) return false;
            var catalogDocument = transaction.get(firestore.collection("grenadeCatalog").document(grenadeCatalogId)).get();
            recordRead("grenadeCatalog", grenadeCatalogId, 1);
            if (!catalogDocument.exists()) return false;
            var catalogJson = mapper.readValue(catalogDocument.getString("json"), Map.class);
            return Boolean.TRUE.equals(catalogJson.get("handGrenade"));
        }
        private Optional<Object> find(String entityId, String operation) throws Exception {
            Map<String, Object> cache = requestCache();
            String cacheKey = collection + ":" + entityId;
            if (cache.containsKey(cacheKey)) return (Optional<Object>) cache.get(cacheKey);
            Optional<Object> result = decode(firestore.collection(collection).document(entityId).get().get());
            recordRead(collection, entityId, 1);
            cache.put(cacheKey, result);
            return result;
        }
        private List<Object> all(String operation) throws Exception {
            List<Object> items = new ArrayList<>();
            var documents = firestore.collection(collection).get().get().getDocuments();
            recordRead(collection, "collection", Math.max(1, documents.size()));
            for (DocumentSnapshot doc : documents) decode(doc).ifPresent(items::add);
            return items;
        }
        private Optional<Object> decode(DocumentSnapshot document) throws Exception { if (!document.exists()) return Optional.empty(); return Optional.of(mapper.readValue(document.getString("json"), type)); }
        private List<Object> query(String expression, Object[] values, boolean limitOne) throws Exception {
            String cacheKey = "query:" + collection + ":" + expression + ":" + limitOne + ":" + java.util.Arrays.deepToString(values);
            Object cachedResult = requestCache().get(cacheKey);
            if (cachedResult instanceof List<?> list) return new ArrayList<>(list);
            String order = null; int orderIndex = expression.indexOf("OrderBy");
            if (orderIndex >= 0) { order = expression.substring(orderIndex + 7); expression = expression.substring(0, orderIndex); }
            expression = expression.replace("ForUpdate", "");
            Query query = firestore.collection(collection); String[] parts = expression.split("And");
            int valueIndex = 0;
            for (int index = 0; index < parts.length; index++) {
                String part = parts[index]; boolean trueValue = part.endsWith("True"), nullValue = part.endsWith("IsNull");
                String property = decap(part.replace("True", "").replace("IsNull", "")); Object wanted = trueValue ? Boolean.TRUE : nullValue ? null : values[valueIndex++];
                query = query.whereEqualTo(property, wanted);
            }
            if (order != null) {
                Matcher matcher = Pattern.compile("([A-Z][a-zA-Z0-9]*?)(Asc|Desc)(?=[A-Z]|$)").matcher(order);
                int parsedUntil = 0;
                while (matcher.find()) {
                    if (matcher.start() != parsedUntil) throw new IllegalArgumentException("Invalid Firestore order expression " + order);
                    String property = decap(matcher.group(1));
                    query = query.orderBy(property, matcher.group(2).equals("Desc") ? Query.Direction.DESCENDING : Query.Direction.ASCENDING);
                    parsedUntil = matcher.end();
                }
                if (parsedUntil != order.length()) throw new IllegalArgumentException("Invalid Firestore order expression " + order);
            }
            if (limitOne) query = query.limit(1);
            List<Object> results = new ArrayList<>();
            var documents = query.get().get().getDocuments();
            recordRead(collection, expression, Math.max(1, documents.size()));
            for (DocumentSnapshot document : documents) decode(document).ifPresent(results::add);
            requestCache().put(cacheKey, new ArrayList<>(results));
            return results;
        }
        private boolean isFirestoreScalar(Object value) { return value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof java.util.Date; }
        @SuppressWarnings("unchecked")
        private Map<String, Object> requestCache() {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes == null) return new java.util.HashMap<>();
            String key = FirestoreRepositoryFactory.class.getName() + ".read-cache";
            Object existing = attributes.getAttribute(key, RequestAttributes.SCOPE_REQUEST);
            if (existing instanceof Map<?, ?> map) return (Map<String, Object>) map;
            Map<String, Object> created = new java.util.HashMap<>();
            attributes.setAttribute(key, created, RequestAttributes.SCOPE_REQUEST);
            return created;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> cachedAggregate(String key) {
            Object value = requestCache().get(key);
            return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
        }

        private void cacheAggregate(String key, Map<String, Object> document) {
            requestCache().put(key, new java.util.HashMap<>(document));
        }

        private void recordRead(String collectionName, String documentId, int count) {
            recordUsage("read", collectionName, documentId, count);
        }

        private void recordWrite(String collectionName, String documentId, int count) {
            recordUsage("write", collectionName, documentId, count);
        }

        private void recordDelete(String collectionName, String documentId, int count) {
            recordUsage("delete", collectionName, documentId, count);
        }

        private void recordUsage(String operation, String collectionName, String documentId, int count) {
            Map<String, Object> cache = requestCache();
            String totalKey = "firestore-usage:" + operation;
            long total = ((Number) cache.getOrDefault(totalKey, 0L)).longValue() + count;
            cache.put(totalKey, total);
            var attributes = RequestContextHolder.getRequestAttributes();
            String request = attributes instanceof ServletRequestAttributes servlet
                    ? servlet.getRequest().getMethod() + " " + servlet.getRequest().getRequestURI()
                    : "non-http";
            log.info("firestore_usage operation={} collection={} document={} count={} total_{}={} request={}",
                    operation, collectionName, documentId, count, operation, total, request);
        }
    }
    private static Field field(Class<?> type, String name) {
        try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (NoSuchFieldException exception) { throw new IllegalArgumentException("Unknown Firestore property " + name + " for " + type.getSimpleName(), exception); }
    }
    private static String decap(String name) { return name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1); }
}
