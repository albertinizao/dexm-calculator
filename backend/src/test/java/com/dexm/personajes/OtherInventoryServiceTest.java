package com.dexm.personajes;

import com.dexm.personajes.adapter.in.web.CharacterController.OtherInventoryItemRequest;
import com.dexm.personajes.adapter.out.persistence.CharacterRepository;
import com.dexm.personajes.adapter.out.persistence.OtherInventoryItemEntity;
import com.dexm.personajes.adapter.out.persistence.OtherInventoryItemRepository;
import com.dexm.personajes.application.OtherInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OtherInventoryServiceTest {
    private CharacterRepository characters;
    private OtherInventoryItemRepository items;
    private OtherInventoryService service;

    @BeforeEach
    void setUp() { characters = mock(CharacterRepository.class); items = mock(OtherInventoryItemRepository.class); service = new OtherInventoryService(characters, items); }

    @Test
    void createsAndListsItemsForCharacter() {
        when(characters.existsById("c1")).thenReturn(true);
        var saved = new OtherInventoryItemEntity("i1", "c1", "Antorcha", "Luz", "Mochila", 2, new BigDecimal("3.50"));
        when(items.save(any())).thenReturn(saved);
        when(items.findByCharacterIdOrderByNameAsc("c1")).thenReturn(List.of(saved));

        var created = service.create("c1", new OtherInventoryItemRequest(" Antorcha ", "Luz", "Mochila", 2, new BigDecimal("3.50")));
        var listed = service.list("c1");

        assertThat(created).containsEntry("name", "Antorcha").containsEntry("quantity", 2);
        assertThat(listed).hasSize(1);
        assertThat(listed.getFirst()).containsEntry("id", "i1");
    }

    @Test
    void updatesAndDeletesOnlyTheRequestedCharacterItem() {
        when(characters.existsById("c1")).thenReturn(true);
        var item = new OtherInventoryItemEntity("i1", "c1", "Antorcha", null, null, 1, null);
        when(items.findByIdAndCharacterId("i1", "c1")).thenReturn(Optional.of(item));
        when(items.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.update("c1", "i1", new OtherInventoryItemRequest("Linterna", "", "Bolsa", 3, BigDecimal.ZERO));
        service.delete("c1", "i1");

        assertThat(updated).containsEntry("name", "Linterna").containsEntry("location", "Bolsa").containsEntry("unitValue", BigDecimal.ZERO);
        verify(items).delete(item);
        verify(items, never()).deleteByCharacterId(any());
    }

    @Test
    void rejectsMissingCharacterOrItem() {
        when(characters.existsById("missing")).thenReturn(false);
        assertThatThrownBy(() -> service.list("missing")).isInstanceOf(java.util.NoSuchElementException.class);
        when(characters.existsById("c1")).thenReturn(true);
        when(items.findByIdAndCharacterId("missing", "c1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get("c1", "missing")).isInstanceOf(java.util.NoSuchElementException.class);
    }
}
