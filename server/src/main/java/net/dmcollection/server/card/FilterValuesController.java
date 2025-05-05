package net.dmcollection.server.card;

import net.dmcollection.model.card.Species;
import net.dmcollection.server.card.internal.RarityService;
import net.dmcollection.server.card.internal.SpeciesService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FilterValuesController {

  private final SetService setService;
  private final SpeciesService speciesService;
  private final RarityService rarityService;

  public FilterValuesController(
      SetService setService, SpeciesService speciesService, RarityService rarityService) {
    this.setService = setService;
    this.speciesService = speciesService;
    this.rarityService = rarityService;
  }

  @GetMapping("/api/sets")
  ResponseEntity<?> getSets() {
    return ResponseEntity.ok(setService.getSets());
  }

  @GetMapping("/api/species")
  ResponseEntity<?> getSpecies() {
    return ResponseEntity.ok(speciesService.getSpecies().stream().map(Species::species));
  }

  @GetMapping("/api/rarities")
  ResponseEntity<?> getRarities() {
    return ResponseEntity.ok(rarityService.getRarities());
  }
}
