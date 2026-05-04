package net.dmcollection.server.card;

import java.util.UUID;
import net.dmcollection.server.user.CurrentUserId;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HistoryController {

  private final HistoryService historyService;

  public HistoryController(HistoryService historyService) {
    this.historyService = historyService;
  }

  @GetMapping(value = "/api/history/latest", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getLatest(
      @CurrentUserId UUID userId, @RequestParam(required = false) Integer limit) {
    String latest = historyService.getLatest(userId, limit == null ? 5 : limit);
    return ResponseEntity.ok(latest);
  }
}
