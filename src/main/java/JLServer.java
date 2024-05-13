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
            host.addContext("/auth", (rq, rs) -> { auth(rq, rs); return 0; }, "POST");
            host.addContext("/user", user((rq, rs, user) -> { }), "GET");
            host.addContext("/user", user((rq, rs, user) -> { }), "PUT");
            host.addContext("/user", user((rq, rs, user) -> { }), "PATCH");
            host.addContext("/blacklist/subnet", admin((rq, rs, user) -> { }), "PUT");
            host.addContext("/blacklist/subnet", admin((rq, rs, user) -> { }), "DELETE");
            host.addContext("/blacklist/user", admin((rq, rs, user) -> { }), "PUT");
            host.addContext("/blacklist/user", admin((rq, rs, user) -> { }), "DELETE");
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void auth(HTTPServer.Request rq, HTTPServer.Response rs) throws IOException {
        try {
            var ip = IPRange.ip(rq.getHeaders().get(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                log.error("blocked IP " + ip);
                rs.send(403, "");
                return;
            }
            var body = rq.getBody();
            var json = (JSONObject) new JSONParser().parse(new InputStreamReader(body));
            var login = (String) json.get(User.LOGIN);
            var user = users.get(login);
            if (user == null) {
                log.error("user not found " + login);
                rs.send(403, "");
                return;
            }
            var password = (String) json.get(User.PASSWORD);
            if (!user.password().equals(password)) {
                log.error("user password invalid \"" + password + "\"");
                rs.send(403, "");
                return;
            }
            if (blacklisted.contains(login)) {
                log.error("blocked user " + login);
                rs.send(403, "");
                return;
            }
            var country = countries.country(ip);
            if (country != user.country()) {
                log.error("IP country \"" + country.name + "\" does not match user country \"" + user.country().name + "\"");
                rs.send(403, "");
                return;
            }
            rs.getHeaders().add("Content-Type", "application/json");
            var nonce = (String) json.get("nonce");
            rs.send(200, new JWT(login, nonce).toJSON());
        } catch (Exception e) {
            rs.send(400, e.getMessage());
        }
    }

    ContextHandler admin(UserHandler handler) {
        return (HTTPServer.Request rq, HTTPServer.Response rs) -> {
            return 0;
        };
    }

    ContextHandler user(UserHandler handler) {
        return (HTTPServer.Request rq, HTTPServer.Response rs) -> {
            return 0;
        };
    }

    interface UserHandler {
        void handle(HTTPServer.Request rq, HTTPServer.Response rs, User user);
    }
}
