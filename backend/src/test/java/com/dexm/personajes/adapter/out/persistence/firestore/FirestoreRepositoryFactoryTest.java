package com.dexm.personajes.adapter.out.persistence.firestore;

import com.dexm.personajes.adapter.out.persistence.CharacterAttributeModifierEntity;
import com.dexm.personajes.adapter.out.persistence.CharacterAttributeModifierRepository;
import com.dexm.personajes.adapter.out.persistence.TrainingActivityEntity;
import com.dexm.personajes.adapter.out.persistence.TrainingActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;

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

    @Test
    void firestoreRepositorySupportsMultipleOrderByProperties() throws Exception {
        Firestore firestore = mock(Firestore.class);
        CollectionReference collection = mock(CollectionReference.class);
        ApiFuture<QuerySnapshot> read = mock(ApiFuture.class);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot first = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot second = mock(QueryDocumentSnapshot.class);
        when(firestore.collection("training")).thenReturn(collection);
        when(collection.get()).thenReturn(read);
        when(read.get()).thenReturn(snapshot);
        when(snapshot.getDocuments()).thenReturn(List.of(first, second));
        when(first.exists()).thenReturn(true);
        when(second.exists()).thenReturn(true);
        when(first.getString("json")).thenReturn("{\"id\":\"a\",\"characterId\":\"c1\",\"type\":\"FORMATION\",\"name\":\"A\",\"startAge\":10,\"endAge\":12,\"priority\":1,\"concurrent\":false}");
        when(second.getString("json")).thenReturn("{\"id\":\"b\",\"characterId\":\"c1\",\"type\":\"FORMATION\",\"name\":\"B\",\"startAge\":10,\"endAge\":12,\"priority\":0,\"concurrent\":false}");

        var repository = new FirestoreRepositoryFactory(firestore, new ObjectMapper())
                .create(TrainingActivityRepository.class, TrainingActivityEntity.class, "training");

        assertThat(repository.findByCharacterIdOrderByStartAgeAscPriorityAsc("c1"))
                .extracting(TrainingActivityEntity::getId)
                .containsExactly("b", "a");
    }
}
