package net.dmcollection.model.converters;

import jakarta.annotation.Nonnull;
import net.dmcollection.model.card.Power;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToPowerConverter implements Converter<String, Power> {

  @Override
  public Power convert(@Nonnull String source) {
    return new Power(source);
  }
}
