package net.dmcollection.server.card.internal;

import java.util.List;
import java.util.Set;
import net.dmcollection.model.card.Civilization;

public interface CivsCondition {

  static TwinpactCivsCondition twinpactCondition() {
    return CivsConditionImpl.forSides(2);
  }

  static CivsCondition nonTwinpactCondition() {
    return CivsConditionImpl.forSides(3);
  }

  CivsCondition not();

  SubCondition sideHasSize(int side, int size);

  SubCondition sideHasMoreCivsThan(int side, int size);

  SubCondition anySideHasMoreCivsThan(int size);

  SubCondition anySideHasExactlyCivs(Set<Civilization> civs);

  SubCondition containsInAnySideAnyOf(Set<Civilization> civs);

  SubCondition sideContainsAnyOf(int side, Set<Civilization> civs);

  void add(List<String> query, List<Object> parameters);

  interface TwinpactCivsCondition extends CivsCondition {
    SubCondition sidesAreEqual();

    SubCondition sidesAreDifferent();

    SubCondition containsAllAcrossAllSides(Set<Civilization> civs);
  }

  interface ConjunctionCondition {}

  interface SubCondition {
    CivsCondition and();

    CivsCondition or();

    void add(List<String> query, List<Object> parameters);
  }
}
