package net.dmcollection.server.card.internal;

import java.sql.JDBCType;
import net.dmcollection.model.card.Civilization;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.mapping.JdbcValue;

@WritingConverter
public class CivilizationToByteConverter implements Converter<Civilization, JdbcValue> {
  @Override
  public JdbcValue convert(Civilization civilization) {
    return JdbcValue.of(Integer.valueOf(civilization.ordinal()).byteValue(), JDBCType.TINYINT);
  }
}
