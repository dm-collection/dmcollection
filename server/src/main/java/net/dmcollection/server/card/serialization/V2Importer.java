package net.dmcollection.server.card.serialization;

import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public class V2Importer {
  private final DSLContext dsl;

  V2Importer(DSLContext dsl) {
    this.dsl = dsl;
  }

  public void importCollection(V2Exporter.V2CollectionExport exported, UUID userId) {
    // get state before import

    // calculate history entries - only for actual changes

    // replace collection entries with entries from import

    // write history entries, all with same timestamp

  }
}
