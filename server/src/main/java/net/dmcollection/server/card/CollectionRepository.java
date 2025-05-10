package net.dmcollection.server.card;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.dmcollection.server.card.CardCollection.CollectionIds;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.repository.CrudRepository;

public interface CollectionRepository extends CrudRepository<CardCollection, Long> {

  List<CardCollection> findByOwnerAndPrimaryIsFalseOrderByUpdatedAtDesc(UUID userId);

  Optional<CardCollection> findByPublicIdAndOwnerAndPrimaryIsFalse(UUID publicId, UUID userId);

  Optional<CardCollection> findByOwnerAndPrimaryIsTrue(UUID userId);

  boolean existsByOwnerAndPrimaryIsTrue(UUID userId);

  Optional<CollectionIds> findIdsByOwnerAndPrimaryIsTrue(UUID userId);
}
