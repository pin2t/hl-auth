import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;

import static java.lang.System.err;
import static java.lang.System.out;

public class RunTasks {
    final JSONParser parser = new JSONParser();
    final String address = "localhost:8080";

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        new RunTasks().run();
    }

    // {"id":1,"method":"POST","location":"/auth","path":"/auth","headers":{"X-FORWARDED-FOR":"203.97.2.85"},"body":"{\"login\":\"TAO8NF\",\"password\":\"hBlGGJx\",\"nonce\":\"6omD5g9nMmco4SNrsuIp8Y\"}",
    // "checks":{"code":200,"headers":{"Content-Type":"application/json"},
    // "jsonBody":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJsb2dpbiI6IlRBTzhORiIsIm5vbmNlIjoiNm9tRDVnOW5NbWNvNFNOcnN1SXA4WSJ9.VoBvFp5j79XmgkwEsotp-3N9ytWoIuekDIRN8zELUMw"}}
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
                        throw new RuntimeException("invalid status code " + response.statusCode() + " expected " + code);
                    }
                    var jsonBody = checks.get("jsonBody");
                    if (jsonBody instanceof String && !((String) jsonBody).equals(response.body())) {
                        throw new RuntimeException("invalid body \"" + response.body() + "\" expected \"" + jsonBody + "\"");
                    }
                    if (jsonBody instanceof JSONObject) {
                        var expected = (JSONObject) jsonBody;
                        var actual = (JSONObject) parser.parse(response.body());
                        for (Map.Entry<String, Object> h : ((Map<String, Object>)expected).entrySet()) {
                            Object exp = h.getValue();
                            Object act = actual.get(h.getKey());
                            if (act == null) {
                                throw new RuntimeException("invalid response body, key \"" + h.getKey() + "\" is absent in response " + response.body());
                            }
                            if (exp instanceof String && act instanceof String && !((String)exp).equals((String)act)) {
                                throw new RuntimeException("invalid response body, key \"" + h.getKey() + "\": \"" + exp + "\" does not match \"" + act + "\"");
                            }
                            if (exp instanceof Boolean && act instanceof Boolean && !((Boolean)exp).equals((Boolean)act)) {
                                throw new RuntimeException("invalid response body, key \"" + h.getKey() + "\": \"" + exp + "\" does not match \"" + act + "\"");
                            }
                        }
                    }
                    var checkHeaders = (JSONObject)checks.get("headers");
                    if (checkHeaders != null) {
                        for (Map.Entry<String, Object> h : ((Map<String, Object>)checkHeaders).entrySet()) {
                            if (!response.headers().allValues(h.getKey()).contains(h.getValue())) {
                                throw new RuntimeException("header not found expected \"" + h.getKey() + "\": \"" + h.getValue() + "\"");
                            }
                        }
                    }
                    out.println("" + id + ": " + response.statusCode() + " " + method + " " + uri);
                } catch (IOException | InterruptedException | RuntimeException e) {
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
