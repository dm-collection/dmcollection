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

  private CivilizationConditionBuilder() {}

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
    if (allIncluded
        && excluded.isEmpty()
        && includeMono
        && includeRainbow
        && !matchExactRainbowCivs) {
      return noCondition();
    }

    List<Condition> branches = new ArrayList<>();

    if (hasColorless && includeMono) {
      branches.add(CARD_CIV_GROUP.INCLUDES_COLORLESS_SIDE.isTrue());
    }

    if (colorCivIds.length > 0) {
      List<Condition> colorBranches = new ArrayList<>();

      if (includeMono) {
        colorBranches.add(monoCondition(allIncluded, colorCivIds));
      }
      if (includeRainbow) {
        colorBranches.add(rainbowCondition(allIncluded, colorCivIds, matchExactRainbowCivs));
      }

      if (!colorBranches.isEmpty()) {
        branches.add(or(colorBranches));
      }
    }

    Condition result = branches.isEmpty() ? noCondition() : or(branches);

    if (!excluded.isEmpty()) {
      result = result.and(excludedCondition(excluded));
    }

    return result;
  }

  private static Condition monoCondition(boolean allIncluded, Short[] colorCivIds) {
    if (allIncluded) {
      return CARD_CIV_GROUP.CIV_COUNT.eq((short) 1);
    }
    return CARD_CIV_GROUP
        .CIV_COUNT
        .eq((short) 1)
        .and(arrayOverlap(CARD_CIV_GROUP.CIVILIZATION_IDS, colorCivIds));
  }

  private static Condition rainbowCondition(
      boolean allIncluded, Short[] colorCivIds, boolean matchExact) {
    if (matchExact) {
      return CARD_CIV_GROUP.CIVILIZATION_IDS.eq(colorCivIds);
    }
    if (allIncluded) {
      return CARD_CIV_GROUP.CIV_COUNT.gt((short) 1);
    }
    return CARD_CIV_GROUP
        .CIV_COUNT
        .gt((short) 1)
        .and(arrayOverlap(CARD_CIV_GROUP.CIVILIZATION_IDS, colorCivIds));
  }

  private static Condition excludedCondition(Set<Civilization> excluded) {
    Short[] excludedIds = toColorIds(excluded);
    return not(arrayOverlap(CARD_CIV_GROUP.CIVILIZATION_IDS, excludedIds));
  }

  private static Short[] toColorIds(Set<Civilization> civs) {
    return civs.stream()
        .filter(c -> c != Civilization.ZERO)
        .map(c -> (short) c.ordinal())
        .sorted()
        .toArray(Short[]::new);
  }
}
