package net.dmcollection.server.card;

import org.springframework.lang.NonNull;

public record Rarity(@NonNull RarityCode code, int order, @NonNull String name) {}
