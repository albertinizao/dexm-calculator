package com.dexm.personajes.adapter.out.persistence.firestore;

import com.dexm.personajes.adapter.out.persistence.CharacterAttributeModifierEntity;
import com.dexm.personajes.adapter.out.persistence.CharacterAttributeModifierRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirestoreRepositoryFactoryTest {

    @Test
    void firestoreRepositoryTreatsFlushAsNoOpAndSaveAndFlushAsSave() throws Exception {
        Firestore firestore = mock(Firestore.class);
        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference document = mock(DocumentReference.class);
        ApiFuture<WriteResult> write = mock(ApiFuture.class);
        when(firestore.collection("modifiers")).thenReturn(collection);
        when(collection.document("m1")).thenReturn(document);
        when(document.set(any())).thenReturn(write);
        when(write.get()).thenReturn(null);

        var repository = new FirestoreRepositoryFactory(firestore, new ObjectMapper())
                .create(CharacterAttributeModifierRepository.class, CharacterAttributeModifierEntity.class, "modifiers");
        var entity = new CharacterAttributeModifierEntity("m1", "c1", "fisico", "Armadura", 2);

        assertThatCode(repository::flush).doesNotThrowAnyException();
        verifyNoInteractions(firestore);
        assertThat(repository.saveAndFlush(entity)).isSameAs(entity);
        verify(document).set(any());
    }
}
