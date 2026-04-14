package net.dmcollection.server.card.serialization.format;

import java.time.OffsetDateTime;

public record Header(int version, OffsetDateTime exportDateTime, String type) {

  public static int EXPORT_FORMAT_VERSION = 2;
  public static String EXPORT_TYPE_COLLECTION = "collection";
}
