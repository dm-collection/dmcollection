package net.dmcollection.model.converters;

import jakarta.annotation.Nonnull;
import net.dmcollection.model.card.CardCost;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class CostReadConverter implements Converter<Integer, CardCost> {

  @Override
  public CardCost convert(@Nonnull Integer source) {
    return CardCost.parseCost(source);
  }
}
