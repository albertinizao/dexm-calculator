package com.dexm.personajes.adapter.out.persistence.firestore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.dexm.personajes.adapter.out.persistence.FirestoreRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
            if (name.equals("save")) return save(args[0]);
            if (name.equals("saveAndFlush")) return save(args[0]);
            if (name.equals("flush")) return null;
            if (name.equals("saveAll")) { List<Object> saved = new ArrayList<>(); for (Object item : (Iterable<?>) args[0]) saved.add(save(item)); return saved; }
            if (name.equals("findById")) return find(String.valueOf(args[0]));
            if (name.equals("existsById")) return find(String.valueOf(args[0])).isPresent();
            if (name.equals("findAll")) return all();
            if (name.equals("count")) return (long) all().size();
            if (name.equals("deleteById")) { firestore.collection(collection).document(String.valueOf(args[0])).delete().get(); return null; }
            if (name.equals("delete")) { firestore.collection(collection).document(String.valueOf(id.get(args[0]))).delete().get(); return null; }
            if (name.equals("deleteAll") && args.length == 0) { for (Object item : all()) firestore.collection(collection).document(String.valueOf(id.get(item))).delete().get(); return null; }
            if (name.equals("deleteAll")) { for (Object item : (Iterable<?>) args[0]) firestore.collection(collection).document(String.valueOf(id.get(item))).delete().get(); return null; }
            if (name.startsWith("deleteBy")) { for (Object item : query(name.substring(8), args)) firestore.collection(collection).document(String.valueOf(id.get(item))).delete().get(); return null; }
            if (name.startsWith("existsBy")) return !query(name.substring(8), args).isEmpty();
            if (name.startsWith("findBy")) {
                List<Object> results = query(name.substring(6), args);
                if (Optional.class.isAssignableFrom(method.getReturnType())) return results.stream().findFirst();
                return results;
            }
            throw new UnsupportedOperationException("Firestore repository method not supported: " + method);
        }

        private Object save(Object entity) throws Exception {
            String entityId = String.valueOf(id.get(entity));
            firestore.collection(collection).document(entityId).set(Map.of("json", mapper.writeValueAsString(entity), "id", entityId)).get();
            return entity;
        }
        private Optional<Object> find(String entityId) throws Exception { return decode(firestore.collection(collection).document(entityId).get().get()); }
        private List<Object> all() throws Exception { List<Object> items = new ArrayList<>(); for (DocumentSnapshot doc : firestore.collection(collection).get().get().getDocuments()) decode(doc).ifPresent(items::add); return items; }
        private Optional<Object> decode(DocumentSnapshot document) throws Exception { if (!document.exists()) return Optional.empty(); return Optional.of(mapper.readValue(document.getString("json"), type)); }
        private List<Object> query(String expression, Object[] values) throws Exception {
            String order = null; int orderIndex = expression.indexOf("OrderBy");
            if (orderIndex >= 0) { order = expression.substring(orderIndex + 7); expression = expression.substring(0, orderIndex); }
            expression = expression.replace("ForUpdate", "");
            List<Object> results = all(); String[] parts = expression.split("And");
            int valueIndex = 0;
            for (int index = 0; index < parts.length; index++) {
                String part = parts[index]; boolean trueValue = part.endsWith("True"), nullValue = part.endsWith("IsNull");
                String property = decap(part.replace("True", "").replace("IsNull", "")); Object wanted = trueValue ? Boolean.TRUE : nullValue ? null : values[valueIndex++];
                results.removeIf(item -> { try { return !java.util.Objects.equals(field(type, property).get(item), wanted); } catch (Exception exception) { throw new IllegalStateException(exception); } });
            }
            if (order != null) { boolean ascending = order.endsWith("Asc"); String property = decap(order.replace("Asc", "").replace("Desc", "")); Comparator<Object> c = Comparator.comparing(item -> (Comparable) value(item, property), Comparator.nullsLast(Comparator.naturalOrder())); results.sort(ascending ? c : c.reversed()); }
            return results;
        }
        private Object value(Object item, String property) { try { return field(type, property).get(item); } catch (Exception exception) { throw new IllegalStateException(exception); } }
    }
    private static Field field(Class<?> type, String name) {
        try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (NoSuchFieldException exception) { throw new IllegalArgumentException("Unknown Firestore property " + name + " for " + type.getSimpleName(), exception); }
    }
    private static String decap(String name) { return name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1); }
}
