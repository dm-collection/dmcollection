package net.dmcollection.model.card;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface EffectRepository extends ListCrudRepository<Effect, Long> {
  Optional<Effect> findByText(String text);
}
