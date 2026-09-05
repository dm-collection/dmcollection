package net.dmcollection.server.card;

import org.jspecify.annotations.NonNull;

public record Rarity(@NonNull RarityCode code, int order, @NonNull String name) {}
