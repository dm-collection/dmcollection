package net.dmcollection.server.card;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.dmcollection.server.Id;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CollectionRepository extends CrudRepository<CardCollection, UUID> {

  List<CardCollection> findByOwnerAndPrimaryIsFalseOrderByUpdatedAtDesc(UUID userId);

  Optional<CardCollection> findByIdAndOwnerAndPrimaryIsFalse(UUID collectionId, UUID userId);

  Optional<CardCollection> findByOwnerAndPrimaryIsTrue(UUID userId);

  boolean existsByOwnerAndPrimaryIsTrue(UUID userId);

  @Query("SELECT ID FROM COLLECTIONS WHERE \"USER\" = :userId AND \"PRIMARY\" IS TRUE")
  Optional<Id> findIdByOwnerAndPrimaryIsTrue(@Param("userId") UUID userId);
}
