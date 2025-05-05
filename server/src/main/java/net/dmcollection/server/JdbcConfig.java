package net.dmcollection.server;

import java.util.Arrays;
import java.util.List;
import net.dmcollection.model.converters.CostReadConverter;
import net.dmcollection.model.converters.CostWriteConverter;
import net.dmcollection.model.converters.PowerToStringConverter;
import net.dmcollection.model.converters.StringToPowerConverter;
import net.dmcollection.model.converters.ByteToCivilizationConverter;
import net.dmcollection.model.converters.CivilizationToByteConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.lang.NonNull;

@Configuration
@EnableJdbcRepositories
@EnableJdbcAuditing
public class JdbcConfig extends AbstractJdbcConfiguration {

  @Override
  @NonNull
  protected List<?> userConverters() {
    return Arrays.asList(
        new CivilizationToByteConverter(),
        new ByteToCivilizationConverter(),
        new CostWriteConverter(),
        new CostReadConverter(),
        new PowerToStringConverter(),
        new StringToPowerConverter());
  }
}
