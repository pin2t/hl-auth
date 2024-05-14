import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static java.lang.Math.max;
import static java.lang.System.err;
import static java.lang.System.out;

public class SimpleTasks {
    final boolean stopOnError, sequential;
    final Set<Integer> done = Collections.newSetFromMap(new ConcurrentHashMap<>());
    final AtomicLong errors = new AtomicLong();

    public static void main(String[] args) {
        new SimpleTasks(args).run();
    }

    SimpleTasks(String[] args) {
        this.sequential = Arrays.asList(args).contains("-seq");
        this.stopOnError = Arrays.asList(args).contains("-stop");
    }

    void run() {
        try {
            final long started = System.nanoTime();
            var tasks = new ConcurrentLinkedQueue<Task>();
            var input = new BufferedReader(new FileReader("data/tasks.jsonl"));
            input.lines().forEach(l -> {
                try {
                    tasks.offer(new Task(l, done));
                } catch (ParseException e) {
                    err.println("error parsing " + l + ": " + e.getMessage());
                }
            });
            var total = tasks.size();
            var threads = Executors.newVirtualThreadPerTaskExecutor();
            for (int i = 0; i < (sequential ? 1 : 200); i++) {
                threads.submit(() -> {
                    try {
                        Socket connection = new Socket(InetAddress.getByName("localhost"), 8080);
                        try {
                            Task t;
                            while ((t = tasks.poll()) != null) {
                                if (connection.isClosed()) {
                                    connection = new Socket(InetAddress.getByName("localhost"), 8080);
                                }
                                try {
                                    while (!t.ready()) {
                                        Thread.sleep(50);
                                    }
                                    if (!t.run(connection)) {
                                        errors.addAndGet(1);
                                        if (stopOnError) {
                                            break;
                                        }
                                    }
                                } catch (SocketException e) {
                                    err.println("socket error " + e.getMessage());
                                    connection.close();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                } finally {
                                    done.add(t.id);
                                }
                            }
                        } finally {
                            try {
                                connection.close();
                            } catch (IOException e) {
                                err.println("io error " + e.getMessage());
                            }
                        }
                    } catch (IOException e) {
                        err.println("io error " + e.getMessage());
                    }
                });
            }
            while (done.size() < total && (!stopOnError || errors.get() == 0)) {
                out.printf("\r%d/%d\t%d rps %d errors\t\t\t\r", done.size(), total,
                    done.size() / max((System.nanoTime() - started) / 1000000000, 1), errors.get()
                );
                Thread.sleep(200);
            }
            out.printf("\r%d/%d\t%d rps %d errors\t\t\t\r", done.size(), total,
                done.size() / max((System.nanoTime() - started) / 1000000000, 1), errors.get()
            );
        } catch (Exception e) {
            err.println("unhandled exception" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    static class Task {
        final int id;
        final List<Integer> depends = new ArrayList<>();
        final Set<Integer> done;
        final long checkCode;
        final Optional<String> checkBody;
        final Map<String, String> checkHeaders = new HashMap<>();
        final String request;
        final String body;
        final Map<String, String> headers = new LinkedHashMap<>();

        Task(String t, Set<Integer> done) throws ParseException {
            this.done = done;
            var json = (JSONObject) new JSONParser().parse(t);
            this.id = ((Long)json.get("id")).intValue();
            if (json.containsKey("dependsOn")) {
                var arr = (JSONArray)json.get("dependsOn");
                arr.forEach(v -> this.depends.add(((Long)v).intValue()));
            }
            var headers = (JSONObject) json.get("headers");
            assert headers != null;
            headers.forEach((key, value) -> this.headers.put((String) key, (String) value));
            this.request = (String) json.get("method") + " " + (String)json.get("path") + " HTTP/1.1\r\n";
            if (!"GET".equals((String) json.get("method"))) {
                this.body = (String) json.getOrDefault("body", "");
            } else {
                this.body = "";
            }
            var checks = (JSONObject) json.get("checks");
            this.checkCode = (Long) checks.get("code");
            if (checks.containsKey("headers")) {
                headers = (JSONObject) checks.get("headers");
                for (Map.Entry<String, Object> h : ((Map<String, Object>) headers).entrySet()) {
                    this.checkHeaders.put(h.getKey(), (String)h.getValue());
                }
            }
            if (checks.containsKey("jsonBody")) {
                var jb = checks.get("jsonBody");
                if (jb instanceof String)          this.checkBody = Optional.of("\"" + (String)jb + "\"");
                else if (jb instanceof JSONObject) this.checkBody = Optional.of(((JSONObject)jb).toJSONString());
                else this.checkBody = Optional.empty();
            } else {
                this.checkBody = Optional.empty();
            }
        }

        boolean ready() {
            return depends.isEmpty() || done.containsAll(depends);
        }

        boolean run(Socket connection) throws IOException {
            var rq = new PrintWriter(connection.getOutputStream());
            try {
                rq.print(request);
                headers.forEach((k, v) -> rq.print(k + ": " + v + "\r\n"));
                rq.print("Host: localhost\r\n");
                rq.print("Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n");
                rq.print("Connection: keep-alive\r\n");
                if (!body.isBlank()) {
                    rq.print("Content-Type: application/json\r\n");
                }
                rq.print("\r\n");
                if (!body.isBlank()) {
                    rq.print(body);
                }
            } finally {
                rq.flush();
            }
            var rs = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            var rsHeaders = new LinkedHashMap<String, String>();
            String statusLine = rs.readLine(), line;
            do {
                line = rs.readLine();
                if (line != null && line.contains(":")) {
                    var key = line.substring(0, line.indexOf(':')).trim();
                    var value = line.substring(line.indexOf(':') + 1).trim();
                    rsHeaders.put(key, value);
                }
            } while (line != null && !line.isBlank());
            var len = Integer.parseInt(rsHeaders.getOrDefault("Content-Length", "0"));
            var rsBody = new StringBuilder();
            if (len > 0) {
                var readLen = 0;
                char[] buf = new char[512];
                do {
                    int n = rs.read(buf);
                    if (n > 0) {
                        rsBody.append(buf, 0, n);
                        readLen += new String(buf, 0, n).getBytes(StandardCharsets.UTF_8).length;
                    }
                } while (readLen < len);
            }
            if ("close".equals(rsHeaders.get("Connection"))) {
                connection.close();
            }
            if (!statusLine.contains(Long.toString(checkCode))) {
                return error("invalid status code, expected " + checkCode + " got " + statusLine);
            }
            return true;
        }

        boolean error(String message) {
            err.println("\r" + id + " ERROR: " + message);
            return false;
        }
    }
}
