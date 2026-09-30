package net.dmcollection.server.card;

import java.time.LocalDate;
import java.util.List;

public record PrintingStub(
    int id,
    String officialId,
    String idText,
    String setCode,
    LocalDate setRelease,
    int amount,
    List<String> imageFileNames) {}
