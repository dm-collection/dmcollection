package net.dmcollection.server.card;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.dmcollection.model.card.OfficialSet;
import net.dmcollection.model.card.SetRepository;
import net.dmcollection.server.card.CardService.SetDto;
import org.springframework.stereotype.Service;

@Service
public class SetService {
  private final SetRepository setRepository;

  public SetService(SetRepository setRepository) {
    this.setRepository = setRepository;
  }

  public Map<Long, OfficialSet> getSetsById() {
    return setRepository.findAll().stream()
        .collect(Collectors.toMap(OfficialSet::id, Function.identity()));
  }

  public Optional<OfficialSet> getSet(@Nonnull String dmId) {
    return setRepository.findByDmId(dmId);
  }

  public Optional<OfficialSet> getSet(@Nonnull Long id) {
    return setRepository.findById(id);
  }

  public List<SetDto> getSets() {
    return setRepository.findByOrderByReleaseDesc().stream()
        .map(set -> new SetDto(set.id(), set.dmId(), set.name()))
        .toList();
  }
}
