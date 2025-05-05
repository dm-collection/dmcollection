package net.dmcollection.model.card;

import jakarta.annotation.Nonnull;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;

public record FacetSpecies(
    @Nonnull Integer position, @Column("SPECIES") AggregateReference<Species, Long> id) {}
