package net.dmcollection.model.card;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;

@Table("EFFECT")
public record Effect(
    @Id Long id,
    int position,
    String text,
    @MappedCollection(idColumn = "PARENT", keyColumn = "POSITION") List<Effect> children) {

  public Effect(String text) {
    this(null, 0, text, new ArrayList<>());
  }

  public Effect(String text, List<Effect> children) {
    this(null, 0, text, children);
  }

  public Effect(int position, String text) {
    this(null, position, text, null);
  }
}
