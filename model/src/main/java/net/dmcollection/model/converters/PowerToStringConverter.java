package net.dmcollection.model.converters;

import net.dmcollection.model.card.Power;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class PowerToStringConverter implements Converter<Power, String> {

  @Override
  public String convert(Power source) {
    return source.value();
  }
}
