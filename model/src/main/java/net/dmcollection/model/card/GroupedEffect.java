package net.dmcollection.model.card;

import jakarta.annotation.Nonnull;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;

public record GroupedEffect(
    @Nonnull Integer position,
    @Column("CHILD_EFFECT") AggregateReference<Effect, Long> childEffect) {}
