package net.dmcollection.model.card;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("CARDS")
public record CardEntity(
    @Id Long id,
    String officialId,
    String idText,
    @MappedCollection(idColumn = "CARDS", keyColumn = "POSITION") List<CardFacet> facets,
    AggregateReference<OfficialSet, Long> set,
    Boolean twinpact,
    @Column("RARITY") RarityCode rarityCode) {

  public CardEntity {
    if (rarityCode == RarityCode.NONE) {
      rarityCode = null;
    }
  }

  public static class Columns {
    private Columns() {}

    public static final String ID = "ID";
    public static final String OFFICIAL_ID = "OFFICIAL_ID";
    public static final String ID_TEXT = "ID_TEXT";
    public static final String SET = "SET";
    public static final String TWINPACT = "TWINPACT";
    public static final String RARITY = "RARITY";
  }
}
