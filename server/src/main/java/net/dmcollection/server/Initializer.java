package net.dmcollection.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.dmcollection.model.card.Civilization;
import net.dmcollection.server.card.internal.SpeciesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class Initializer {

  private static final Logger log = LoggerFactory.getLogger(Initializer.class);
  private final AppProperties properties;
  private final JdbcTemplate jdbcTemplate;
  private final SpeciesService speciesService;

  public Initializer(
      AppProperties properties, JdbcTemplate jdbcTemplate, SpeciesService speciesService) {
    this.properties = properties;
    this.jdbcTemplate = jdbcTemplate;
    this.speciesService = speciesService;
  }

  @EventListener
  public void initialize(ContextRefreshedEvent event) {
    Thread civLoading = Thread.ofVirtual().start(this::loadFacetCivs);
    Thread facetFilterInit = Thread.ofVirtual().start(speciesService::initialize);
    try {
      civLoading.join();
      facetFilterInit.join();
    } catch (InterruptedException e) {
      log.error("Error while waiting for initialization to finish", e);
    }
  }

  private void loadFacetCivs() {
    CsvMapper mapper = new CsvMapper();
    CsvSchema schema =
        mapper
            .schemaFor(CsvFacet.class)
            .withHeader()
            .withColumnReordering(true)
            .withNullValue("NULL");
    try (InputStream facetsIn =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("net/dmcollection/card-data/csv/card_facets.csv")) {
      if (facetsIn == null) {
        log.error("Resource csv/card_facets.csv not found.");
        return;
      }
      String sql = "MERGE INTO CARD_FACETS (ID, CIVS) KEY(ID) VALUES (?, ?)";
      MappingIterator<CsvFacet> it =
          mapper.readerFor(CsvFacet.class).with(schema).readValues(facetsIn);
      long line = 1;
      while (it.hasNextValue()) {
        line++;
        CsvFacet csvFacet = it.nextValue();
        if (csvFacet.civs != null) {
          try {
            jdbcTemplate.update(sql, csvFacet.toArgs());
          } catch (NumberFormatException nfe) {
            log.error("Unable to parse civs {} from line {}", csvFacet.civs, line);
          }
        }
      }
      log.info("{} facets updated.", line - 1);
    } catch (IOException e) {
      log.error("Error loading facets:", e);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record CsvFacet(@JsonProperty("ID") long id, @JsonProperty("CIVS") String civs) {
    Object[] toArgs() {
      List<Object> args = new ArrayList<>(10);
      args.add(id);
      Set<Civilization> civSet = Set.of();
      if (civs != null) {
        civSet =
            Arrays.stream(civs.replace("[", "").replace("]", "").split(",\\s*"))
                .filter(s -> !s.isBlank())
                .map(s -> Civilization.values()[Integer.parseInt(s)])
                .collect(Collectors.toSet());
      }
      args.add(Civilization.toInts(civSet).toArray());
      return args.toArray();
    }
  }
}
