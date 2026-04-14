package net.dmcollection.server.card.serialization.collection.format.v2;

import java.util.List;
import net.dmcollection.server.card.serialization.collection.CollectionReader;

public record V2CollectionEntry(String cardName, List<V2Printing> prints) {
  public static V2CollectionEntry fromEntry(CollectionReader.CollectionEntry entry) {
    return new V2CollectionEntry(
        entry.cardName(),
        entry.printings().stream()
            .map(p -> new V2Printing(p.officialSiteId(), p.quantity()))
            .toList());
  }
}
