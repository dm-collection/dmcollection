package net.dmcollection.model.card;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PowerTest {

  @Test
  void powersAreSortedCorrectly() {
    final Power one = new Power("1000");
    final Power two = new Power("2000");
    final Power ten = new Power("10000");
    final Power infinity = new Power("∞");
    final Power onePlus = new Power("1000+");
    final Power oneMinus = new Power("1000－");
    List<Power> powers = Arrays.asList(onePlus, two, ten, one, infinity, oneMinus);
    Collections.sort(powers);
    assertThat(powers).containsExactly(oneMinus, one, onePlus, two, ten, infinity);
  }
}
