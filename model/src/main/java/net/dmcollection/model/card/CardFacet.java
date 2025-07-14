package net.dmcollection.model.card;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("CARD_FACETS")
public record CardFacet(
    @Id Long id,
    @Nonnull Integer position,
    String name,
    @Column("COST") CardCost cost,
    @Column("CIVS") List<Integer> civilizations,
    @Column("POWER") Integer power,
    @Column("POWER_SORT") Integer powerSort,
    @Column("POWER_TXT") Power powerText,
    String type,
    @MappedCollection(idColumn = "CARD_FACETS", keyColumn = "POSITION") List<FacetSpecies> species,
    String imageFilename) {

  public static final class Columns {
    private Columns() {}

    public static final String NAME = "NAME";
    public static final String POWER_SORT = "POWER_SORT";
    public static final String COST = "COST";
  }

  public CardFacet {
    if (civilizations != null) {
      Collections.sort(civilizations);
    }
  }

  public Set<Civilization> getCivs() {
    return civilizations == null ? Set.of() : Civilization.fromInts(civilizations);
  }
}
