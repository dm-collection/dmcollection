package net.dmcollection.server.perf;

import static net.dmcollection.server.card.SearchFilterApi.SORT_RELEASE;
import static net.dmcollection.server.jooq.generated.Tables.CARD;
import static net.dmcollection.server.jooq.generated.Tables.COLLECTION_ENTRY;
import static net.dmcollection.server.jooq.generated.Tables.PRINTING;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import net.dmcollection.server.AppProperties;
import net.dmcollection.server.IntegrationTestBase;
import net.dmcollection.server.card.CollectionService;
import net.dmcollection.server.card.SearchFilterApi;
import net.dmcollection.server.card.internal.CardQueryService;
import net.dmcollection.server.card.internal.RarityService;
import net.dmcollection.server.card.internal.SearchFilter;
import net.dmcollection.server.carddata.CardDataImportService;
import net.dmcollection.server.carddata.CardDataJson;
import net.dmcollection.server.testutils.SearchBuilder;
import net.dmcollection.server.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Query performance benchmark.
 *
 * <p>Run with
 *
 * <pre>./mvnw -pl server verify -Pperf</pre>
 *
 * Properties: {@code -Dperf.collection=<export file>} (default {@code
 * server/perf-data/collection.json}, falls back to a synthetic collection), {@code
 * -Dperf.warmup=20}, {@code -Dperf.iterations=50}. Reports are written to {@code
 * server/perf-results/}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(QueryCapture.Config.class)
@TestPropertySource(properties = "logging.level.net.dmcollection=INFO")
class CardQueryBenchmark extends IntegrationTestBase {

  private static final Path RESULTS_DIR = Path.of("perf-results");
  private static final Path LATEST_CSV = RESULTS_DIR.resolve("latest.csv");

  @Autowired CardDataImportService importService;
  @Autowired CardQueryService cardQueryService;
  @Autowired CollectionService collectionService;
  @Autowired RarityService rarityService;
  @Autowired AppProperties appProperties;
  @Autowired JsonMapper objectMapper;
  @Autowired QueryCapture capture;

  private User user;
  private String collectionSource;

  record Case(String name, Supplier<?> call) {}

  record Stats(double min, double median, double p95, double max, double mean) {}

  record Result(Case benchCase, Stats stats, List<String> queries, List<String> plans) {}

  @BeforeAll
  void prepare() throws Exception {
    try (InputStream is =
        getClass().getResourceAsStream("/net/dmcollection/card-data/json/card-data.json")) {
      importService.importCardData(objectMapper.readValue(is, CardDataJson.class));
    }
    cardTypeResolver.loadNameToId();
    rarityService.loadRarities();

    user = createUser("perf-");
    Path file = Path.of(System.getProperty("perf.collection", "perf-data/collection.json"));
    if (Files.isRegularFile(file)) {
      collectionService.importCollection(user.getId(), Files.readAllBytes(file));
      collectionSource = "file " + file;
    } else {
      createSyntheticCollection();
      collectionSource = "SYNTHETIC (no file at " + file + ")";
    }
    dsl.execute("VACUUM ANALYZE");
  }

  /** ~30% of printings owned with quantity 1-4. */
  private void createSyntheticCollection() {
    var random = new Random(20020530);
    var ids = dsl.select(PRINTING.ID).from(PRINTING).orderBy(PRINTING.ID).fetch(PRINTING.ID);
    var insert =
        dsl.insertInto(
            COLLECTION_ENTRY,
            COLLECTION_ENTRY.USER_ID,
            COLLECTION_ENTRY.PRINTING_ID,
            COLLECTION_ENTRY.QUANTITY);
    boolean any = false;
    for (int id : ids) {
      if (random.nextInt(100) < 30) {
        insert = insert.values(user.getId(), id, 1 + random.nextInt(4));
        any = true;
      }
    }
    if (any) {
      insert.execute();
    }
  }

  private SearchFilter defaultFilter(boolean ownedOnly) {
    var api =
        new SearchFilterApi(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null);
    return api.toSearchFilter(
        user.getId(),
        ownedOnly,
        0,
        Math.min(appProperties.cardPage().defaultSize(), appProperties.cardPage().maxSize()));
  }

  private List<Case> cases() {
    var cardsDefault = defaultFilter(false);
    var collectionDefault = defaultFilter(true);
    var oldestFirstPageThree =
        SearchBuilder.search(user)
            .setPageable(
                PageRequest.of(
                    2, appProperties.cardPage().defaultSize(), Sort.by(SORT_RELEASE).ascending()))
            .build();
    return List.of(
        new Case("cards-default", () -> cardQueryService.search(cardsDefault)),
        new Case(
            "collection-default",
            () -> collectionService.getPrimaryCollection(user.getId(), collectionDefault)),
        new Case("cards-oldest-page-3", () -> cardQueryService.search(oldestFirstPageThree)));
  }

  @Test
  void benchmark() throws Exception {
    int warmup = Integer.getInteger("perf.warmup", 20);
    int iterations = Integer.getInteger("perf.iterations", 50);

    List<Result> results = new ArrayList<>();
    for (Case c : cases()) {
      for (int i = 0; i < warmup; i++) {
        c.call().get();
      }
      double[] times = new double[iterations];
      for (int i = 0; i < iterations; i++) {
        long start = System.nanoTime();
        c.call().get();
        times[i] = (System.nanoTime() - start) / 1e6;
      }
      capture.start();
      c.call().get();
      List<String> queries = capture.stop();
      List<String> plans = queries.stream().map(this::explain).toList();
      results.add(new Result(c, stats(times), queries, plans));
    }
    writeReport(results, readPrevious());
  }

  private String explain(String sql) {
    return dsl.connectionResult(
        conn -> {
          var sb = new StringBuilder();
          try (var st = conn.createStatement();
              var rs = st.executeQuery("EXPLAIN (ANALYZE, BUFFERS, SETTINGS) " + sql)) {
            while (rs.next()) {
              sb.append(rs.getString(1)).append('\n');
            }
          }
          return sb.toString();
        });
  }

  private static Stats stats(double[] times) {
    double[] sorted = times.clone();
    Arrays.sort(sorted);
    int n = sorted.length;
    return new Stats(
        sorted[0],
        sorted[n / 2],
        sorted[Math.min(n - 1, (int) Math.ceil(n * 0.95) - 1)],
        sorted[n - 1],
        Arrays.stream(sorted).average().orElse(0));
  }

  private Map<String, Double> readPrevious() throws Exception {
    Map<String, Double> previous = new LinkedHashMap<>();
    if (Files.isRegularFile(LATEST_CSV)) {
      for (String line : Files.readAllLines(LATEST_CSV)) {
        String[] parts = line.split(",");
        if (parts.length >= 3 && !parts[0].equals("case")) {
          previous.put(parts[0], Double.parseDouble(parts[1]));
        }
      }
    }
    return previous;
  }

  private static String delta(Double previous, double now) {
    if (previous == null || previous == 0) {
      return "n/a";
    }
    return "%.1f → %.1f ms (%+.0f%%)".formatted(previous, now, (now - previous) / previous * 100);
  }

  private String git(String... args) {
    try {
      var cmd = new ArrayList<>(List.of("git"));
      cmd.addAll(List.of(args));
      var p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
      String out = new String(p.getInputStream().readAllBytes()).trim();
      return p.waitFor() == 0 ? out : "";
    } catch (Exception _) {
      return "";
    }
  }

  private void writeReport(List<Result> results, Map<String, Double> previous) throws Exception {
    Files.createDirectories(RESULTS_DIR);
    String hash = git("rev-parse", "--short", "HEAD");
    String dirty = git("status", "--porcelain").isEmpty() ? "" : "-dirty";
    String rev = (hash.isEmpty() ? "nogit" : hash) + dirty;
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

    var md = new StringBuilder();
    md.append("# Query benchmark ").append(rev).append(" (").append(timestamp).append(")\n\n");
    md.append("- Postgres: ").append(dsl.fetchValue("select version()")).append('\n');
    md.append("- Rows: card=").append(dsl.fetchCount(CARD));
    md.append(", printing=").append(dsl.fetchCount(PRINTING));
    md.append(", collection_entry=").append(dsl.fetchCount(COLLECTION_ENTRY)).append('\n');
    md.append("- Collection: ").append(collectionSource).append('\n');
    md.append("- Iterations: ").append(Integer.getInteger("perf.iterations", 50));
    md.append(" (warmup ").append(Integer.getInteger("perf.warmup", 20)).append(")\n\n");

    md.append("| case | min | median | p95 | max | mean | median vs. previous run |\n");
    md.append("|---|---|---|---|---|---|---|\n");
    var csv = new StringBuilder("case,median,p95,mean\n");
    var console = new StringBuilder("\n===== Query benchmark ").append(rev).append(" =====\n");
    for (Result r : results) {
      Stats s = r.stats();
      String name = r.benchCase().name();
      String d = delta(previous.get(name), s.median());
      md.append(
          "| %s | %.1f | %.1f | %.1f | %.1f | %.1f | %s |%n"
              .formatted(name, s.min(), s.median(), s.p95(), s.max(), s.mean(), d));
      csv.append("%s,%.3f,%.3f,%.3f%n".formatted(name, s.median(), s.p95(), s.mean()));
      console.append(
          "%-20s median %.1f ms, p95 %.1f ms, mean %.1f ms | vs previous: %s%n"
              .formatted(name, s.median(), s.p95(), s.mean(), d));
    }
    md.append("\nAll times in ms.\n");

    for (Result r : results) {
      md.append("\n## ").append(r.benchCase().name()).append("\n");
      for (int i = 0; i < r.queries().size(); i++) {
        md.append("\n### Query ").append(i + 1).append("\n\n```sql\n");
        md.append(r.queries().get(i)).append("\n```\n\n```\n");
        md.append(r.plans().get(i)).append("```\n");
      }
    }

    Path report = RESULTS_DIR.resolve(timestamp + "-" + rev + ".md");
    Files.writeString(report, md.toString());
    Files.writeString(LATEST_CSV, csv.toString());
    console.append("Report: ").append(report.toAbsolutePath()).append('\n');
    System.out.println(console);
  }
}
