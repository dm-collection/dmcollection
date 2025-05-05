package net.dmcollection.model;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@AutoConfiguration
@EnableJdbcRepositories(basePackages = "net.dmcollection.model.card")
public class AutoConfigure {}
