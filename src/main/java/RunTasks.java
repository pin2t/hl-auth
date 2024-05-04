import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import java.io.*;
import java.net.*;
import java.net.http.*;

import static java.lang.System.*;

public class RunTasks {
    final JSONParser parser = new JSONParser();
    final String address = "localhost:8080";

    public static void main(String[] args) throws FileNotFoundException {
        new RunTasks().run();
    }

    void run() throws FileNotFoundException {
        var client = HttpClient.newHttpClient();
        new BufferedReader(new InputStreamReader(new FileInputStream("data/tasks.jsonl"))).lines().forEach(line -> {
            try {
                var json = (JSONObject)parser.parse(line);
                var id = (Long)json.get("id");
                var uri = URI.create("http://" + address + (String)json.get("path"));
                var request = HttpRequest.newBuilder().uri(uri);
                var headers = (JSONObject)json.get("headers");
                assert headers != null;
                headers.forEach((key, value) -> request.header((String)key, (String)value));
                var method = (String)json.get("method");
                if ("GET".equals(method)) {
                    request.GET();
                } else {
                    request.method(method, HttpRequest.BodyPublishers.ofString((String)json.getOrDefault("body", "")));
                }
                var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
                var checks = (JSONObject)json.get("checks");
                var code = (Long)checks.get("code");
                if (!code.equals((long)response.statusCode())) {
                    throw new RuntimeException("invalid status code " + response.statusCode() + " excpected " + code);
                }
            } catch (ParseException | IOException | InterruptedException e) {
                throw new RuntimeException("error processing " + line, e);
            }
        });
    }
}
