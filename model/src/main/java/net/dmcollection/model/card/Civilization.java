package net.dmcollection.model.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum Civilization {
  ZERO("ゼロ"),
  LIGHT("光"),
  WATER("水"),
  DARK("闇"),
  FIRE("火"),
  NATURE("自然");

  private final String name;
  private static final Map<String, Civilization> BY_NAME =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(civ -> civ.name, civ -> civ));

  Civilization(String name) {
    this.name = name;
  }

  public static Civilization fromName(String name) {
    Civilization civ = BY_NAME.get(name);
    if (civ == null) {
      throw new IllegalArgumentException("Invalid civilization name: " + name);
    }
    return civ;
  }

  public static Set<Civilization> fromInts(List<Integer> civs) {
    EnumSet<Civilization> result = EnumSet.noneOf(Civilization.class);
    for (Integer b : civs) {
      if (b != null) {
        result.add(Civilization.values()[b]);
      }
    }
    return result;
  }

  public static List<Integer> toInts(Set<Civilization> civs) {
    List<Integer> result = new ArrayList<>(civs.stream().map(Enum::ordinal).toList());
    Collections.sort(result);
    return result;
  }

  public String toString() {
    return this.name;
  }
}
