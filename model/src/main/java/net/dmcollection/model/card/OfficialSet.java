package net.dmcollection.model.card;

import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("CARD_SET")
public record OfficialSet(@Id Long id, String dmId, String name, LocalDate release) {

  public static class Columns {
    private Columns() {}

    public static final String RELEASE = "RELEASE";
  }

  public OfficialSet withId(Long id) {
    return new OfficialSet(id, dmId, name, release);
  }
}
