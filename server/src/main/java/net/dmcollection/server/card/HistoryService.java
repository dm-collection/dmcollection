package net.dmcollection.server.card;

import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_HISTORY_ENTRY;
import static org.jooq.impl.DSL.arrayAgg;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;

import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONFormat;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {

  private final DSLContext dsl;

  public HistoryService(DSLContext dsl) {
    this.dsl = dsl;
  }

  public String getLatest(UUID userId, int limit) {
    return dsl.select(
            COLLECTION_HISTORY_ENTRY.LABEL,
            COLLECTION_HISTORY_ENTRY.PREVIOUS_QTY,
            COLLECTION_HISTORY_ENTRY.NEW_QTY,
            COLLECTION_HISTORY_ENTRY.CHANGED_AT,
            COLLECTION_HISTORY_ENTRY.printing().OFFICIAL_SITE_ID,
            COLLECTION_HISTORY_ENTRY.printing().COLLECTOR_NUMBER,
            COLLECTION_HISTORY_ENTRY.printing().card().NAME,
            COLLECTION_HISTORY_ENTRY.printing().cardSet().ID,
            COLLECTION_HISTORY_ENTRY.printing().cardSet().NAME,
            field(
                    select(
                            arrayAgg(
                                COLLECTION_HISTORY_ENTRY.printing().printingSide().IMAGE_FILENAME))
                        .from(COLLECTION_HISTORY_ENTRY.printing().printingSide())
                        .where(
                            COLLECTION_HISTORY_ENTRY
                                .printing()
                                .printingSide()
                                .IMAGE_FILENAME
                                .isNotNull()))
                .as("images"))
        .from(COLLECTION_HISTORY_ENTRY)
        .where(COLLECTION_HISTORY_ENTRY.USER_ID.eq(userId))
        .orderBy(COLLECTION_HISTORY_ENTRY.CHANGED_AT.desc())
        .limit(limit)
        .fetch()
        .formatJSON(JSONFormat.DEFAULT_FOR_RECORDS);
  }
}
