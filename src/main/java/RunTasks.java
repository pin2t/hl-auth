import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static java.lang.System.err;
import static java.lang.System.out;

public class RunTasks {
    final String address = "localhost:8080";
    final ExecutorService pool = Executors.newFixedThreadPool(200);
    final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    //final HttpClient client = HttpClient.newHttpClient();
    final AtomicLong errors = new AtomicLong();
    final AtomicLong total = new AtomicLong();
    final Set<Long> done = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void main(String[] args) throws IOException, InterruptedException {
        new RunTasks().run();
    }

    void run() throws IOException, InterruptedException {
        final long started = System.nanoTime();
        try (var input = new BufferedReader(new FileReader("data/tasks.jsonl"))) {
            String line;
            while ((line = input.readLine()) != null) {
                String finalLine = line;
                pool.submit(() -> send(finalLine));
                total.addAndGet(1);
            }
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.MINUTES);
        err.println(String.format("%d requests sent in %.2f s, %d errors, %d requests/sec",
            total.get(),
            (System.nanoTime() - started) / 1000000000., errors.get(),
            total.get() / ((System.nanoTime() - started) / 1000000000))
        );
    }

    void send(String line) {
        try {
            var json = (JSONObject) new JSONParser().parse(line);
            var id = (Long) json.get("id");
            try {
                var dependencies = (JSONArray) json.get("dependsOn");
                if (dependencies != null && !dependencies.isEmpty()) {
                    while (!dependencies.stream().allMatch(item -> done.contains((Long)item))) {
                        Thread.sleep(100);
                    }
                }
                var uri = URI.create("http://" + address + (String) json.get("path"));
                var request = HttpRequest.newBuilder().uri(uri);
                var headers = (JSONObject) json.get("headers");
                assert headers != null;
                headers.forEach((key, value) -> request.header((String) key, (String) value));
                var method = (String) json.get("method");
                if ("GET".equals(method)) {
                    request.GET();
                } else {
                    request.method(method, HttpRequest.BodyPublishers.ofString((String) json.getOrDefault("body", "")));
                }
                var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
                var checks = (JSONObject) json.get("checks");
                var code = (Long) checks.get("code");
                if (!code.equals((long) response.statusCode())) {
                    throw new RuntimeException("invalid status code " + response.statusCode() + " expected " + code);
                }
                var jsonBody = checks.get("jsonBody");
                if (jsonBody instanceof String && !((String) jsonBody).equals(response.body())) {
                    throw new RuntimeException("invalid body \"" + response.body() + "\" expected \"" + jsonBody + "\"");
                }
                if (jsonBody instanceof JSONObject) {
                    var expected = (JSONObject) jsonBody;
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
                }
                var checkHeaders = (JSONObject) checks.get("headers");
                if (checkHeaders != null) {
                    for (Map.Entry<String, Object> h : ((Map<String, Object>) checkHeaders).entrySet()) {
                        if (!response.headers().allValues(h.getKey()).contains(h.getValue())) {
                            throw new RuntimeException("header not found expected \"" + h.getKey() + "\": \"" + h.getValue() + "\"");
                        }
                    }
                }
                out.println("" + id + ": " + response.statusCode() + " " + method + " " + uri);
            } catch (IOException | InterruptedException | RuntimeException | ParseException e) {
                err.println("" + id + ":" + e.getMessage());
                errors.addAndGet(1);
            }
            done.add(id);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
