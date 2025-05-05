package net.dmcollection.server.card;

import static org.assertj.core.api.Assertions.assertThat;

import net.dmcollection.model.card.Species;
import net.dmcollection.model.card.SpeciesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class SpeciesRepositoryIntegrationTest {

  @Autowired SpeciesRepository repository;

  @Test
  void speciesCanBeStoredAndRetrieved() {
    String species = "レッド・コマンド・ドラゴン";
    Species toSave = new Species(null, species);
    repository.save(toSave);
    var found = repository.findBySpecies(species);
    assertThat(found)
        .isNotEmpty()
        .hasValueSatisfying(s -> assertThat(s.species()).isEqualTo(species));
  }
}
