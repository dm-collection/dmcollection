package net.dmcollection.server.card;

import java.util.List;

public record CardStub(int id, String name, List<PrintingStub> printings) {}
