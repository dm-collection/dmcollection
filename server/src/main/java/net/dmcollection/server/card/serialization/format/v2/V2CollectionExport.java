package net.dmcollection.server.card.serialization.format.v2;

import java.util.List;
import net.dmcollection.server.card.serialization.format.Header;
import net.dmcollection.server.card.serialization.format.MetaData;

public record V2CollectionExport(Header version, MetaData meta, List<V2CollectionEntry> cards) {}
