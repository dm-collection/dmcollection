package net.dmcollection.server.card.internal.query;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes card names and readings for searching, so that width, case, hiragana/katakana, spaces
 * and (visually indistinct) punctuation don't affect matching.
 */
public final class NameNormalizer {

  private static final Pattern IGNORED = Pattern.compile("[\\p{Z}\\p{Cc}・「」！!＝=～~♪“”]");

  private NameNormalizer() {}

  public static String normalize(String input) {
    if (input == null) {
      return "";
    }
    String nfkc = Normalizer.normalize(input, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    String stripped = IGNORED.matcher(nfkc).replaceAll("");
    StringBuilder result = new StringBuilder(stripped.length());
    for (int i = 0; i < stripped.length(); i++) {
      char c = stripped.charAt(i);
      // Hiragana ぁ..ゖ map onto katakana ァ..ヶ
      if (c >= 'ぁ' && c <= 'ゖ') {
        c += 0x60;
      }
      result.append(c);
    }
    return result.toString();
  }

  /**
   * Normalized reading of the whole card: the sides' readings in side order, falling back to the
   * side name for sides without a reading.
   */
  public static String reading(List<SideName> sides) {
    StringBuilder result = new StringBuilder();
    for (SideName side : sides) {
      result.append(normalize(side.reading() != null ? side.reading() : side.name()));
    }
    return result.toString();
  }

  public record SideName(String name, String reading) {}
}
