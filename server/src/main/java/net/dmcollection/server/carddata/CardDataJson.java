package net.dmcollection.server.carddata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CardDataJson(
    @JsonProperty("set_groups") List<SetGroupJson> setGroups,
    @JsonProperty("card_sets") List<CardSetJson> cardSets,
    List<CardJson> cards,
    List<PrintingJson> printings,
    @JsonProperty("card_aliases") List<CardAliasJson> cardAliases,
    List<RarityJson> rarities,
    @JsonProperty("card_civ_groups") List<CivGroupJson> cardCivGroups) {

  public record SetGroupJson(String name, @JsonProperty("sort_order") int sortOrder) {}

  public record CardSetJson(
      String code,
      String name,
      @JsonProperty("release_date") String releaseDate,
      @JsonProperty("product_type") String productType,
      @JsonProperty("set_group") String setGroup) {}

  public record CardJson(
      String name,
      @JsonProperty("is_twinpact") boolean isTwinpact,
      @JsonProperty("deck_zone") String deckZone,
      List<CardSideJson> sides,
      @JsonProperty("sort_cost") Integer sortCost,
      @JsonProperty("sort_power") Integer sortPower,
      @JsonProperty("sort_power_modifier") short sortPowerModifier,
      @JsonProperty("sort_civilization") List<Integer> sortCivilization) {}

  public record CardSideJson(
      @JsonProperty("side_order") int sideOrder,
      String name,
      Integer cost,
      @JsonProperty("cost_is_infinity") boolean costIsInfinity,
      Integer power,
      @JsonProperty("power_is_infinity") boolean powerIsInfinity,
      @JsonProperty("power_modifier") String powerModifier,
      @JsonProperty("civilization_ids") List<Integer> civilizationIds,
      @JsonProperty("card_types") List<String> cardTypes,
      List<String> races) {}

  public record PrintingJson(
      @JsonProperty("official_site_id") String officialSiteId,
      @JsonProperty("card_name") String cardName,
      @JsonProperty("set_code") String setCode,
      @JsonProperty("collector_number") String collectorNumber,
      String rarity,
      String illustrator,
      List<PrintingSideJson> sides) {}

  public record PrintingSideJson(
      @JsonProperty("side_order") int sideOrder,
      @JsonProperty("flavor_text") String flavorText,
      @JsonProperty("image_filename") String imageFilename,
      List<AbilityJson> abilities) {}

  public record AbilityJson(
      String text,
      @JsonProperty("search_text") String searchText,
      int position,
      @JsonProperty("indent_level") int indentLevel) {}

  public record CardAliasJson(
      @JsonProperty("old_name") String oldName, @JsonProperty("new_name") String newName) {}

  public record RarityJson(String name, @JsonProperty("sort_order") short sortOrder) {}

  public record CivGroupJson(
      @JsonProperty("card_name") String cardName,
      @JsonProperty("civilization_ids") List<Integer> civilizationIds,
      @JsonProperty("includes_colorless_side") boolean includesColorlessSide) {}
}
