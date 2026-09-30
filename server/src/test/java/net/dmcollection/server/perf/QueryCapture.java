package net.dmcollection.server.perf;

import java.util.ArrayList;
import java.util.List;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** jOOQ listener that records the SQL of executed queries while recording is switched on. */
public class QueryCapture implements ExecuteListener {

  private volatile List<String> captured;

  public void start() {
    captured = new ArrayList<>();
  }

  public List<String> stop() {
    var result = captured;
    captured = null;
    return result == null ? List.of() : result;
  }

  @Override
  public void executeStart(ExecuteContext ctx) {
    var recording = captured;
    if (recording != null && ctx.query() != null) {
      recording.add(ctx.dsl().renderInlined(ctx.query()));
    }
  }

  @TestConfiguration
  public static class Config {
    @Bean
    QueryCapture queryCapture() {
      return new QueryCapture();
    }

    @Bean
    ExecuteListenerProvider queryCaptureProvider(QueryCapture capture) {
      return new DefaultExecuteListenerProvider(capture);
    }
  }
}
