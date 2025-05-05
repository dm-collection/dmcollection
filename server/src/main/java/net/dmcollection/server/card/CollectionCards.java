package net.dmcollection.server.card;

import jakarta.annotation.Nonnull;
import net.dmcollection.model.card.CardEntity;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;

public record CollectionCards(
    @Nonnull Integer amount, @Nonnull @Column("CARD") AggregateReference<CardEntity, Long> id) {}
