package net.dmcollection.server.card;

import static net.dmcollection.server.jooq.generated.tables.CardSet.CARD_SET;

import java.util.List;
import net.dmcollection.server.card.CardService.SetDto;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class SetService {
  private final DSLContext dsl;

  public SetService(DSLContext dsl) {
    this.dsl = dsl;
  }

  public List<SetDto> getSets() {
    return dsl.select(CARD_SET.ID, CARD_SET.CODE, CARD_SET.NAME)
        .from(CARD_SET)
        .orderBy(CARD_SET.RELEASE_DATE.desc())
        .fetch(r -> new SetDto(r.get(CARD_SET.ID).longValue(), r.get(CARD_SET.CODE), r.get(CARD_SET.NAME)));
  }
}
