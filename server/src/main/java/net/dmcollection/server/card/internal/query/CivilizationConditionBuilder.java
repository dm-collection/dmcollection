package net.dmcollection.server.card.internal.query;

import static net.dmcollection.server.jooq.generated.tables.CardCivGroup.CARD_CIV_GROUP;
import static org.jooq.impl.DSL.arrayOverlap;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.not;
import static org.jooq.impl.DSL.or;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.dmcollection.server.card.Civilization;
import org.jooq.Condition;

public class CivilizationConditionBuilder {

  public static Condition build(
      Set<Civilization> included,
      Set<Civilization> excluded,
      boolean includeMono,
      boolean includeRainbow,
      boolean matchExactRainbowCivs) {

    if (included.isEmpty() && excluded.isEmpty()) {
      return noCondition();
    }

    boolean allIncluded = included.size() == Civilization.values().length;
    boolean hasColorless = included.contains(Civilization.ZERO);
    Short[] colorCivIds = toColorIds(included);

    // When all civs included, no exclusions, both mono and rainbow → no filter needed
    if (allIncluded && excluded.isEmpty() && includeMono && includeRainbow && !matchExactRainbowCivs) {
      return noCondition();
    }

    List<Condition> branches = new ArrayList<>();

    if (hasColorless && includeMono) {
      branches.add(CARD_CIV_GROUP.INCLUDES_COLORLESS_SIDE.isTrue());
    }

    if (colorCivIds.length > 0) {
      List<Condition> colorBranches = new ArrayList<>();

      if (includeMono) {
        if (allIncluded) {
          // All civs included, just filter by count
          colorBranches.add(CARD_CIV_GROUP.CIV_COUNT.eq((short) 1));
        } else {
          colorBranches.add(
              CARD_CIV_GROUP
                  .CIV_COUNT
                  .eq((short) 1)
                  .and(arrayOverlap(CARD_CIV_GROUP.CIVILIZATION_IDS, colorCivIds)));
        }
      }

      if (includeRainbow) {
        if (matchExactRainbowCivs) {
          colorBranches.add(CARD_CIV_GROUP.CIVILIZATION_IDS.eq(colorCivIds));
        } else if (allIncluded) {
          // All civs included, just filter by count > 1
          colorBranches.add(CARD_CIV_GROUP.CIV_COUNT.gt((short) 1));
        } else {
          colorBranches.add(
              CARD_CIV_GROUP
                  .CIV_COUNT
                  .gt((short) 1)
                  .and(arrayOverlap(CARD_CIV_GROUP.CIVILIZATION_IDS, colorCivIds)));
        }
      }

      if (!colorBranches.isEmpty()) {
        branches.add(or(colorBranches));
      }
    }

    Condition result = branches.isEmpty() ? noCondition() : or(branches);

    if (!excluded.isEmpty()) {
      Short[] excludedIds = toColorIds(excluded);
      result = result.and(not(arrayOverlap(CARD_CIV_GROUP.CIVILIZATION_IDS, excludedIds)));
    }

    return result;
  }

  private static Short[] toColorIds(Set<Civilization> civs) {
    return civs.stream()
        .filter(c -> c != Civilization.ZERO)
        .map(c -> (short) c.ordinal())
        .sorted()
        .toArray(Short[]::new);
  }
}
