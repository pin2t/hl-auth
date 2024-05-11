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
    final AtomicLong errors = new AtomicLong();
    final Set<Integer> done = Collections.newSetFromMap(new ConcurrentHashMap<>());
    final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    final boolean sequential, stopError;
    final LinkedBlockingDeque<Task> postponed = new LinkedBlockingDeque<>();

    public static void main(String[] args) throws InterruptedException {
        new Tasks(args).run();
    }

    Tasks(String[] args) {
        this.sequential = Arrays.asList(args).contains("-seq");
        this.stopError = Arrays.asList(args).contains("-stop");
    }

    void run() throws InterruptedException {
        final long started = System.nanoTime();
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        var total = 1;
        try (var input = new BufferedReader(new FileReader("data/tasks.jsonl"))) {
            var lines = input.lines().collect(Collectors.toList());
            total = lines.size();
            for (var line : lines) {
                if (stopError && errors.get() > 0) {
                    break;
                }
                var task = new Task(line, client, done);
                if (sequential) {
                    try {
                        task.run().get();
                    } catch (ExecutionException e) {
                        errors.addAndGet(1);
                    }
                    done.add(task.id);
                } else {
                    if (task.ready()) {
                        futures.add(task.run().handle((ok, ex) -> {
                            done.add(task.id);
                            if (ex != null || !ok) errors.addAndGet(1);
                            return ok;
                        }));
                    } else {
                        postponed.offer(task);
                    }
                }
                out.printf("\r%d/%d\t%d rps %d errors\t\t\t", done.size(), total,
                    done.size() / max((System.nanoTime() - started) / 1000000000, 1), errors.get()
                );
            }
        } catch (FileNotFoundException e) {
            err.println("file not found data/tasks.jsonl");
            e.printStackTrace();
        } catch (IOException e) {
            err.println("read error");
            e.printStackTrace();
        }
        for (var f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
            }
            out.printf("\r%d/%d\t%d rps %d errors\t\t\t", done.size(), total,
                    done.size() / max((System.nanoTime() - started) / 1000000000, 1), errors.get()
            );
        }
        out.printf("\r%d/%d\t%d rps %d errors\t\t\t", done.size(), total,
                done.size() / max((System.nanoTime() - started) / 1000000000, 1), errors.get()
        );
        while (!postponed.isEmpty()) {
            var task = postponed.poll();
            if (task.ready()) {
                try {
                    task.run().handle((ok, ex) -> {
                        done.add(task.id);
                        if (ex != null || !ok) errors.addAndGet(1);
                        return ok;
                    }).get();
                } catch (ExecutionException e) {
                }
            } else {
                postponed.offer(task);
            }
            out.printf("\r%d/%d\t%d rps %d errors\t\t\t", done.size(), total,
                done.size() / max((System.nanoTime() - started) / 1000000000, 1),
                errors.get()
            );
        }
        out.printf("%n\r%d requests sent in %.2f s, %d errors, %d rps%n",
            done.size(), (System.nanoTime() - started) / 1000000000., errors.get(),
            done.size() / max((System.nanoTime() - started) / 1000000000, 1));
        client.shutdown();
    }

    static class Task {
        final int id;
        final List<Integer> depends;
        final HttpClient http;
        final Set<Integer> done;
        final HttpRequest request;
        final long checkCode;
        final Optional<String> checkBody;
        final Map<String, String> checkHeaders;

        Task(String t, HttpClient client, Set<Integer> done) {
            this.http = client;
            this.done = done;
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

        boolean ready() {
            return depends.isEmpty() || done.containsAll(depends);
        }

        CompletableFuture<Boolean> run() {
            return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (checkCode != response.statusCode()) {
                        return error("invalid status code " + response.statusCode() + " expected " + checkCode);
                    }
                    if (!checkBody.isEmpty()) {
                        if (checkBody.get().startsWith("{")) {
                            try {
                                var expected = (JSONObject) new JSONParser().parse(checkBody.get());
                                var actual = (JSONObject) new JSONParser().parse(response.body());
                                for (Map.Entry<String, Object> h : ((Map<String, Object>) expected).entrySet()) {
                                    Object exp = h.getValue();
                                    Object act = actual.get(h.getKey());
                                    if (act == null) {
                                        return error("invalid response body, key \"" + h.getKey() + "\" is absent in response " + response.body());
                                    }
                                    if (exp instanceof String && act instanceof String && !((String) exp).equals((String) act)) {
                                        return error("invalid response body, key \"" + h.getKey() + "\": \"" + exp + "\" does not match \"" + act + "\"");
                                    }
                                    if (exp instanceof Boolean && act instanceof Boolean && !((Boolean) exp).equals((Boolean) act)) {
                                        return error("invalid response body, key \"" + h.getKey() + "\": \"" + exp + "\" does not match \"" + act + "\"");
                                    }
                                }
                            } catch (ParseException e) {
                                return error(e.getMessage());
                            }
                        } else if (!checkBody.get().equals(response.body())) {
                            return error("invalid body \"" + response.body() + "\" expected \"" + checkBody.get() + "\"");
                        }
                    }
                    return true;
                });
        }

        boolean error(String message) {
            err.println("\r" + id + " ERROR: " + message);
            return false;
        }
    }
}
