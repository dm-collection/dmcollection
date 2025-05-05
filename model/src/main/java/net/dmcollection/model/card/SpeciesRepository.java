package net.dmcollection.model.card;

import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

public interface SpeciesRepository extends ListCrudRepository<Species, Long> {

  Optional<Species> findBySpecies(String species);

  boolean existsBySpecies(String species);
}
