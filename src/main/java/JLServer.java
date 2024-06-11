import countries.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class JLServer {
    static final Logger log = LoggerFactory.getLogger(JLServer.class);
    static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
    static final String X_API_KEY = "X-API-Key";
    final Users users;
    final Set<String> blacklisted = Collections.newSetFromMap(new ConcurrentHashMap<>());
    final IPRanges blacklistedIPs = new IPRanges();
    final TreeCountries countries;
    final ExecutorService pool;
    final HTTPServer server;

    JLServer(Users users, TreeCountries countries, ExecutorService pool) {
        this.users = users;
        this.countries = countries;
        this.pool = pool;
        this.server = new HTTPServer(8080);
    }

    void start() {
        try {
            server.setExecutor(pool);
            HTTPServer.VirtualHost host = server.getVirtualHost(null);
            host.addContext("/auth", this::auth, "POST");
            host.addContext("/user", this::getUser, "GET");
            host.addContext("/user", this::createUser, "PUT");
            host.addContext("/user", this::updateUser, "PATCH");
            host.addContext("/blacklist/subnet", this::blockSubnet, "PUT");
            host.addContext("/blacklist/subnet", this::unblockSubnet, "DELETE");
            host.addContext("/blacklist/user", this::blockUser, "PUT");
            host.addContext("/blacklist/user", this::unblockUser, "DELETE");
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void stop() {
        server.stop();
    }

    int auth(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        try {
            var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                rs.send(403, "");
                return 0;
            }
            var body = rq.getBody();
            var json = (JSONObject) new JSONParser().parse(new InputStreamReader(body));
            var login = (String) json.get(User.LOGIN);
            if (blacklisted.contains(login)) {
                rs.send(403, "");
                return 0;
            }
            var user = users.get(login);
            if (user == null) {
                rs.send(403, "");
                return 0;
            }
            var password = (String) json.get(User.PASSWORD);
            if (!user.password().equals(password)) {
                rs.send(403, "");
                return 0;
            }
            var country = countries.get(ip);
            if (country != user.country()) {
                rs.send(403, "");
                return 0;
            }
            rs.getHeaders().add("Content-Type", "application/json");
            rs.send(200, new JWT(login, (String) json.get("nonce")).toJSON());
        } catch (Exception e) {
            rs.send(400, e.getMessage());
        }
        return 0;
    }

    int getUser(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        byUser(rq, rs, user -> {
            rs.getHeaders().add("Content-Type", "application/json");
            rs.send(200, user.toJSON());
        });
        return 0;
    }

    int createUser(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        try {
            var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                rs.send(403, "");
                return 0;
            }
            var json = (JSONObject)new JSONParser().parse(new InputStreamReader(rq.getBody()));
            var login = (String)json.get(User.LOGIN);
            if (users.get(login) != null) {
                rs.send(409, "");
                return 0;
            }
            users.put(login, new User(json));
            rs.send(201, "");
        } catch (ParseException e) {
            rs.send(400, "");
        }
        return 0;
    }

    int updateUser(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        byUser(rq, rs, user -> {
            try {
                var json = (JSONObject)new JSONParser().parse(new InputStreamReader(rq.getBody()));
                if (user.json.containsKey("is_admin")) {
                    json.putIfAbsent("is_admin", user.isAdmin());
                }
                json.putIfAbsent("login", user.login());
                json.putIfAbsent("country", user.json.get("country"));
                json.putIfAbsent("password", user.password());
                json.putIfAbsent("name", user.name());
                json.putIfAbsent("phone", user.phone());
                users.put(user.login(), new User(json));
                rs.send(202, "");
            } catch (ParseException e) {
                rs.send(400, "");
            }
        });
        return 0;
    }

    int blockSubnet(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        byAdmin(rq, rs, user -> {
            var items = rq.getPath().split("/");
            var ip = items[3];
            var mask = items[4];
            if (blacklistedIPs.contains(ip, mask)) {
                rs.send(409, "");
                return;
            }
            blacklistedIPs.add(ip, mask);
            rs.send(201, "");
        });
        return 0;
    }

    int unblockSubnet(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        byAdmin(rq, rs, user -> {
            var items = rq.getPath().split("/");
            var ip = items[3];
            var mask = items[4];
            if (!blacklistedIPs.contains(ip, mask)) {
                rs.send(404, "");
                return;
            }
            blacklistedIPs.remove(ip, mask);
            rs.send(204, "");
        });
        return 0;
    }

    int blockUser(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        byAdmin(rq, rs, u -> {
            var items = rq.getPath().split("/");
            var login = items[3];
            var user = users.get(login);
            if (user == null) {
                rs.send(404, "");
                return;
            }
            var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                rs.send(409, "");
                return;
            }
            if (!blacklisted.add(login)) {
                rs.send(409, "");
                return;
            }
            rs.send(201, "");
        });
        return 0;
    }

    int unblockUser(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        byAdmin(rq, rs, u -> {
            var items = rq.getPath().split("/");
            var login = items[3];
            var user = users.get(login);
            if (user == null) {
                rs.send(404, "");
                return;
            }
            if (!blacklisted.remove(login)) {
                rs.send(404, "");
                return;
            }
            rs.send(204, "");
        });
        return 0;
    }

    void byAdmin(HTTPServer.Request rq, HTTPServer.Response rs, UserHandler handler) throws IOException {
        var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
        if (blacklistedIPs.contains(ip)) {
            rs.send(403, "");
            return;
        }
        JWT jwt = new JWT(rq.getHeaders().get((X_API_KEY)));
        if (!jwt.isValid()) {
            rs.send(403, "");
            return;
        }
        try {
            var json = (JSONObject) new JSONParser().parse(jwt.payload());
            var login = (String)json.get(User.LOGIN);
            if (blacklisted.contains(login)) {
                rs.send(403, "");
                return;
            }
            var admin = users.get(login);
            if (admin == null) {
                rs.send(403, "");
                return;
            }
            if (!admin.isAdmin()) {
                rs.send(403, "");
                return;
            }
            var country = countries.get(ip);
            if (country != admin.country()) {
                rs.send(403, "");
                return;
            }
            handler.handle(admin);
        } catch (ParseException e) {
            rs.send(400, e.getMessage());
        }
    }

    void byUser(HTTPServer.Request rq, HTTPServer.Response rs, UserHandler handler) throws IOException {
        var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
        if (blacklistedIPs.contains(ip)) {
            rs.send(403, "");
            return;
        }
        JWT jwt = new JWT(rq.getHeaders().get((X_API_KEY)));
        if (!jwt.isValid()) {
            rs.send(403, "");
            return;
        }
        try {
            var json = (JSONObject) new JSONParser().parse(jwt.payload());
            var login = (String)json.get(User.LOGIN);
            if (blacklisted.contains(login)) {
                rs.send(403, "");
                return;
            }
            var user = users.get(login);
            if (user == null) {
                rs.send(403, "");
                return;
            }
            var country = countries.get(ip);
            if (country != user.country()) {
                rs.send(403, "");
                return;
            }
            handler.handle(user);
        } catch (ParseException e) {
            log.error("payload parse error: " + e.getMessage() + ", " + jwt.payload());
            rs.send(400, e.getMessage());
        }
    }

    interface UserHandler {
        void handle(User user) throws IOException;
    }
}
