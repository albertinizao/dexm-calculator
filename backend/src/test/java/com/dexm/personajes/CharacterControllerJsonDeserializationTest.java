package com.dexm.personajes;

import com.dexm.personajes.adapter.in.web.CharacterController;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterControllerJsonDeserializationTest {
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void omittedPortraitIsRepresentedAsAbsent() throws Exception {
        var request = json.readValue("{\"name\":\"Astrid\",\"experience\":0}", CharacterController.SaveRequest.class);

        assertThat(request.imageUrl()).isNull();
    }

    @Test
    void nullPortraitIsPreservedAsJsonNull() throws Exception {
        var request = json.readValue("{\"name\":\"Astrid\",\"experience\":0,\"imageUrl\":null}", CharacterController.SaveRequest.class);

        assertThat(request.imageUrl()).isNotNull();
        assertThat(request.imageUrl().isNull()).isTrue();
    }

    @Test
    void stringPortraitIsDeserializedAsText() throws Exception {
        var request = json.readValue("{\"name\":\"Astrid\",\"experience\":0,\"imageUrl\":\"data:image/jpeg;base64,AA==\"}",
                CharacterController.SaveRequest.class);

        assertThat(request.imageUrl().asText()).isEqualTo("data:image/jpeg;base64,AA==");
    }
}
