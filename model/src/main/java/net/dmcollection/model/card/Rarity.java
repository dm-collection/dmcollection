package net.dmcollection.model.card;

import org.springframework.lang.NonNull;

public record Rarity(@NonNull RarityCode code, @NonNull int order, @NonNull String name) {

  public static final class Columns {
    private Columns() {}

    public static final String ORDER = "ORDER";
  }
}
