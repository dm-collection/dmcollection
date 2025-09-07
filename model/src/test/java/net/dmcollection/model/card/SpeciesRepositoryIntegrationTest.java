package net.dmcollection.model.card;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@ActiveProfiles("test")
class SpeciesRepositoryIntegrationTest {

  @Autowired SpeciesRepository repository;

  @Test
  void savesAndFindsBySpecies() {
    repository.save(new Species(null, "レッド・コマンド・ドラゴン"));
    var dbEntry = repository.findBySpecies("レッド・コマンド・ドラゴン");
    assertThat(dbEntry)
        .isNotEmpty()
        .hasValueSatisfying(s -> assertThat(s.species()).isEqualTo("レッド・コマンド・ドラゴン"));
  }
}
