package net.dmcollection.server.card.internal;

import static net.dmcollection.server.jooq.generated.tables.Rarity.RARITY;

import java.util.ArrayList;
import java.util.List;
import net.dmcollection.server.card.Rarity;
import net.dmcollection.server.card.RarityCode;
import org.jooq.DSLContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class RarityService {

  private final DSLContext dsl;

  public RarityService(DSLContext dsl) {
    this.dsl = dsl;
  }

  public int getOrder(@NonNull RarityCode rarityCode) {
    Short order =
        dsl.select(RARITY.SORT_ORDER)
            .from(RARITY)
            .where(RARITY.NAME.eq(rarityCode.toString()))
            .fetchOne(RARITY.SORT_ORDER);
    return order != null ? order : 0;
  }

  public List<Rarity> getRarities() {
    List<Rarity> result = new ArrayList<>();
    dsl.select(RARITY.NAME, RARITY.SORT_ORDER)
        .from(RARITY)
        .orderBy(RARITY.SORT_ORDER)
        .forEach(
            r -> {
              try {
                result.add(
                    new Rarity(
                        RarityCode.valueOf(r.get(RARITY.NAME)),
                        r.get(RARITY.SORT_ORDER),
                        r.get(RARITY.NAME)));
              } catch (IllegalArgumentException ignored) {
                // Skip rarities not in enum
              }
            });
    return result;
  }
}
