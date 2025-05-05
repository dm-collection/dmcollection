package net.dmcollection.server.card.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.dmcollection.model.card.Species;
import net.dmcollection.model.card.SpeciesRepository;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SpeciesService {

  private static final Logger log = LoggerFactory.getLogger(SpeciesService.class);
  private final JdbcTemplate jdbcTemplate;

  private final SpeciesRepository speciesRepository;

  private final Map<Long, Set<Long>> facetsBySpeciesId;
  private final List<Species> species;

  public SpeciesService(JdbcTemplate jdbcTemplate, SpeciesRepository speciesRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.speciesRepository = speciesRepository;
    this.facetsBySpeciesId = new HashMap<>();
    this.species = new ArrayList<>();
  }

  public List<Species> getSpecies() {
    return new UnmodifiableList<>(species);
  }

  public Set<Long> getFacetIdsForSpeciesSearch(String search) {
    return species.stream()
        .filter(s -> s.species().contains(search))
        .map(Species::id)
        .flatMap(id -> facetsBySpeciesId.get(id).stream())
        .collect(Collectors.toSet());
  }

  public void initialize() {
    if (!facetsBySpeciesId.isEmpty()) {
      return;
    }
    species.addAll(speciesRepository.findAll());
    species.sort(Comparator.comparing(Species::species));
    jdbcTemplate.query(
        "SELECT SPECIES, CARD_FACETS FROM FACET_SPECIES",
        (rs) -> {
          long speciesId = rs.getLong(1);
          long facetId = rs.getLong(2);
          facetsBySpeciesId.computeIfAbsent(speciesId, k -> new HashSet<>()).add(facetId);
        });
    log.info("Finished initialization of facets by species");
  }
}
