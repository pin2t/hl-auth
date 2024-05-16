import net.freeutils.httpserver.*;
import net.freeutils.httpserver.HTTPServer.*;
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
    final Countries countries;
    final ExecutorService pool;

    JLServer(Users users, Countries countries, ExecutorService pool) {
        this.users = users;
        this.countries = countries;
        this.pool = pool;
    }

    void run() {
        HTTPServer server = new HTTPServer(8080);
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

    int auth(Request rq, Response rs) throws IOException {
        try {
            var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                log.error("blocked IP " + ip);
                rs.send(403, "");
                return 0;
            }
            var body = rq.getBody();
            var json = (JSONObject) new JSONParser().parse(new InputStreamReader(body));
            var login = (String) json.get(User.LOGIN);
            if (blacklisted.contains(login)) {
                log.error("blocked user " + login);
                rs.send(403, "");
                return 0;
            }
            var user = users.get(login);
            if (user == null) {
                log.error("user not found " + login);
                rs.send(403, "");
                return 0;
            }
            var password = (String) json.get(User.PASSWORD);
            if (!user.password().equals(password)) {
                log.error("user password invalid \"" + password + "\"");
                rs.send(403, "");
                return 0;
            }
            var country = countries.country(ip);
            if (country != user.country()) {
                log.error("IP country \"" + country.name + "\" does not match user country \"" + user.country().name + "\"");
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

    int getUser(Request rq, Response rs) throws IOException {
        byUser(rq, rs, user -> rs.send(200, user.toJSON()));
        return 0;
    }

    int createUser(Request rq, Response rs) throws IOException {
        try {
            var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                log.error("blocked IP " + ip);
                rs.send(403, "");
                return 0;
            }
            var json = (JSONObject)new JSONParser().parse(new InputStreamReader(rq.getBody()));
            var login = (String)json.get(User.LOGIN);
            if (users.get(login) != null) {
                log.error("user already exist " + login);
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

    int updateUser(Request rq, Response rs) throws IOException {
        byUser(rq, rs, user -> {
            rs.send(404, "not implemented");
        });
        return 0;
    }

    int blockSubnet(Request rq, Response rs) throws IOException {
        byAdmin(rq, rs, user -> {
            rs.send(404, "not implemented");
        });
        return 0;
    }

    int unblockSubnet(Request rq, Response rs) throws IOException {
        byAdmin(rq, rs, user -> {
            rs.send(404, "not implemented");
        });
        return 0;
    }

    int blockUser(Request rq, Response rs) throws IOException {
        byAdmin(rq, rs, user -> {
            rs.send(404, "not implemented");
        });
        return 0;
    }

    int unblockUser(Request rq, Response rs) throws IOException {
        byAdmin(rq, rs, user -> {
            rs.send(404, "not implemented");
        });
        return 0;
    }

    void byAdmin(Request rq, Response rs, UserHandler handler) throws IOException {
        var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
        if (blacklistedIPs.contains(ip)) {
            log.error("blocked IP " + ip);
            rs.send(403, "");
            return;
        }
        JWT jwt = new JWT(rq.getHeaders().get((X_API_KEY)));
        if (!jwt.isValid()) {
            log.error("JWT is not valid");
            rs.send(403, "");
            return;
        }
        try {
            var json = (JSONObject) new JSONParser().parse(jwt.payload());
            var login = (String)json.get(User.LOGIN);
            if (blacklisted.contains(login)) {
                log.error("user " + login + " blocked");
                rs.send(403, "");
                return;
            }
            var admin = users.get(login);
            if (admin == null) {
                log.error("user not found " + login);
                rs.send(403, "");
                return;
            }
            if (!admin.isAdmin()) {
                log.error("user not admin " + login);
                rs.send(403, "");
                return;
            }
            var country = countries.country(ip);
            if (country != admin.country()) {
                log.error("IP country \"" + country.name + "\" does not match user country \"" + admin.country().name + "\"");
                rs.send(403, "");
                return;
            }
            handler.handle(admin);
        } catch (ParseException e) {
            log.error("payload parse error: " + e.getMessage() + ", " + jwt.payload());
            rs.send(400, e.getMessage());
        }
    }

    void byUser(Request rq, Response rs, UserHandler handler) throws IOException {
        var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
        if (blacklistedIPs.contains(ip)) {
            log.error("blocked IP " + ip);
            rs.send(403, "");
            return;
        }
        JWT jwt = new JWT(rq.getHeaders().get((X_API_KEY)));
        if (!jwt.isValid()) {
            log.error("JWT is not valid");
            rs.send(403, "");
            return;
        }
        try {
            var json = (JSONObject) new JSONParser().parse(jwt.payload());
            var login = (String)json.get(User.LOGIN);
            if (blacklisted.contains(login)) {
                log.error("user " + login + " blocked");
                rs.send(403, "");
                return;
            }
            var user = users.get(login);
            if (user == null) {
                log.error("user not found " + login);
                rs.send(403, "");
                return;
            }
            var country = countries.country(ip);
            if (country != user.country()) {
                log.error("IP country \"" + country.name + "\" does not match user country \"" + user.country().name + "\"");
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
