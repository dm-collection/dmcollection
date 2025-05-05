package net.dmcollection.model.card;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

public interface CardRepository
    extends ListCrudRepository<CardEntity, Long>,
        PagingAndSortingRepository<CardEntity, Long>,
        QueryByExampleExecutor<CardEntity> {
  Optional<CardEntity> findByOfficialId(String officialId);

  @Query("SELECT c.* FROM CARDS c WHERE c.official_id IN (:officialIds)")
  List<CardEntity> findByOfficialIdIn(Collection<String> officialIds);

  boolean existsByOfficialId(String officialId);

  long countBySet(long set);
}
