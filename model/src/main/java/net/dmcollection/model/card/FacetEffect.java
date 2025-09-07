package net.dmcollection.model.card;

import jakarta.annotation.Nonnull;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("FACET_EFFECT")
public record FacetEffect(
    @Nonnull Integer position, @Column("EFFECT") AggregateReference<Effect, Long> effect) {}
