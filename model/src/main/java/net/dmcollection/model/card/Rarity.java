package net.dmcollection.model.card;

import org.springframework.lang.NonNull;

public record Rarity(@NonNull RarityCode code, @NonNull int order, @NonNull String name) {}
