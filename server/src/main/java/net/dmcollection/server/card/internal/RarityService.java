package net.dmcollection.server.card.internal;

import static net.dmcollection.server.jooq.generated.tables.Rarity.RARITY;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.dmcollection.server.card.Rarity;
import net.dmcollection.server.card.RarityCode;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class RarityService {

  private static final Logger log = LoggerFactory.getLogger(RarityService.class);
  private final DSLContext dsl;

  private List<Rarity> rarities;
  private Map<RarityCode, Integer> codeToOrder;

  public RarityService(DSLContext dsl) {
    this.dsl = dsl;
  }

  public void loadRarities() {
    List<Rarity> loaded = new ArrayList<>();
    Map<RarityCode, Integer> orders = new EnumMap<>(RarityCode.class);
    dsl.select(RARITY.NAME, RARITY.SORT_ORDER, RARITY.DESCRIPTION)
        .from(RARITY)
        .orderBy(RARITY.SORT_ORDER)
        .forEach(
            r -> {
              try {
                RarityCode code = RarityCode.valueOf(r.get(RARITY.NAME));
                Short sortOrder = r.get(RARITY.SORT_ORDER);
                loaded.add(new Rarity(code, sortOrder, r.get(RARITY.DESCRIPTION)));
                orders.put(code, sortOrder.intValue());
              } catch (IllegalArgumentException ignored) {
                log.error(
                    "Unable to create rarity {} {} {}",
                    r.get(RARITY.NAME),
                    r.get(RARITY.SORT_ORDER),
                    r.get(RARITY.DESCRIPTION));
                // Skip rarities not in enum
              }
            });
    this.rarities = List.copyOf(loaded);
    this.codeToOrder = orders;
  }

  public int getOrder(@NonNull RarityCode rarityCode) {
    if (codeToOrder == null) {
      loadRarities();
    }
    Integer order = codeToOrder.get(rarityCode);
    return order != null ? order : 0;
  }

  public List<Rarity> getRarities() {
    if (rarities == null) {
      loadRarities();
    }
    return rarities;
  }
}
