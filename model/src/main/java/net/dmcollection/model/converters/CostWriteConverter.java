package net.dmcollection.model.converters;

import net.dmcollection.model.card.CardCost;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class CostWriteConverter implements Converter<CardCost, Integer> {
  @Override
  public Integer convert(CardCost cost) {
    return cost.value();
  }
}
