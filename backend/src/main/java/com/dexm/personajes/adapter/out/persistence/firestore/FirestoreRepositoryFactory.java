package com.dexm.personajes.adapter.out.persistence.firestore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.dexm.personajes.adapter.out.persistence.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;

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
            if (aggregateField() != null) return invokeAggregate(method, name, args);
            if (name.equals("save")) return save(args[0]);
            if (name.equals("saveAndFlush")) return save(args[0]);
            if (name.equals("flush")) return null;
            if (name.equals("saveAll")) { List<Object> saved = new ArrayList<>(); for (Object item : (Iterable<?>) args[0]) saved.add(save(item)); return saved; }
            if (name.equals("consumeOneGrenade")) return consumeOneGrenade(String.valueOf(args[0]), String.valueOf(args[1]));
            if (name.equals("findById")) return find(String.valueOf(args[0]), name);
            if (name.equals("existsById")) return find(String.valueOf(args[0]), name).isPresent();
            if (name.equals("findAll")) return all(name);
            if (name.equals("count")) return firestore.collection(collection).count().get().get().getCount();
            if (name.equals("deleteById")) { firestore.collection(collection).document(String.valueOf(args[0])).delete().get(); return null; }
            if (name.equals("delete")) { firestore.collection(collection).document(String.valueOf(id.get(args[0]))).delete().get(); return null; }
            if (name.equals("deleteAll") && args.length == 0) { for (Object item : all(name)) firestore.collection(collection).document(String.valueOf(id.get(item))).delete().get(); return null; }
            if (name.equals("deleteAll")) { for (Object item : (Iterable<?>) args[0]) firestore.collection(collection).document(String.valueOf(id.get(item))).delete().get(); return null; }
            if (name.startsWith("deleteBy")) { for (Object item : query(name.substring(8), args, false)) firestore.collection(collection).document(String.valueOf(id.get(item))).delete().get(); return null; }
            if (name.startsWith("existsBy")) return !query(name.substring(8), args, true).isEmpty();
            if (name.startsWith("findBy")) {
                List<Object> results = query(name.substring(6), args, false);
                if (Optional.class.isAssignableFrom(method.getReturnType())) return results.stream().findFirst();
                return results;
            }
            throw new UnsupportedOperationException("Firestore repository method not supported: " + method);
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
            if (name.equals("deleteById")) { aggregateDelete(String.valueOf(args[0])); return null; }
            if (name.equals("delete")) { aggregateDelete(String.valueOf(id.get(args[0]))); return null; }
            if (name.equals("deleteAll") && args.length == 0) {
                String aggregateCollection = collection.equals("trainingActivities") ? "characterActivities" : "characterInventories";
                for (DocumentSnapshot document : firestore.collection(aggregateCollection).get().get().getDocuments()) document.getReference().delete().get();
                return null;
            }
            if (name.equals("deleteAll")) { for (Object item : (Iterable<?>) args[0]) aggregateDelete(String.valueOf(id.get(item))); return null; }
            if (name.startsWith("findBy") || name.startsWith("existsBy") || name.startsWith("deleteBy")) {
                boolean exists = name.startsWith("existsBy"); boolean delete = name.startsWith("deleteBy");
                String expression = name.substring(exists ? 8 : delete ? 8 : 6);
                List<Object> result = aggregateFilter(expression, args);
                if (delete) { result.forEach(item -> { try { aggregateDelete(String.valueOf(id.get(item))); } catch (Exception e) { throw new RuntimeException(e); } }); return null; }
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
            if (name.equals("findByDefinitionId")) return embeddedRowsForDefinition(String.valueOf(args[0]));
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
            if (character.getAggregateVersion() < 1) return legacyItems(characterId);
            Object value = aggregateField().endsWith("modifiers") ? character.getModifiers() : character.getMinorAttributeValues();
            List<Object> rows = new ArrayList<>(); for (Object row : (List<?>) value) rows.add(mapper.convertValue(row, type)); return rows;
        }
        private List<Object> embeddedRowsForDefinition(String definitionId) throws Exception {
            List<Object> result = new ArrayList<>();
            for (DocumentSnapshot doc : firestore.collection("characters").get().get().getDocuments()) if (doc.exists()) {
                CharacterEntity c = mapper.readValue(doc.getString("json"), CharacterEntity.class);
                if (c.getAggregateVersion() >= 1) for (Object row : aggregateField().endsWith("modifiers") ? c.getModifiers() : c.getMinorAttributeValues()) if (propertyValue(row, "definitionId", definitionId)) result.add(mapper.convertValue(row, type));
            }
            return result;
        }
        private void saveEmbedded(Object entity) throws Exception {
            String characterId = String.valueOf(field(type, "characterId").get(entity));
            var documentRef = firestore.collection("characters").document(characterId); var document = documentRef.get().get();
            if (!document.exists()) throw new IllegalArgumentException("Personaje no encontrado");
            CharacterEntity character = mapper.readValue(document.getString("json"), CharacterEntity.class); character.setAggregateVersion(1);
            String entityId = String.valueOf(id.get(entity));
            List<Object> rows = new ArrayList<>(embeddedRows(characterId)); rows.removeIf(row -> sameId(row, entityId)); rows.add(entity);
            if (aggregateField().endsWith("modifiers")) character.setModifiers(rows.stream().map(row -> mapper.convertValue(row, CharacterAttributeModifierEntity.class)).toList());
            else character.setMinorAttributeValues(rows.stream().map(row -> mapper.convertValue(row, CharacterMinorAttributeValueEntity.class)).toList());
            saveCharacter(documentRef, character);
        }
        private void replaceEmbedded(String characterId, List<Object> rows) throws Exception {
            var ref = firestore.collection("characters").document(characterId); var document = ref.get().get(); if (!document.exists()) return;
            CharacterEntity character = mapper.readValue(document.getString("json"), CharacterEntity.class); character.setAggregateVersion(1);
            if (aggregateField().endsWith("modifiers")) character.setModifiers(rows.stream().map(row -> mapper.convertValue(row, CharacterAttributeModifierEntity.class)).toList());
            else character.setMinorAttributeValues(rows.stream().map(row -> mapper.convertValue(row, CharacterMinorAttributeValueEntity.class)).toList());
            saveCharacter(ref, character);
        }
        private void saveCharacter(com.google.cloud.firestore.DocumentReference ref, CharacterEntity character) throws Exception {
            Map<String,Object> document = mapper.convertValue(character, Map.class); document.put("id", character.getId()); document.put("json", mapper.writeValueAsString(character)); ref.set(document).get();
            requestCache().put("characters:" + character.getId(), Optional.of(character));
        }
        private CharacterEntity readCharacter(String characterId) throws Exception {
            String cacheKey = "characters:" + characterId;
            var cache = requestCache();
            var cached = cache.get(cacheKey);
            if (cached != null) return cached.orElse(null) instanceof CharacterEntity c ? c : null;
            var document = firestore.collection("characters").document(characterId).get().get();
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

        private void aggregateDelete(String entityId) throws Exception {
            String aggregateCollection = collection.equals("trainingActivities") ? "characterActivities" : "characterInventories";
            for (DocumentSnapshot document : firestore.collection(aggregateCollection).get().get().getDocuments()) {
                if (!document.exists()) continue;
                Object raw = document.getData() == null ? null : document.getData().get(aggregateField());
                if (!(raw instanceof List<?> list)) continue;
                List<Object> items = new ArrayList<>();
                for (Object item : list) items.add(mapper.convertValue(item, type));
                if (items.removeIf(item -> sameId(item, entityId))) aggregateWriteForCharacter(document.getId(), items);
            }
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
            var ref = firestore.collection("characterInventories").document(characterId);
            if (collection.equals("trainingActivities")) ref = firestore.collection("characterActivities").document(characterId);
            var snapshot = ref.get().get();
            if (!snapshot.exists()) return legacyItems(characterId);
            Map<String, Object> document = snapshot.getData(); Object raw = document == null ? null : document.get(aggregateField());
            if (!(raw instanceof List<?> list)) return new ArrayList<>();
            List<Object> result = new ArrayList<>(); for (Object item : list) result.add(mapper.convertValue(item, type));
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
            var snapshot = ref.get().get();
            Map<String, Object> document = snapshot.exists() && snapshot.getData() != null
                    ? new java.util.HashMap<>(snapshot.getData())
                    : new java.util.HashMap<>();
            document.put("id", characterId);
            document.put("characterId", characterId);
            document.put(aggregateField(), items);
            document.put("json", mapper.writeValueAsString(document)); ref.set(document).get();
        }

        private List<Object> legacyItems(String requestedId) throws Exception {
            List<Object> items = new ArrayList<>();
            var documents = firestore.collection(collection).get().get().getDocuments();
            for (DocumentSnapshot document : documents) decode(document).ifPresent(items::add);
            if (requestedId != null) items = items.stream().filter(item -> { try { return requestedId.equals(String.valueOf(field(type, "characterId").get(item))); } catch (Exception e) { return false; } }).toList();
            return new ArrayList<>(items);
        }

        private Optional<Object> legacyDocument(String entityId) throws Exception {
            var document = firestore.collection(collection).document(entityId).get().get();
            return decode(document);
        }

        private Optional<Object> aggregateDocument(String entityId) throws Exception {
            var legacy = legacyDocument(entityId);
            if (legacy.isPresent()) return legacy;
            String aggregateCollection = collection.equals("trainingActivities") ? "characterActivities" : "characterInventories";
            for (DocumentSnapshot document : firestore.collection(aggregateCollection).get().get().getDocuments()) {
                Object raw = document.getData() == null ? null : document.getData().get(aggregateField());
                if (!(raw instanceof List<?> list)) continue;
                for (Object item : list) {
                    Object decoded = mapper.convertValue(item, type);
                    if (entityId.equals(String.valueOf(id.get(decoded)))) return Optional.of(decoded);
                }
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
            return entity;
        }
        private Object consumeOneGrenade(String entityId, String characterId) throws Exception {
            var reference = firestore.collection(collection).document(entityId);
            return firestore.runTransaction(transaction -> {
                var document = transaction.get(reference).get();
                if (!document.exists()) return null;
                var entity = mapper.readValue(document.getString("json"), type);
                var character = field(type, "characterId").get(entity);
                var ammunitionType = field(type, "type").get(entity);
                var grenadeCatalogId = field(type, "grenadeCatalogId").get(entity);
                var quantity = ((Number) field(type, "quantity").get(entity)).intValue();
                if (!characterId.equals(character) || !"GRENADE".equals(ammunitionType) || quantity < 1 || !isHandGrenade(transaction, String.valueOf(grenadeCatalogId))) return null;
                field(type, "quantity").set(entity, quantity - 1);
                if (quantity == 1) transaction.delete(reference);
                else transaction.set(reference, Map.of("json", mapper.writeValueAsString(entity), "id", entityId));
                return entity;
            }).get();
        }
        private boolean isHandGrenade(com.google.cloud.firestore.Transaction transaction, String grenadeCatalogId) throws Exception {
            if (grenadeCatalogId == null || "null".equals(grenadeCatalogId)) return false;
            var catalogDocument = transaction.get(firestore.collection("grenadeCatalog").document(grenadeCatalogId)).get();
            if (!catalogDocument.exists()) return false;
            var catalogJson = mapper.readValue(catalogDocument.getString("json"), Map.class);
            return Boolean.TRUE.equals(catalogJson.get("handGrenade"));
        }
        private Optional<Object> find(String entityId, String operation) throws Exception {
            Map<String, Optional<Object>> cache = requestCache();
            String cacheKey = collection + ":" + entityId;
            if (cache.containsKey(cacheKey)) return cache.get(cacheKey);
            Optional<Object> result = decode(firestore.collection(collection).document(entityId).get().get());
            cache.put(cacheKey, result);
            return result;
        }
        private List<Object> all(String operation) throws Exception {
            List<Object> items = new ArrayList<>();
            var documents = firestore.collection(collection).get().get().getDocuments();
            for (DocumentSnapshot doc : documents) decode(doc).ifPresent(items::add);
            return items;
        }
        private Optional<Object> decode(DocumentSnapshot document) throws Exception { if (!document.exists()) return Optional.empty(); return Optional.of(mapper.readValue(document.getString("json"), type)); }
        private List<Object> query(String expression, Object[] values, boolean limitOne) throws Exception {
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
            for (DocumentSnapshot document : query.get().get().getDocuments()) decode(document).ifPresent(results::add);
            return results;
        }
        private boolean isFirestoreScalar(Object value) { return value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof java.util.Date; }
        @SuppressWarnings("unchecked")
        private Map<String, Optional<Object>> requestCache() {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes == null) return new java.util.HashMap<>();
            String key = FirestoreRepositoryFactory.class.getName() + ".read-cache";
            Object existing = attributes.getAttribute(key, RequestAttributes.SCOPE_REQUEST);
            if (existing instanceof Map<?, ?> map) return (Map<String, Optional<Object>>) map;
            Map<String, Optional<Object>> created = new java.util.HashMap<>();
            attributes.setAttribute(key, created, RequestAttributes.SCOPE_REQUEST);
            return created;
        }
    }
    private static Field field(Class<?> type, String name) {
        try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (NoSuchFieldException exception) { throw new IllegalArgumentException("Unknown Firestore property " + name + " for " + type.getSimpleName(), exception); }
    }
    private static String decap(String name) { return name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1); }
}
