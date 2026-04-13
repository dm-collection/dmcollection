package net.dmcollection.server.card.serialization;

import java.util.List;
import java.util.UUID;
import net.dmcollection.server.card.CollectionService;
import net.dmcollection.server.card.serialization.format.Header;
import org.springframework.stereotype.Component;

@Component
public class V1Importer {

  private final V2Importer v2Importer;

  V1Importer(V2Importer v2Importer) {
    this.v2Importer = v2Importer;
  }

  public void importCollection(CollectionService.CollectionExport exported, UUID userId) {
    // TODO: use a Map<String, Integer> printing official id -> amount instead somehow
    // migrate to v2 schema equivalent
    V2Exporter.V2CollectionExport v2Export =
        new V2Exporter.V2CollectionExport(
            new Header(2, exported.exportDateTime(), exported.title()),
            null,
            exported.cards().stream().map(this::fromCardExport).toList());
    // use v2 importer
    v2Importer.importCollection(v2Export, userId);
  }

  private V2Exporter.V2CollectionEntry fromCardExport(
      CollectionService.CollectionCardExport cardExport) {
    return new V2Exporter.V2CollectionEntry(
        cardExport.name(),
        List.of(new V2Exporter.V2Printing(cardExport.shortName(), cardExport.amount())));
  }
}
