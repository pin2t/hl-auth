import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import static java.lang.Math.max;
import static java.lang.System.err;
import static java.lang.System.out;

public class Tasks {
    final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
    final AtomicLong errors = new AtomicLong();
    final Set<Integer> done = Collections.newSetFromMap(new ConcurrentHashMap<>());
    final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    boolean sequential, stopError;

    public static void main(String[] args) throws IOException, InterruptedException {
        new Tasks(args).run();
    }

    Tasks(String[] args) {
        this.sequential = Arrays.asList(args).contains("-seq");
        this.stopError = Arrays.asList(args).contains("-stop");
    }

    void run() throws InterruptedException {
        final long started = System.nanoTime();
        try (var input = new BufferedReader(new FileReader("data/tasks.jsonl"))) {
            for (var line : input.lines().collect(Collectors.toList())) {
                if (stopError && errors.get() > 0) {
                    break;
                }
                var task = new Task(line);
                if (sequential) {
                    run(task);
                } else {
                    pool.submit(() -> {
                        if (stopError && errors.get() > 0) {
                            return;
                        }
                        try {
                            run(task);
                        } catch (InterruptedException e) {
                        }
                    });
                }
            }
        } catch (FileNotFoundException e) {
            err.println("file not found data/tasks.jsonl");
            e.printStackTrace();
        } catch (IOException e) {
            err.println("read error");
            e.printStackTrace();
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.MINUTES);
        out.printf("%d requests sent in %.2f s, %d errors, %d requests/sec%n",
            done.size(), (System.nanoTime() - started) / 1000000000., errors.get(),
            done.size() / max((System.nanoTime() - started) / 1000000000, 1));
        client.shutdown();
    }

    void run(Task task) throws InterruptedException {
        try {
            while (!done.containsAll(task.depends)) {
                Thread.sleep(100);
            }
            var result = task.run(client);
            if (!result) {
                errors.addAndGet(1);
            }
        } finally {
            done.add(task.id);
        }
    }

    static class Task {
        final int id;
        final List<Integer> depends;
        final HttpRequest request;
        final long checkCode;
        final Optional<String> checkBody;
        final Map<String, String> checkHeaders;

        Task(String t) {
            try {
                var json = (JSONObject) new JSONParser().parse(t);
                this.id = ((Long)json.get("id")).intValue();
                this.depends = new ArrayList<>();
                if (json.containsKey("dependsOn")) {
                    var arr = (JSONArray)json.get("dependsOn");
                    arr.forEach(v -> this.depends.add(((Long)v).intValue()));
                }
                var builder = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080" + (String) json.get("path")));
                var headers = (JSONObject) json.get("headers");
                assert headers != null;
                headers.forEach((key, value) -> builder.header((String) key, (String) value));
                var method = (String) json.get("method");
                if ("GET".equals(method)) {
                    builder.GET();
                } else {
                    builder.method(method, HttpRequest.BodyPublishers.ofString((String) json.getOrDefault("body", "")));
                }
                this.request = builder.build();
                var checks = (JSONObject) json.get("checks");
                this.checkCode = (Long) checks.get("code");
                this.checkHeaders = new HashMap<>();
                if (checks.containsKey("headers")) {
                    headers = (JSONObject) checks.get("headers");
                    for (Map.Entry<String, Object> h : ((Map<String, Object>) headers).entrySet()) {
                        this.checkHeaders.put(h.getKey(), (String)h.getValue());
                    }
                }
                if (checks.containsKey("jsonBody")) {
                    var jb = checks.get("jsonBody");
                    if (jb instanceof String)     this.checkBody = Optional.of("\"" + (String)jb + "\"");
                    else if (jb instanceof JSONObject) this.checkBody = Optional.of(((JSONObject)jb).toJSONString());
                    else this.checkBody = Optional.empty();
                } else {
                    this.checkBody = Optional.empty();
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        boolean run(HttpClient client) {
            try {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (checkCode != response.statusCode()) {
                    throw new RuntimeException("invalid status code " + response.statusCode() + " expected " + checkCode);
                }
                if (!checkBody.isEmpty()) {
                    if (checkBody.get().startsWith("{")) {
                        var expected = (JSONObject) new JSONParser().parse(checkBody.get());
                        var actual = (JSONObject) new JSONParser().parse(response.body());
                        for (Map.Entry<String, Object> h : ((Map<String, Object>) expected).entrySet()) {
                            Object exp = h.getValue();
                            Object act = actual.get(h.getKey());
                            if (act == null) {
                                throw new RuntimeException("invalid response body, key \"" + h.getKey() + "\" is absent in response " + response.body());
                            }
                            if (exp instanceof String && act instanceof String && !((String) exp).equals((String) act)) {
                                throw new RuntimeException("invalid response body, key \"" + h.getKey() + "\": \"" + exp + "\" does not match \"" + act + "\"");
                            }
                            if (exp instanceof Boolean && act instanceof Boolean && !((Boolean) exp).equals((Boolean) act)) {
                                throw new RuntimeException("invalid response body, key \"" + h.getKey() + "\": \"" + exp + "\" does not match \"" + act + "\"");
                            }
                        }
                    } else if (!checkBody.get().equals(response.body())) {
                        throw new RuntimeException("invalid body \"" + response.body() + "\" expected \"" + checkBody.get() + "\"");
                    }
                }
                out.println("" + id + " " + response.statusCode() + " " + request.method() + " " + request.uri());
                return true;
            } catch (IOException | InterruptedException | RuntimeException | ParseException e) {
                err.println("" + id + " ERROR: " + e.getMessage());
                return false;
            }
        }
    }
}
