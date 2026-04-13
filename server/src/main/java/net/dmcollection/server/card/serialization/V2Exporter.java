package net.dmcollection.server.card.serialization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import net.dmcollection.server.card.serialization.format.Header;
import net.dmcollection.server.card.serialization.format.MetaData;
import org.springframework.stereotype.Component;

@Component
public class V2Exporter {

  private final CollectionReader collectionReader;

  V2Exporter(CollectionReader collectionReader) {
    this.collectionReader = collectionReader;
  }

  public record V2Printing(String id, int amount) {}

  public record V2CollectionEntry(String cardName, List<V2Printing> prints) {
    static V2CollectionEntry fromEntry(CollectionReader.CollectionEntry entry) {
      return new V2CollectionEntry(
          entry.cardName(),
          entry.printings().stream()
              .map(p -> new V2Printing(p.officialSiteId(), p.quantity()))
              .toList());
    }
  }

  public record V2CollectionExport(Header version, MetaData meta, List<V2CollectionEntry> cards) {}

  public V2CollectionExport export(UUID userId) {
    List<V2CollectionEntry> entries =
        collectionReader.readCollection(userId).stream().map(V2CollectionEntry::fromEntry).toList();
    int unique = entries.size();
    int total =
        entries.parallelStream()
            .flatMap(e -> e.prints().parallelStream())
            .mapToInt(V2Printing::amount)
            .sum();
    var meta = new MetaData(total, unique);
    var header =
        new Header(
            Header.EXPORT_FORMAT_VERSION, LocalDateTime.now(), Header.EXPORT_TYPE_COLLECTION);
    return new V2CollectionExport(header, meta, entries);
  }
}
