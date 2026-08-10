package com.dexm.personajes.application;

import com.dexm.personajes.adapter.out.persistence.CharacterRepository;
import com.dexm.personajes.adapter.out.persistence.OtherInventoryItemEntity;
import com.dexm.personajes.adapter.out.persistence.OtherInventoryItemRepository;
import com.dexm.personajes.adapter.in.web.CharacterController.OtherInventoryItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class OtherInventoryService {
    private final CharacterRepository characters;
    private final OtherInventoryItemRepository items;

    public OtherInventoryService(CharacterRepository characters, OtherInventoryItemRepository items) {
        this.characters = characters; this.items = items;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String characterId) {
        ensureCharacter(characterId);
        return items.findByCharacterIdOrderByNameAsc(characterId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String characterId, String itemId) {
        return view(find(characterId, itemId));
    }

    @Transactional
    public Map<String, Object> create(String characterId, OtherInventoryItemRequest request) {
        ensureCharacter(characterId);
        var entity = new OtherInventoryItemEntity(UUID.randomUUID().toString(), characterId, request.name().trim(),
                clean(request.description()), clean(request.location()), request.quantity(), request.unitValue());
        return view(items.save(entity));
    }

    @Transactional
    public Map<String, Object> update(String characterId, String itemId, OtherInventoryItemRequest request) {
        var entity = find(characterId, itemId);
        entity.setName(request.name().trim()); entity.setDescription(clean(request.description()));
        entity.setLocation(clean(request.location())); entity.setQuantity(request.quantity()); entity.setUnitValue(request.unitValue());
        return view(items.save(entity));
    }

    @Transactional
    public void delete(String characterId, String itemId) { items.delete(find(characterId, itemId)); }

    private OtherInventoryItemEntity find(String characterId, String itemId) {
        ensureCharacter(characterId);
        return items.findByIdAndCharacterId(itemId, characterId)
                .orElseThrow(() -> new NoSuchElementException("Objeto de inventario no encontrado"));
    }
    private void ensureCharacter(String id) {
        if (!characters.existsById(id)) throw new NoSuchElementException("Personaje no encontrado");
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private Map<String, Object> view(OtherInventoryItemEntity item) {
        var result = new LinkedHashMap<String, Object>();
        result.put("id", item.getId()); result.put("name", item.getName()); result.put("description", item.getDescription());
        result.put("location", item.getLocation()); result.put("quantity", item.getQuantity()); result.put("unitValue", item.getUnitValue());
        return result;
    }
}
