package net.dmcollection.server.card.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.dmcollection.model.card.Civilization;
import net.dmcollection.server.card.internal.CivsCondition.ConjunctionCondition;
import net.dmcollection.server.card.internal.CivsCondition.SubCondition;
import net.dmcollection.server.card.internal.CivsCondition.TwinpactCivsCondition;

public class CivsConditionImpl
    implements TwinpactCivsCondition, SubCondition, ConjunctionCondition {

  private final List<Object> parameters = new ArrayList<>();
  private final List<String> parts = new ArrayList<>();
  private final int sides;

  private CivsConditionImpl(int numberOfSides) {
    this.sides = numberOfSides;
  }

  public static CivsConditionImpl forSides(int numberOfSides) {
    return new CivsConditionImpl(numberOfSides);
  }

  @Override
  public SubCondition sidesAreEqual() {
    if (sides != 2) {
      throw new IllegalStateException("Can only check two-sided cards for civ equality");
    }
    parts.add("CIVS0 = CIVS1");
    return this;
  }

  @Override
  public SubCondition sidesAreDifferent() {
    parts.add("CIVS0 <> CIVS1"); // can do this because civs are sorted
    return this;
  }

  @Override
  public CivsCondition and() {
    parts.add("AND");
    return this;
  }

  @Override
  public CivsCondition or() {
    parts.add("OR");
    return this;
  }

  @Override
  public CivsCondition not() {
    parts.add("NOT");
    return this;
  }

  @Override
  public SubCondition sideHasSize(int side, int size) {
    parts.add("CARDINALITY(CIVS%d) = ?".formatted(side));
    parameters.add(size);
    return this;
  }

  @Override
  public SubCondition sideHasMoreCivsThan(int side, int size) {
    parts.add("CARDINALITY(CIVS%d) > ?".formatted(side));
    parameters.add(size);
    return this;
  }

  @Override
  public SubCondition anySideHasMoreCivsThan(int size) {
    List<String> conditions = new ArrayList<>();
    for (int i = 0; i < sides; i++) {
      conditions.add("CARDINALITY(CIVS%d) > ?".formatted(i));
      parameters.add(size);
    }
    parts.add(or(conditions));
    return this;
  }

  @Override
  public SubCondition anySideHasExactlyCivs(Set<Civilization> civs) {
    List<String> conditions = new ArrayList<>();
    for (int i = 0; i < sides; i++) {
      List<String> andSubConditions = new ArrayList<>();
      andSubConditions.add("CARDINALITY(CIVS%d) = ?".formatted(i));
      parameters.add(civs.size());
      for (Civilization civ : civs) {
        andSubConditions.add("ARRAY_CONTAINS(CIVS%d, ?)".formatted(i));
        parameters.add(civ.ordinal());
      }
      conditions.add(and(andSubConditions));
    }
    parts.add(or(conditions));
    return this;
  }

  @Override
  public SubCondition containsAllAcrossAllSides(Set<Civilization> civs) {
    List<String> conditions = new ArrayList<>();
    for (Civilization civ : civs) {
      conditions.add("(ARRAY_CONTAINS(CIVS0, ?) OR ARRAY_CONTAINS(CIVS1, ?))");
      parameters.add(civ.ordinal());
      parameters.add(civ.ordinal());
    }
    parts.add(and(conditions));
    return this;
  }

  @Override
  public SubCondition containsInAnySideAnyOf(Set<Civilization> civs) {
    List<String> conditions = new ArrayList<>();
    for (int i = 0; i < sides; i++) {
      for (Civilization civ : civs) {
        conditions.add("ARRAY_CONTAINS(CIVS%d, ?)".formatted(i));
        parameters.add(civ.ordinal());
      }
    }
    parts.add(or(conditions));
    return this;
  }

  @Override
  public void add(List<String> query, List<Object> parameters) {
    query.add(String.join(" ", parts));
    parameters.addAll(this.parameters);
  }

  @Override
  public SubCondition sideContainsAnyOf(int side, Set<Civilization> civs) {
    List<String> conditions = new ArrayList<>();
    for (Civilization civ : civs) {
      conditions.add("ARRAY_CONTAINS(CIVS%d, ?)".formatted(side));
      parameters.add(civ.ordinal());
    }
    parts.add(or(conditions));
    return this;
  }

  private static String and(List<String> conditions) {
    return connectConditions(conditions, "AND");
  }

  private static String or(List<String> conditions) {
    return connectConditions(conditions, "OR");
  }

  private static String connectConditions(List<String> conditions, String andOrOr) {
    if (conditions.isEmpty()) {
      return "";
    }
    if (conditions.size() == 1) {
      return "(" + conditions.getFirst() + ")";
    }
    String inbetween = ") " + andOrOr + " (";
    return "((" + String.join(inbetween, conditions) + "))";
  }
}
