import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import java.io.*;
import java.net.*;
import java.net.http.*;

import static java.lang.System.err;
import static java.lang.System.out;

public class RunTasks {
    final JSONParser parser = new JSONParser();
    final String address = "localhost:8080";

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        new RunTasks().run();
    }

    void run() throws FileNotFoundException, InterruptedException {
//        var pool = Executors.newFixedThreadPool(500);
        var client = HttpClient.newHttpClient();
        new BufferedReader(new InputStreamReader(new FileInputStream("data/tasks.jsonl"))).lines().forEach(line -> {
            try {
                var json = (JSONObject) parser.parse(line);
//                pool.submit(() -> {
                try {
                    var id = (Long) json.get("id");
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
                        err.println("error processing " + line);
                        throw new RuntimeException("invalid status code " + response.statusCode() + " excpected " + code);
                    }
                    out.println("" + id + ": " + response.statusCode() + " " + uri);
                } catch (IOException | InterruptedException e) {
                    err.println("ERROR: error processing " + line);
                    e.printStackTrace();
                    throw new RuntimeException("error processing " + line, e);
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
//                });
        });
//        pool.shutdown();
//        pool.awaitTermination(10, TimeUnit.MINUTES);
    }
}
