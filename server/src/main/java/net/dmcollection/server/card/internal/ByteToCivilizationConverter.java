package net.dmcollection.server.card.internal;

import net.dmcollection.model.card.Civilization;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

@ReadingConverter
public class ByteToCivilizationConverter implements Converter<Byte, Civilization> {
  @Override
  public Civilization convert(@NonNull Byte source) {
    return Civilization.values()[source];
  }
}
