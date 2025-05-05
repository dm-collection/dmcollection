package net.dmcollection.model.card;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

public interface SetRepository extends ListCrudRepository<OfficialSet, Long> {
  Optional<OfficialSet> findByDmId(@Nonnull String dmId);

  Optional<OfficialSet> findFirstByName(String name);

  List<OfficialSet> findByOrderByDmId();

  List<OfficialSet> findByOrderByReleaseDesc();

  List<OfficialSet> findByOrderByIdDesc();

  boolean existsByDmId(String dmId);
}
