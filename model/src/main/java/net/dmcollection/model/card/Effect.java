package net.dmcollection.model.card;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Table("EFFECT")
public record Effect(
    @Id Long id,
    String text,
    @MappedCollection(idColumn = "PARENT_EFFECT", keyColumn = "POSITION")
        List<GroupedEffect> children) {}
