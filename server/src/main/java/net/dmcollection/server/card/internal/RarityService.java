package net.dmcollection.server.card.internal;

import java.util.Comparator;
import java.util.List;
import net.dmcollection.model.card.Rarity;
import net.dmcollection.model.card.RarityCode;
import net.dmcollection.model.card.RarityRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class RarityService {

  private final RarityRepository rarityRepository;

  private final List<Rarity> rarities;

  public RarityService(RarityRepository rarityRepository) {
    this.rarityRepository = rarityRepository;
    this.rarities =
        rarityRepository.findAll().stream().sorted(Comparator.comparing(Rarity::order)).toList();
  }

  public int getOrder(@NonNull RarityCode rarityCode) {
    return rarities.stream()
        .filter(r -> r.code().equals(rarityCode))
        .findFirst()
        .map(Rarity::order)
        .orElse(0);
  }

  public List<Rarity> getRarities() {
    if (rarities.isEmpty()) {
      this.rarities.addAll(rarityRepository.findAll());
    }
    return rarities;
  }
}
