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
import static java.lang.Thread.sleep;

public class Tasks {
    final boolean stopOnError, sequential;
    final Set<Integer> done;
    final AtomicLong errors;

    public static void main(String[] args) {
        new Tasks(args).run();
    }

    Tasks(String[] args) {
        this.sequential = Arrays.asList(args).contains("-seq");
        this.stopOnError = Arrays.asList(args).contains("-stop");
        this.done = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.errors = new AtomicLong();
    }

    @SuppressWarnings("BusyWait")
    void run() {
        try {
            final long started = System.nanoTime();
            var tasks = new ConcurrentLinkedQueue<Task>();
            var input = new BufferedReader(new FileReader("data/tasks.jsonl"));
            var threads = Executors.newVirtualThreadPerTaskExecutor();
            var batch = new ArrayList<String>();
            var total = new int[]{0};
            var drain = (Runnable) () -> {
                var _batch = new ArrayList<>(batch);
                total[0] += batch.size();
                batch.clear();
                threads.submit(() -> {
                    for (var ll : _batch) {
                        try {
                            tasks.offer(new Task(ll, done));
                        } catch (ParseException e) {
                            err.println("error parsing " + ll + ": " + e.getMessage());
                        }
                    }
                });
            };
            input.lines().forEach(l -> {
                batch.add(l);
                if (batch.size() >= 1000) {
                    drain.run();
                }
            });
            drain.run();
            while (tasks.size() < total[0]) { sleep(200); }
            for (int i = 0; i < (sequential ? 1 : 100); i++) {
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
                                        sleep(50);
                                    }
                                    t.run(connection);
                                    if (!t.succeeded()) {
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
            var prevTime = System.nanoTime();
            var prevDone = done.size();
            while (done.size() < total[0] && (!stopOnError || errors.get() == 0)) {
                var rps = (done.size() - prevDone) / max((System.nanoTime() - prevTime) / 1000000000, 1);
                out.printf("\r%d/%d\t%d rps %d errors\t\t\t", done.size(), total[0], rps, errors.get());
                prevTime = System.nanoTime();
                prevDone = done.size();
                sleep(200);
            }
            out.printf("\r%d/%d\t%d rps %d errors\t\t\t\n", done.size(), total[0],
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
        final String checkBody;
        final Map<String, String> checkHeaders = new LinkedHashMap<>();
        final String request;
        final String body;
        final Map<String, String> headers = new LinkedHashMap<>();
        final Map<String, String> rsHeaders = new LinkedHashMap<>();
        String statusLine, rsBody;

        Task(String t, Set<Integer> done) throws ParseException {
            this.done = done;
            var json = (JSONObject) new JSONParser().parse(t);
            this.id = ((Long)json.get("id")).intValue();
            if (json.containsKey("dependsOn")) {
                var arr = (JSONArray)json.get("dependsOn");
                for (Object v : arr) {
                    this.depends.add(((Long)v).intValue());
                }
            }
            var headers = (JSONObject) json.get("headers");
            assert headers != null;
            headers.forEach((key, value) -> this.headers.put((String) key, (String) value));
            this.request = json.get("method") + " " + (String)json.get("path") + " HTTP/1.1\r\n";
            if (!"GET".equals(json.get("method")))
                this.body = (String) json.getOrDefault("body", "");
            else
                this.body = "";
            var checks = (JSONObject) json.get("checks");
            this.checkCode = (Long) checks.get("code");
            if (checks.containsKey("headers")) {
                headers = (JSONObject) checks.get("headers");
                ((Map<String, Object>) headers).forEach((key, value) -> this.checkHeaders.put(key, (String) value));
            }
            var checkBody = "";
            if (checks.containsKey("jsonBody")) {
                var jb = checks.get("jsonBody");
                if (jb instanceof String) {
                    checkBody = "\"" + jb + "\"";
                } else if (jb instanceof JSONObject o) {
                    var fields = "\"login\":\"" + o.get("login").toString() + "\"," +
                        "\"name\":\"" + o.get("name").toString() + "\"," +
                        "\"phone\":\"" + o.get("phone").toString() + "\"," +
                        "\"country\":\"" + o.get("country").toString() + "\"";
                    if (o.containsKey("is_admin")) {
                        fields = fields + ",\"is_admin\":true";
                    }
                    checkBody = "{" + fields + "}";
                }
            }
            this.checkBody = checkBody;
        }

        boolean ready() {
            return depends.isEmpty() || done.containsAll(depends);
        }

        void run(Socket connection) throws IOException {
            write(connection);
            read(connection);
            if ("close".equals(rsHeaders.get("Connection"))) {
                connection.close();
            }
        }

        void write(Socket connection) throws IOException {
            var writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream())));
            try {
                writer.print(request);
                for (var h : headers.entrySet()) {
                    writer.print(h.getKey() + ": " + h.getValue() + "\r\n");
                }
                writer.print("Host: localhost\r\n");
                writer.print("Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n");
                writer.print("Connection: keep-alive\r\n");
                if (!body.isBlank()) {
                    writer.print("Content-Type: application/json\r\n");
                }
                writer.print("\r\n");
                if (!body.isBlank()) {
                    writer.print(body);
                }
            } finally {
                writer.flush();
            }
        }

        void read(Socket connection) throws IOException {
            var rs = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            this.statusLine = rs.readLine();
            String line;
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
            this.rsBody = rsBody.toString();
        }

        boolean succeeded() {
            if (!statusLine.contains(Long.toString(checkCode))) {
                return error("invalid status code, expected " + checkCode + " got " + statusLine);
            }
            for (var h : checkHeaders.entrySet()) {
                if (!rsHeaders.containsKey(h.getKey())) {
                    return error("no header " + h.getKey() + " in response");
                }
                var val = rsHeaders.get(h.getKey());
                if (!val.equals(h.getValue())) {
                    return error("invalid header " + h.getKey() + " value, expected \"" + h.getValue() + "\" got \"" + val + "\"");
                }
            }
            if (!checkBody.isEmpty() && !checkBody.equals(rsBody)) {
                return error("invalid body, expected \"" + checkBody + "\" got \"" + rsBody + "\"");
            }
            return true;
        }

        boolean error(String message) {
            err.println("\r" + id + " ERROR: " + message);
            return false;
        }
    }
}
