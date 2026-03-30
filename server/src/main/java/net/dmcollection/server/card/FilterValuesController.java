package net.dmcollection.server.card;

import static net.dmcollection.server.jooq.generated.tables.Race.RACE;

import net.dmcollection.server.card.internal.RarityService;
import org.jooq.DSLContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FilterValuesController {

  private final SetService setService;
  private final DSLContext dsl;
  private final RarityService rarityService;

  public FilterValuesController(SetService setService, DSLContext dsl, RarityService rarityService) {
    this.setService = setService;
    this.dsl = dsl;
    this.rarityService = rarityService;
  }

  @GetMapping("/api/sets")
  ResponseEntity<?> getSets() {
    return ResponseEntity.ok(setService.getSets());
  }

  @GetMapping("/api/species")
  ResponseEntity<?> getSpecies() {
    return ResponseEntity.ok(dsl.select(RACE.NAME).from(RACE).orderBy(RACE.NAME).fetch(RACE.NAME));
  }

  @GetMapping("/api/rarities")
  ResponseEntity<?> getRarities() {
    return ResponseEntity.ok(rarityService.getRarities());
  }
}
