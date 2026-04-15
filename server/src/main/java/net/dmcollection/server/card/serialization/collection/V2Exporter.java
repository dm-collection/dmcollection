package net.dmcollection.server.card.serialization.collection;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import net.dmcollection.server.card.serialization.collection.format.Header;
import net.dmcollection.server.card.serialization.collection.format.MetaData;
import net.dmcollection.server.card.serialization.collection.format.v2.V2CollectionEntry;
import net.dmcollection.server.card.serialization.collection.format.v2.V2CollectionExport;
import net.dmcollection.server.card.serialization.collection.format.v2.V2Printing;
import org.springframework.stereotype.Component;

@Component
public class V2Exporter {

  private final CollectionReader collectionReader;

  V2Exporter(CollectionReader collectionReader) {
    this.collectionReader = collectionReader;
  }

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
            Header.EXPORT_FORMAT_VERSION, OffsetDateTime.now(), Header.EXPORT_TYPE_COLLECTION);
    return new V2CollectionExport(header, meta, entries);
  }
}
