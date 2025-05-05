package net.dmcollection.model.card;

import org.springframework.data.annotation.Id;

public record Species(@Id Long id, String species) {}
