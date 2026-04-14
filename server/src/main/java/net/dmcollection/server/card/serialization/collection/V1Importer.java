package net.dmcollection.server.card.serialization.collection;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import net.dmcollection.server.card.serialization.collection.format.Header;
import net.dmcollection.server.card.serialization.collection.format.v1.V1CollectionCardExport;
import net.dmcollection.server.card.serialization.collection.format.v1.V1CollectionExport;
import net.dmcollection.server.card.serialization.collection.format.v2.V2CollectionEntry;
import net.dmcollection.server.card.serialization.collection.format.v2.V2CollectionExport;
import net.dmcollection.server.card.serialization.collection.format.v2.V2Printing;
import org.springframework.stereotype.Component;

@Component
public class V1Importer {

  private final V2Importer v2Importer;

  V1Importer(V2Importer v2Importer) {
    this.v2Importer = v2Importer;
  }

  public void importCollection(V1CollectionExport exported, UUID userId) {
    // migrate to v2 schema equivalent, might introduce common import format to migrate to when V3
    // is necessary
    V2CollectionExport v2Export =
        new V2CollectionExport(
            new Header(
                2,
                exported.exportDateTime().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                exported.title()),
            null,
            exported.cards().stream().map(this::fromCardExport).toList());
    // use v2 importer
    v2Importer.importCollection(v2Export, userId);
  }

  private V2CollectionEntry fromCardExport(V1CollectionCardExport cardExport) {
    return new V2CollectionEntry(
        cardExport.name(), List.of(new V2Printing(cardExport.shortName(), cardExport.amount())));
  }
}
