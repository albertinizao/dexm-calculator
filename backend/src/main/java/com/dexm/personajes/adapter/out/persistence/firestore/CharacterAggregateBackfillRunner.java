package com.dexm.personajes.adapter.out.persistence.firestore;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CharacterAggregateBackfillRunner implements ApplicationRunner {
    private final CharacterAggregateBackfill backfill;
    private final boolean enabled;

    public CharacterAggregateBackfillRunner(CharacterAggregateBackfill backfill,
                                            @Value("${app.maintenance.backfill-character-aggregates:false}") boolean enabled) {
        this.backfill = backfill; this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (!enabled) return;
        backfill.run();
    }
}

