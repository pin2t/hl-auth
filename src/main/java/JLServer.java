import net.freeutils.httpserver.*;
import org.json.simple.parser.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

class JLServer {
    static final Logger log = LoggerFactory.getLogger(JLServer.class);
    static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
    static final String X_API_KEY = "X-API-Key";
    static final String CONTENT_TYPE = "Content-Type";
    static final String CONTENT_TYPE1 = "application/json";
    static final String NONCE = "\"nonce\":";
    final Users users;
    final Set<String> blacklisted = Collections.newSetFromMap(new ConcurrentHashMap<>());
    final TreeRanges blacklistedIPs = new TreeRanges();
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

    int auth(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        try {
            var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                rs.send(403, "");
                return 0;
            }
            var json = new JSONString(new Scanner(rq.getBody()).useDelimiter("\\A").next());
            var login = json.field(User.LOGIN_PREF);
            if (blacklisted.contains(login)) {
                rs.send(403, "");
                return 0;
            }
            var user = users.get(login);
            if (user.isEmpty()) {
                rs.send(403, "");
                return 0;
            }
            if (!user.get().isValid(json.field(User.PASSWORD_PREF))) {
                rs.send(403, "");
                return 0;
            }
            var country = countries.get(ip);
            if (country != user.get().country()) {
                rs.send(403, "");
                return 0;
            }
            rs.getHeaders().add(CONTENT_TYPE, CONTENT_TYPE1);
            rs.send(200, new JWT(login, json.field(NONCE)).toJSON());
        } catch (Exception e) {
            rs.send(400, e.getMessage());
        }
        return 0;
    }

    int getUser(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        try {
            byUser(rq, rs, user -> {
                rs.getHeaders().add(CONTENT_TYPE, CONTENT_TYPE1);
                rs.send(200, user.toJSON());
            });
        } catch (Exception e) {
            log.error("unhandled exception", e);
        }
        return 0;
    }

    int createUser(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
        if (blacklistedIPs.contains(ip)) {
            rs.send(403, "");
            return 0;
        }
        var json = new JSONString(new Scanner(rq.getBody()).useDelimiter("\\A").next());
        var login = json.field(User.LOGIN_PREF);
        if (users.get(login).isPresent()) {
            rs.send(409, "");
            return 0;
        }
        var removed = json.removeBoolean(User.IS_ADMIN_PREF);
        users.put(login, new User(removed.first().toJSON()));
        rs.send(201, "");
        return 0;
    }

    int updateUser(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        byUser(rq, rs, user -> {
            try {
                var json = new Scanner(rq.getBody()).useDelimiter("\\A").next();
                users.put(user.login(), new User(user, json));
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
            var range = new IPRange(ip + "/" + mask);
            if (blacklistedIPs.contains(range)) {
                rs.send(409, "");
                return;
            }
            blacklistedIPs.add(range);
            rs.send(201, "");
        });
        return 0;
    }

    int unblockSubnet(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        byAdmin(rq, rs, user -> {
            var items = rq.getPath().split("/");
            var ip = items[3];
            var mask = items[4];
            var range = new IPRange(ip + "/" + mask);
            if (!blacklistedIPs.contains(range)) {
                rs.send(404, "");
                return;
            }
            blacklistedIPs.remove(range);
            rs.send(204, "");
        });
        return 0;
    }

    int blockUser(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        byAdmin(rq, rs, u -> {
            var items = rq.getPath().split("/");
            var login = items[3];
            var user = users.get(login);
            if (user.isEmpty()) {
                rs.send(404, "");
                return;
            }
            if (blacklistedIPs.contains(IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR)))) {
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
            if (user.isEmpty()) {
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
        var token = rq.getHeaders().get((X_API_KEY));
        if (token == null) {
            rs.send(403, "");
            return;
        }
        JWT jwt = new JWT(token);
        if (!jwt.isValid()) {
            rs.send(403, "");
            return;
        }
        var json = new JSONString((jwt.payload()));
        var login = (String)json.field(User.LOGIN_PREF);
        if (blacklisted.contains(login)) {
            rs.send(403, "");
            return;
        }
        var admin = users.get(login);
        if (admin.isEmpty()) {
            rs.send(403, "");
            return;
        }
        if (!admin.get().isAdmin()) {
            rs.send(403, "");
            return;
        }
        var country = countries.get(ip);
        if (country != admin.get().country()) {
            rs.send(403, "");
            return;
        }
        handler.handle(admin.get());
    }

    void byUser(HTTPServer.Request rq, HTTPServer.Response rs, UserHandler handler) throws IOException {
        var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
        if (blacklistedIPs.contains(ip)) {
            rs.send(403, "");
            return;
        }
        var token = rq.getHeaders().get((X_API_KEY));
        if (token == null) {
            rs.send(403, "");
            return;
        }
        JWT jwt = new JWT(token);
        if (!jwt.isValid()) {
            rs.send(403, "");
            return;
        }
        var json = new JSONString((jwt.payload()));
        var login = (String)json.field(User.LOGIN_PREF);
        if (blacklisted.contains(login)) {
            rs.send(403, "");
            return;
        }
        var user = users.get(login);
        if (user.isEmpty()) {
            rs.send(403, "");
            return;
        }
        var country = countries.get(ip);
        if (country != user.get().country()) {
            rs.send(403, "");
            return;
        }
        handler.handle(user.get());
    }

    interface UserHandler {
        void handle(User user) throws IOException;
    }
}
