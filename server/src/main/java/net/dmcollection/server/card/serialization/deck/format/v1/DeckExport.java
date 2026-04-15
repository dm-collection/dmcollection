package net.dmcollection.server.card.serialization.deck.format.v1;

import java.time.LocalDateTime;
import java.util.List;

public record DeckExport(
    int version,
    LocalDateTime exportDateTime,
    String title,
    int cardCount,
    int countWithoutDuplicates,
    List<DeckCardExport> cards) {}
