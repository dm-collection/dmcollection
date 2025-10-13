package net.dmcollection.server.card.internal;

import static net.dmcollection.server.card.internal.CivsCondition.nonTwinpactCondition;
import static net.dmcollection.server.card.internal.CivsCondition.twinpactCondition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.dmcollection.model.card.CardEntity.Columns;
import net.dmcollection.model.card.CardFacet;
import net.dmcollection.model.card.Civilization;
import net.dmcollection.server.card.CardService.CardStub;
import net.dmcollection.server.card.ImageService;
import net.dmcollection.server.card.internal.SearchFilter.CardType;
import net.dmcollection.server.card.internal.SearchFilter.FilterState;
import net.dmcollection.server.card.internal.SearchFilter.RarityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class CardQueryService {

  private static final Logger log = LoggerFactory.getLogger(CardQueryService.class);
  public static final String FACET_NAME_CONDITION =
      "LOCATE(UPPER(?),UPPER(%s)) > 0".formatted(CardFacet.Columns.NAME);
  public static final String AND = " AND ";
  private final JdbcTemplate db;
  private final ImageService imageService;
  private final SpeciesService speciesService;
  private final RarityService rarityService;
  private final TypeCondition typeCondition = new TypeCondition("TYPE");

  private Instant start;

  public CardQueryService(
      JdbcTemplate db,
      ImageService imageService,
      SpeciesService speciesService,
      RarityService rarityService) {
    this.db = db;
    this.imageService = imageService;
    this.speciesService = speciesService;
    this.rarityService = rarityService;
  }

  private static final String CIV_AGG = "CIVS";
  private static final String IMAGE_AGG = "IMAGES";
  public static final String AMOUNT = "AMOUNT";
  private static final String UNIQUE_COUNT = "UNIQUE_COUNT";
  private static final String TOTAL_COUNT = "TOTAL_COUNT";

  private static final String QUERY_TEMPLATE =
      """
      SELECT c.ID, c.OFFICIAL_ID, c.ID_TEXT,
        ARRAY_AGG(cf.CIVS ORDER BY cf.POSITION ASC) as %s,
        ARRAY_AGG(cf.IMAGE_FILENAME ORDER BY cf.POSITION ASC) as %s,
        0 AS %s,
        COUNT(*) OVER() as %s
      FROM CARDS_W_RELEASE c
      LEFT JOIN RARITY r ON COALESCE(c.RARITY, 'NONE') = r.CODE
      LEFT JOIN CARD_FACETS cf ON c.ID = cf.CARDS
      """;

  private static final String COLLECTION_QUERY_TEMPLATE =
      """
      SELECT c.ID, c.OFFICIAL_ID, c.ID_TEXT,
        ARRAY_AGG(cf.CIVS ORDER BY cf.POSITION ASC) as %s,
        ARRAY_AGG(cf.IMAGE_FILENAME ORDER BY cf.POSITION ASC) as %s,
        COALESCE(cc.AMOUNT,0) AS %s,
        COUNT(*) OVER() as %s,
        SUM(cc.AMOUNT) OVER() as %s
      FROM CARDS_W_RELEASE c
      LEFT JOIN CARD_FACETS cf ON c.ID = cf.CARDS
      LEFT JOIN RARITY r ON COALESCE(c.RARITY, 'NONE') = r.CODE
      %s JOIN COLLECTION_CARDS cc ON c.ID = cc.CARD AND cc.COLLECTIONS = ?
      """;

  private String makeQueryStart(SearchFilter filter, List<Object> parameters) {
    if (filter.collectionFilter() == null) {
      return QUERY_TEMPLATE.formatted(CIV_AGG, IMAGE_AGG, AMOUNT, UNIQUE_COUNT);
    }
    String join = filter.collectionFilter().searchCollection() ? "INNER" : "LEFT";
    parameters.add(filter.collectionFilter().internalId());
    return COLLECTION_QUERY_TEMPLATE.formatted(
        CIV_AGG, IMAGE_AGG, AMOUNT, UNIQUE_COUNT, TOTAL_COUNT, join);
  }

  public record CardStubWithCount(
      Long id,
      String dmId,
      String idText,
      List<Set<Civilization>> civs,
      List<String> images,
      int amount,
      long totalCount,
      long uniqueCount) {}

  public record SearchResult(Page<CardStub> pageOfCards, long totalCollected) {}

  public SearchResult search(@NonNull SearchFilter searchFilter) {
    if (log.isDebugEnabled()) {
      this.start = Instant.now();
    }
    if (searchFilter.isInvalid()) {
      log.warn("Invalid search filter: {}", searchFilter);
      return new SearchResult(new PageImpl<>(List.of()), 0);
    }
    log.debug("Searching with filter: {}", searchFilter);
    Set<Long> facetFilter = Collections.emptySet();
    if (searchFilter.speciesSearch() != null && !searchFilter.speciesSearch().isBlank()) {
      speciesService.initialize();
      facetFilter = speciesService.getFacetIdsForSpeciesSearch(searchFilter.speciesSearch());
      if (facetFilter.isEmpty()) {
        return new SearchResult(new PageImpl<>(List.of(), searchFilter.pageable(), 0), 0);
      }
    }

    // Effect text search
    if (searchFilter.effectSearch() != null && !searchFilter.effectSearch().isBlank()) {
      Set<Long> effectFacetIds = getEffectFacetIds(searchFilter.effectSearch());
      if (effectFacetIds.isEmpty()) {
        return new SearchResult(new PageImpl<>(List.of(), searchFilter.pageable(), 0), 0);
      }

      // Intersect with existing facet filter if present
      if (!facetFilter.isEmpty()) {
        effectFacetIds.retainAll(facetFilter);
        if (effectFacetIds.isEmpty()) {
          return new SearchResult(new PageImpl<>(List.of(), searchFilter.pageable(), 0), 0);
        }
      }
      facetFilter = effectFacetIds;
    }

    List<String> query = new ArrayList<>();
    List<Object> parameters = new ArrayList<>();
    query.add(makeQueryStart(searchFilter, parameters));
    if (searchFilter.needsCivFilter()
        || searchFilter.needsFacetColumnFilter()
        || searchFilter.needsCardColumnsFilter()
        || !facetFilter.isEmpty()) {
      query.add("WHERE");
      List<String> andConditions = new ArrayList<>();

      // if there is a facet filter, add the facets subquery
      if (!facetFilter.isEmpty()) {
        String subqueryCondition =
            """
            c.ID IN (
              SELECT DISTINCT CARDS FROM CARD_FACETS WHERE ID IN (%s)
            )
            """
                .formatted(questionMarks(facetFilter.size()));
        parameters.addAll(facetFilter);
        andConditions.add(subqueryCondition);
      }

      // if there are filters on civilizations, add the civs subquery
      if (searchFilter.needsCivFilter()) {
        List<String> subquery = new ArrayList<>();
        addCivSubquery(
            subquery,
            parameters,
            searchFilter.matchExactRainbowCivs(),
            searchFilter.includedCivs(),
            searchFilter.excludedCivs(),
            searchFilter.includeMono(),
            searchFilter.includeRainbow(),
            searchFilter.twinpact());
        String subqueryCondition = "c.ID IN (" + String.join(" ", subquery) + ")";
        andConditions.add(subqueryCondition);
      }

      // if there are filters on facets, add the facets subquery
      if (searchFilter.needsFacetColumnFilter()) {
        List<String> subquery = new ArrayList<>();
        addFacetColumnFilters(subquery, parameters, searchFilter);
        String subqueryCondition = "c.ID IN (" + String.join(" ", subquery) + ")";
        andConditions.add(subqueryCondition);
      }
      // if there are filters on card columns, add the conditions
      if (searchFilter.needsCardColumnsFilter()) {
        addCardColumnConditions(andConditions, parameters, searchFilter);
      }

      query.add(and(andConditions));
    }
    query.add("GROUP BY c.ID");

    addSortingAndPaging(query, parameters, searchFilter.pageable());
    String finalQuery = String.join(" ", query);
    log.atDebug()
        .setMessage("Built query in {}ms")
        .addArgument(() -> Duration.between(start, Instant.now()).toMillis())
        .log();
    if (log.isDebugEnabled()) {
      log.debug(finalQuery.replace("?", "{}"), parameters.toArray());
    }
    List<CardStubWithCount> result = List.of();
    try {
      if (log.isDebugEnabled()) {
        this.start = Instant.now();
      }
      result =
          db.query(
              finalQuery,
              new CardStubRowMapper(searchFilter.collectionFilter() != null),
              parameters.toArray());
    } catch (DataIntegrityViolationException e) {
      log.error("Error executing SQL statement:");
      log.error(finalQuery.replace("?", "{}"), parameters.toArray());
      log.error("Search parameters: {}", searchFilter);
      log.error("Original exception:", e);
    }
    log.atDebug()
        .setMessage("Query executed in {}ms")
        .addArgument(() -> Duration.between(start, Instant.now()).toMillis())
        .log();
    long uniqueCount = !result.isEmpty() ? result.getFirst().uniqueCount() : 0;
    long totalCount = !result.isEmpty() ? result.getFirst().totalCount() : 0;
    log.debug("Query result size: {}", uniqueCount);
    List<CardStub> pageContent =
        result.stream()
            .map(
                sc ->
                    new CardStub(
                        sc.id(),
                        sc.dmId(),
                        sc.idText(),
                        sc.civs().stream()
                            .flatMap(Collection::stream)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet()),
                        sc.images().stream()
                            .map(imageService::makeImageUrl)
                            .filter(Objects::nonNull)
                            .toList(),
                        sc.amount()))
            .toList();
    return new SearchResult(
        new PageImpl<>(pageContent, searchFilter.pageable(), uniqueCount), totalCount);
  }

  private String questionMarks(int number) {
    return "?, ".repeat(number).substring(0, 3 * number - 2);
  }

  private void addCardColumnConditions(
      List<String> query, List<Object> parameters, @NonNull SearchFilter filter) {
    List<String> conditions = new ArrayList<>();
    addSetCondition(conditions, parameters, filter.setId());
    addRarityCondition(conditions, parameters, filter.rarityFilter());
    if (!filter.needsCivFilter()) {
      addTwinpactCondition(conditions, filter.twinpact());
    }
    if (!conditions.isEmpty()) {
      query.add(String.join(AND, conditions));
    }
  }

  private void addRarityCondition(
      List<String> query, List<Object> parameters, @Nullable RarityFilter rarityFilter) {
    if (rarityFilter == null) {
      return;
    }
    int rarityOrder = rarityService.getOrder(rarityFilter.rarityCode());
    switch (rarityFilter.range()) {
      case EQ:
        query.add("\"ORDER\" = ?");
        break;
      case LE:
        query.add("\"ORDER\" <= ?");
        break;
      case GE:
        query.add("\"ORDER\" >= ?");
        break;
    }
    parameters.add(rarityOrder);
  }

  private void addSetCondition(List<String> query, List<Object> parameters, @Nullable Long setId) {
    if (setId == null) {
      return;
    }
    query.add("c.\"SET\" = ?");
    parameters.add(setId);
  }

  private void addTwinpactCondition(List<String> query, FilterState twinpact) {
    if (twinpact == FilterState.EX) {
      query.add("c.TWINPACT IS NOT TRUE");
    }
    if (twinpact == FilterState.ONLY) {
      query.add("c.TWINPACT IS TRUE");
    }
  }

  private void addSortingAndPaging(
      List<String> conditions, List<Object> parameters, @NonNull Pageable pageable) {
    Sort sort = pageable.getSort();
    if (!sort.isUnsorted()) {
      conditions.add("ORDER BY");
      List<String> sorts =
          sort.stream()
              .map(
                  order ->
                      switch (order.getProperty()) {
                        case CardFacet.Columns.COST ->
                            "MAX(cf.COST) FILTER (WHERE cf.COST IS NOT NULL) %s NULLS LAST"
                                .formatted(order.getDirection().name());
                        case CardFacet.Columns.POWER_SORT ->
                            "MAX(cf.POWER_SORT) FILTER (WHERE cf.POWER_SORT IS NOT NULL) %s NULLS LAST"
                                .formatted(order.getDirection().name());
                        default ->
                            "\"%s\" %s".formatted(order.getProperty(), order.getDirection().name());
                      })
              .toList();
      conditions.add(String.join(",", sorts));
    }
    if (pageable.isUnpaged()) {
      return;
    }
    conditions.add("LIMIT ? OFFSET ?");
    parameters.add(pageable.getPageSize());
    parameters.add(pageable.getOffset());
  }

  void addFacetColumnFilters(
      List<String> query, List<Object> parameters, @NonNull SearchFilter searchFilter) {
    query.add("SELECT CARDS FROM CARD_FACETS WHERE (");
    List<String> conditions = new ArrayList<>();
    addCostCondition(conditions, parameters, searchFilter.minCost(), searchFilter.maxCost());
    addPowerCondition(conditions, parameters, searchFilter.minPower(), searchFilter.maxPower());
    addCardTypeCondition(conditions, searchFilter.cardType());
    addCardNameCondition(conditions, parameters, searchFilter.nameSearch());
    query.add(String.join(AND, conditions));
    query.add(")");
  }

  private void addCivSubquery(
      List<String> query,
      List<Object> parameters,
      boolean matchExactRainbowCivs,
      @NonNull Set<Civilization> includedCivs,
      @NonNull Set<Civilization> excludedCivs,
      @NonNull Boolean includeMono,
      @NonNull Boolean includeRainbow,
      @NonNull FilterState twinpactFilter) {
    int rainbowCivsCount = (int) includedCivs.stream().filter(c -> c != Civilization.ZERO).count();
    query.add("SELECT ID FROM (");
    String innerSelect = getInnerSelect(twinpactFilter);
    query.add(innerSelect);
    List<String> havingConditions = new ArrayList<>();

    if (twinpactFilter != FilterState.EX) {
      addTwinpactCivConditions(
          havingConditions,
          parameters,
          matchExactRainbowCivs,
          includedCivs,
          excludedCivs,
          includeMono,
          includeRainbow);
    }
    if (twinpactFilter != FilterState.ONLY) {
      addNonTwinpactCivConditions(
          havingConditions,
          parameters,
          matchExactRainbowCivs,
          includedCivs,
          excludedCivs,
          includeMono,
          includeRainbow);
      addFourSidedCardCondition(
          havingConditions,
          matchExactRainbowCivs,
          includedCivs,
          excludedCivs,
          includeMono,
          includeRainbow);
    }
    query.add(or(havingConditions));
    query.add(")"); // close outer query
  }

  private static String getInnerSelect(FilterState twinpactFilter) {
    String innerSelect =
        """
        SELECT
          c.ID,
          COALESCE(MAX(CASE WHEN cf.POSITION = 0 THEN cf.CIVS END), ARRAY[]) AS CIVS0,
          COALESCE(MAX(CASE WHEN cf.POSITION = 1 THEN cf.CIVS END), ARRAY[]) AS CIVS1%s
        """
            .formatted(twinpactFilter == FilterState.ONLY ? "" : ",");
    if (twinpactFilter != FilterState.ONLY) {
      innerSelect +=
          "COALESCE(MAX(CASE WHEN cf.POSITION = 2 THEN cf.CIVS END), ARRAY[]) AS CIVS2\n";
    }
    innerSelect +=
        """
        FROM CARDS c
        LEFT JOIN CARD_FACETS cf ON c.ID = cf.CARDS
        GROUP BY c.ID
        HAVING
        """;
    return innerSelect;
  }

  private void addTwinpactCivConditions(
      List<String> query,
      List<Object> parameters,
      boolean matchExactRainbowCivs,
      @NonNull Set<Civilization> includedCivs,
      @NonNull Set<Civilization> excludedCivs,
      @NonNull Boolean includeMono,
      @NonNull Boolean includeRainbow) {
    List<String> conditions = new ArrayList<>();
    conditions.add("c.TWINPACT IS TRUE AND (");
    if (!includeRainbow) {
      if (includedCivs.size() != Civilization.values().length) {
        conditions.add("(");
        twinpactCondition()
            .sidesAreEqual()
            .and()
            .sideHasSize(0, 1)
            .and()
            .containsInAnySideAnyOf(includedCivs)
            .add(conditions, parameters);
        conditions.add(")");
      } else {
        twinpactCondition().sidesAreEqual().and().sideHasSize(0, 1).add(conditions, parameters);
      }
    }
    if (!includeMono) {
      if (!matchExactRainbowCivs) {
        if (includedCivs.size() != Civilization.values().length) {
          conditions.add("((");
          // exclude monochromes
          twinpactCondition()
              .sidesAreDifferent()
              .or()
              .sideHasMoreCivsThan(0, 1)
              .add(conditions, parameters);
          conditions.add(") AND (");
          twinpactCondition().containsInAnySideAnyOf(includedCivs).add(conditions, parameters);
          conditions.add(")");
          if (!excludedCivs.isEmpty()) {
            conditions.add(") AND NOT (");
            twinpactCondition().containsInAnySideAnyOf(excludedCivs).add(conditions, parameters);
          }
          conditions.add(")");
        } else {
          twinpactCondition()
              .sidesAreDifferent()
              .or()
              .sideHasMoreCivsThan(0, 1)
              .add(conditions, parameters);
        }
      } else {
        conditions.add("((");
        twinpactCondition().containsAllAcrossAllSides(includedCivs).add(conditions, parameters);
        conditions.add(")");
        if (!excludedCivs.isEmpty()) {
          conditions.add(") AND NOT (");
          twinpactCondition().containsInAnySideAnyOf(excludedCivs).add(conditions, parameters);
        }
        conditions.add(")");
      }
    }
    if (includeMono && includeRainbow) {
      conditions.add("(");
      // include monochrome
      if (includedCivs.size() != Civilization.values().length) {
        conditions.add("(");
        twinpactCondition()
            .sidesAreEqual()
            .and()
            .sideHasSize(0, 1)
            .and()
            .containsInAnySideAnyOf(includedCivs)
            .add(conditions, parameters);
        conditions.add(")");
      } else {
        twinpactCondition().sidesAreEqual().and().sideHasSize(0, 1).add(conditions, parameters);
      }
      conditions.add(") OR (");
      // or include rainbow
      if (!matchExactRainbowCivs) {
        if (includedCivs.size() != Civilization.values().length) {
          conditions.add("((");
          // exclude monochromes
          twinpactCondition()
              .sidesAreDifferent()
              .or()
              .sideHasMoreCivsThan(0, 1)
              .add(conditions, parameters);
          conditions.add(") AND (");
          twinpactCondition().containsInAnySideAnyOf(includedCivs).add(conditions, parameters);
          conditions.add(")");
          if (!excludedCivs.isEmpty()) {
            conditions.add(") AND NOT (");
            twinpactCondition().containsInAnySideAnyOf(excludedCivs).add(conditions, parameters);
          }
          conditions.add(")");
        }
      } else {
        conditions.add("((");
        twinpactCondition().containsAllAcrossAllSides(includedCivs).add(conditions, parameters);
        conditions.add(")");
        if (!excludedCivs.isEmpty()) {
          conditions.add(") AND NOT (");
          twinpactCondition().containsInAnySideAnyOf(excludedCivs).add(conditions, parameters);
        }
        conditions.add(")");
      }
      conditions.add(")");
    }

    conditions.add(")");
    query.add(String.join(" ", conditions));
  }

  private void addNonTwinpactCivConditions(
      List<String> query,
      List<Object> parameters,
      boolean matchExactRainbowCivs,
      @NonNull Set<Civilization> includedCivs,
      @NonNull Set<Civilization> excludedCivs,
      @NonNull Boolean includeMono,
      @NonNull Boolean includeRainbow) {
    List<String> conditions = new ArrayList<>();
    conditions.add("c.TWINPACT IS FALSE AND (");
    if (!includeRainbow) {
      if (includedCivs.size() != Civilization.values().length) {
        addNonTwinpactMonochromeOnlyIncludesCondition(conditions, parameters, includedCivs);
      } else {
        addNonTwinpactMonochromeOnlyCondition(conditions, parameters);
      }
    }
    if (!includeMono) {
      if (!matchExactRainbowCivs) {
        if (includedCivs.size() != Civilization.values().length) {
          conditions.add("(");
          addNonTwinpactRainbowOnlyIncludesCondition(conditions, parameters, includedCivs);
          if (!excludedCivs.isEmpty()) {
            conditions.add(") AND NOT (");
            nonTwinpactCondition().containsInAnySideAnyOf(excludedCivs).add(conditions, parameters);
          }
          conditions.add(")");
        } else {
          nonTwinpactCondition().anySideHasMoreCivsThan(1).add(conditions, parameters);
        }
      } else {
        nonTwinpactCondition().anySideHasExactlyCivs(includedCivs).add(conditions, parameters);
      }
    }
    if (includeMono && includeRainbow) {
      conditions.add("(");
      // mono conditions
      if (includedCivs.size() != Civilization.values().length) {
        addNonTwinpactMonochromeOnlyIncludesCondition(conditions, parameters, includedCivs);
      } else {
        addNonTwinpactMonochromeOnlyCondition(conditions, parameters);
      }
      conditions.add(") OR (");
      // rainbow conditions
      if (!matchExactRainbowCivs) {
        if (includedCivs.size() != Civilization.values().length) {
          conditions.add("(");
          addNonTwinpactRainbowOnlyIncludesCondition(conditions, parameters, includedCivs);
          if (!excludedCivs.isEmpty()) {
            conditions.add(") AND NOT (");
            nonTwinpactCondition().containsInAnySideAnyOf(excludedCivs).add(conditions, parameters);
          }
          conditions.add(")");
        } else {
          nonTwinpactCondition().anySideHasMoreCivsThan(1).add(conditions, parameters);
        }
      } else {
        nonTwinpactCondition().anySideHasExactlyCivs(includedCivs).add(conditions, parameters);
      }
      conditions.add(")");
    }
    conditions.add(")");
    query.add(String.join(" ", conditions));
  }

  private static void addNonTwinpactRainbowOnlyIncludesCondition(
      List<String> conditions, List<Object> parameters, Set<Civilization> includedCivs) {
    conditions.add("(");
    extracted(conditions, parameters, includedCivs, 0);
    conditions.add(") OR (");
    extracted(conditions, parameters, includedCivs, 1);
    conditions.add(") OR (");
    extracted(conditions, parameters, includedCivs, 2);
    conditions.add(")");
  }

  private static void extracted(
      List<String> conditions, List<Object> parameters, Set<Civilization> includedCivs, int side) {
    nonTwinpactCondition()
        .sideHasMoreCivsThan(side, 1)
        .and()
        .sideContainsAnyOf(side, includedCivs)
        .add(conditions, parameters);
  }

  private void addNonTwinpactMonochromeOnlyIncludesCondition(
      List<String> conditions, List<Object> parameters, Set<Civilization> included) {
    conditions.add("(");
    addSideIsMonochromeAndContainsAnyOf(0, included, parameters, conditions);
    conditions.add(") OR (");
    addSideIsMonochromeAndContainsAnyOf(1, included, parameters, conditions);
    conditions.add(") OR (");
    addSideIsMonochromeAndContainsAnyOf(2, included, parameters, conditions);
    conditions.add(")");
  }

  private void addSideIsMonochromeAndContainsAnyOf(
      int side, Set<Civilization> civs, List<Object> parameters, List<String> query) {
    nonTwinpactCondition()
        .sideHasSize(side, 1)
        .and()
        .sideContainsAnyOf(side, civs)
        .add(query, parameters);
  }

  private void addNonTwinpactMonochromeOnlyCondition(
      List<String> conditions, List<Object> parameters) {
    conditions.add("(");
    addSideIsMonochrome(0, parameters, conditions);
    conditions.add(") OR (");
    addSideIsMonochrome(1, parameters, conditions);
    conditions.add(") OR (");
    addSideIsMonochrome(2, parameters, conditions);
    conditions.add(")");
  }

  private void addSideIsMonochrome(int side, List<Object> parameters, List<String> query) {
    nonTwinpactCondition().sideHasSize(side, 1).add(query, parameters);
  }

  private void addFourSidedCardCondition(
      List<String> conditions,
      boolean matchExactRainbowCivs,
      @NonNull Set<Civilization> includedCivs,
      @NonNull Set<Civilization> excludedCivs,
      @NonNull Boolean includeMono,
      @NonNull Boolean includeRainbow) {
    if (includeMono) {
      if (includedCivs.contains(Civilization.WATER)
          || includedCivs.contains(Civilization.FIRE)
          || includedCivs.contains(Civilization.NATURE)) {
        conditions.add("(c.OFFICIAL_ID = 'dmbd13-001')");
        return;
      }
    }
    if (includeRainbow) {
      if (!matchExactRainbowCivs) {
        if (includedCivs.contains(Civilization.WATER)
            || includedCivs.contains(Civilization.FIRE)
            || includedCivs.contains(Civilization.NATURE)) {
          if (!excludedCivs.contains(Civilization.WATER)
              && !excludedCivs.contains(Civilization.FIRE)
              && !excludedCivs.contains(Civilization.NATURE)) {
            conditions.add("(c.OFFICIAL_ID = 'dmbd13-001')");
          }
        }
      } else {
        if (includedCivs.equals(
            Set.of(Civilization.WATER, Civilization.FIRE, Civilization.NATURE))) {
          conditions.add("(c.OFFICIAL_ID = 'dmbd13-001')");
        }
      }
    }
  }

  private void addCostCondition(
      List<String> conditions,
      List<Object> parameters,
      @Nullable Integer minCost,
      @Nullable Integer maxCost) {
    if (minCost == null && maxCost == null) {
      return;
    }
    List<String> costConds = new ArrayList<>();
    if (minCost != null) {
      costConds.add("COST >= ?");
      parameters.add(minCost);
    }
    if (maxCost != null) {
      costConds.add("COST <= ?");
      parameters.add(maxCost);
    }
    String costCondition = String.join(AND, costConds);

    if (Integer.valueOf(0).equals(minCost) || Integer.valueOf(0).equals(maxCost)) {
      costCondition = "(" + costCondition + " OR " + "COST IS NULL)";
    } else {
      costCondition = "(" + costCondition + ")";
    }
    conditions.add(costCondition);
  }

  private void addPowerCondition(
      List<String> conditions,
      List<Object> parameters,
      @Nullable Integer minPower,
      @Nullable Integer maxPower) {
    if (minPower == null && maxPower == null) {
      return;
    }
    List<String> powerConds = new ArrayList<>();
    if (minPower != null) {
      powerConds.add("POWER >= ?");
      parameters.add(minPower);
    }
    if (maxPower != null) {
      powerConds.add("POWER <= ?");
      parameters.add(maxPower);
    }
    String powerCondition = String.join(AND, powerConds);

    powerCondition = "(" + powerCondition + ")";
    conditions.add(powerCondition);
  }

  private void addCardTypeCondition(List<String> conditions, @Nullable CardType cardType) {
    if (cardType == null) {
      return;
    }
    conditions.add(typeCondition.forType(cardType));
  }

  private void addCardNameCondition(
      List<String> conditions, List<Object> parameters, @Nullable String nameSearch) {
    if (nameSearch == null || nameSearch.isBlank()) {
      return;
    }
    conditions.add(FACET_NAME_CONDITION);
    parameters.add(nameSearch);
  }

  /**
   * Retrieves facet IDs for cards whose effects contain the search text. Searches all effects
   * including child effects. Since child effects are only associated with their parent effects (not
   * directly with card facets), we need to join through the parent effect to reach the facet.
   *
   * @param effectSearch The text to search for in effect texts
   * @return Set of facet IDs that have matching effects
   */
  private Set<Long> getEffectFacetIds(String effectSearch) {
    String sql =
        """
        SELECT DISTINCT fe.CARD_FACETS
        FROM EFFECT e
        LEFT JOIN EFFECT parent ON e.PARENT = parent.ID
        JOIN FACET_EFFECT fe ON COALESCE(parent.ID, e.ID) = fe.EFFECT
        WHERE LOCATE(?, e.TEXT) > 0
        """;

    List<Long> results = db.queryForList(sql, Long.class, effectSearch.toUpperCase());

    log.debug("Effect search '{}' found {} matching facets", effectSearch, results.size());
    return new HashSet<>(results);
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

  public static class CardStubRowMapper implements RowMapper<CardQueryService.CardStubWithCount> {

    private final boolean hasAmount;

    CardStubRowMapper(boolean hasAmount) {
      this.hasAmount = hasAmount;
    }

    @Override
    public CardStubWithCount mapRow(ResultSet rs, int rowNum) throws SQLException {
      List<Set<Civilization>> civs = List.of();
      if (rs.getArray(CIV_AGG) != null) {
        civs = new ArrayList<>();
        ResultSet outerArray = rs.getArray(CIV_AGG).getResultSet();
        while (outerArray.next()) {
          if (outerArray.getArray(2) != null) {
            ResultSet innerArray = outerArray.getArray(2).getResultSet();
            Set<Civilization> facetCivs = EnumSet.noneOf(Civilization.class);
            while (innerArray.next()) {
              facetCivs.add(Civilization.values()[innerArray.getByte(2)]);
            }
            civs.add(facetCivs);
          }
        }
      }
      List<String> images = List.of();
      if (rs.getArray(IMAGE_AGG) != null) {
        images = new ArrayList<>();
        ResultSet array = rs.getArray((IMAGE_AGG)).getResultSet();
        while (array.next()) {
          String image = array.getString(2);
          images.add(image);
        }
      }
      int amount = 0;
      long amountSum = 0;
      if (this.hasAmount) {
        amount = rs.getInt(AMOUNT);
        amountSum = rs.getLong(TOTAL_COUNT);
      }
      return new CardStubWithCount(
          rs.getLong(Columns.ID),
          rs.getString(Columns.OFFICIAL_ID),
          rs.getString(Columns.ID_TEXT),
          civs,
          images,
          amount,
          amountSum,
          rs.getLong(UNIQUE_COUNT));
    }
  }
}
