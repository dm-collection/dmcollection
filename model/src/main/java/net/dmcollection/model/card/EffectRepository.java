package net.dmcollection.model.card;

import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface EffectRepository extends ListCrudRepository<Effect, Long> {
  List<Effect> findByText(String text);
}
