package net.dmcollection.server.card.serialization.format;

import java.time.LocalDateTime;

public record Header(int version, LocalDateTime exportDateTime, String type) {

  public static int EXPORT_FORMAT_VERSION = 2;
  public static String EXPORT_TYPE_COLLECTION = "collection";
}
