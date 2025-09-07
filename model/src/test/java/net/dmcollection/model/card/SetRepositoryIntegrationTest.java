package net.dmcollection.model.card;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@ActiveProfiles("test")
class SetRepositoryIntegrationTest {

  @Autowired SetRepository repository;

  private final OfficialSet set = new OfficialSet(null, "dm01", "DM-01", LocalDate.of(2002, 5, 30));

  @Test
  void savesAndFindsById() {
    var saved = repository.save(set);
    var dbEntry = repository.findById(saved.id());
    assertThat(dbEntry)
        .hasValueSatisfying(
            v -> {
              assertThat(v.dmId()).isEqualTo("dm01");
              assertThat(v.name()).isEqualTo("DM-01");
              assertThat(v.release()).isEqualTo("2002-05-30");
            });
  }

  @Test
  void findsByDmId() {
    repository.save(set);
    var dbEntry = repository.findByDmId(set.dmId());
    assertThat(dbEntry)
        .hasValueSatisfying(
            v -> {
              assertThat(v.dmId()).isEqualTo("dm01");
              assertThat(v.name()).isEqualTo("DM-01");
              assertThat(v.release()).isEqualTo("2002-05-30");
            });
  }

  @Test
  void ordersByReleaseDate() {
    var first = repository.save(set);
    var second =
        repository.save(
            new OfficialSet(
                null, "dm02", "DM-02 進化獣降臨(マスター・オブ・エボリューション)", LocalDate.of(2002, 7, 25)));
    assertThat(repository.findByOrderByReleaseDesc()).containsExactly(second, first);
  }
}
