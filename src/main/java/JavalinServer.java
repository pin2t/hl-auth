import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.Javalin;
import io.javalin.http.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class JavalinServer {
    static final Logger log = LoggerFactory.getLogger(JavalinServer.class);
    static final String ALG_HS_256_TYP_JWT = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
    static final String X_API_KEY = "X-API-Key";
    static final String LOGIN = "login";
    static final String QLOGIN = "\"login\"";
    static final String QPASSWORD = "\"password\"";
    static final String QNONCE = "\"nonce\"";

    final Users users;
    final Countries countries;
    final Algorithm hs256 = Algorithm.HMAC256(Base64.getDecoder().decode("CGWpjarkRIXzCIIw5vXKc+uESy5ebrbOyVMZvftj19k="));
    final Set<String> blacklisted = Collections.newSetFromMap(new ConcurrentHashMap<>());
    final IPRanges blacklistedIPs = new IPRanges();

    JavalinServer(Users users, Countries countries) {
        this.users = users;
        this.countries = countries;
    }

    void run() throws FileNotFoundException {
        assert new IPRange("205.161.14.0/23").contains(IPRange.ip("205.161.15.2"));
        assert !new IPRange("46.32.0.0/19").contains(IPRange.ip("46.31.243.46"));
        var bl = new IPRanges();
        bl.add("41.174.0.0", "16");
        assert bl.contains(IPRange.ip("41.174.13.223"));
        Javalin.create(config -> {
                config.http.disableCompression();
                config.showJavalinBanner = false;
                config.useVirtualThreads = true;
                config.pvt.jetty.httpConfigurationConfigs.add(c -> c.setHeaderCacheSize(512));
                //            config.jetty.threadPool = new QueuedThreadPool(4, 4, new LinkedBlockingDeque<>());
            }).post("/auth", this::auth)
            .get("/user", this::getUser)
            .put("/user", this::createUser)
            .patch("/user", this::updateUser)
            .put("/blacklist/subnet/{ip}/{mask}", this::block)
            .delete("/blacklist/subnet/{ip}/{mask}", this::unblock)
            .put("/blacklist/user/{login}", this::blockUser)
            .delete("/blacklist/user/{login}", this::unblockUser)
            .start(8080);
    }

    void auth(Context ctx) {
        try {
            var body = ctx.body();
            var login = jsonValue(body, QLOGIN);
            var user = users.get(login);
            if (user == null) {
                ctx.status(403);
                return;
            }
            var password = jsonValue(body, QPASSWORD);
            if (!user.password().equals(password)) {
                ctx.status(403);
                return;
            }
            if (blacklisted.contains(login)) {
                ctx.status(403);
                return;
            }
            var ip = IPRange.ip(ctx.header(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                ctx.status(403);
                return;
            }
            var country = countries.country(ip);
            if (country != user.country()) {
                ctx.status(403);
                return;
            }
            ctx.contentType(ContentType.APPLICATION_JSON);
            var nonce = jsonValue(body, QNONCE);
            var payload = new StringBuilder(128).append("{\"login\":\"").append(login).append("\",\"nonce\":\"").append(nonce).append("\"}");
            ctx.result("\"" + JWT.create().withHeader(ALG_HS_256_TYP_JWT).withPayload(payload.toString()).sign(hs256) + "\"");
            ctx.status(200);
        } catch (Exception e) {
            ctx.status(400);
        }
    }

    void getUser(Context ctx) {
        runUser(ctx, user -> {
            ctx.json(user.toJSON());
            ctx.status(200);
        });
    }

    void createUser(Context ctx) {
        try {
            var ip = IPRange.ip(ctx.header(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                ctx.status(403);
                return;
            }
            var json = (JSONObject)new JSONParser().parse(ctx.body());
            var login = (String)json.get(LOGIN);
            if (users.get(login) != null) {
                ctx.status(409);
                return;
            }
            users.put(login, new User(json));
            ctx.status(201);
        } catch (ParseException e) {
            ctx.status(400);
        }
    }

    void updateUser(Context ctx) {
        runUser(ctx, user -> {
            try {
                var json = (JSONObject)new JSONParser().parse(ctx.body());
                if (user.json.containsKey("is_admin")) {
                    json.putIfAbsent("is_admin", user.isAdmin());
                }
                json.putIfAbsent(LOGIN, user.login());
                json.putIfAbsent("country", user.json.get("country"));
                json.putIfAbsent("password", user.password());
                json.putIfAbsent("name", user.name());
                json.putIfAbsent("phone", user.phone());
                users.put(user.login(), new User(json));
                ctx.status(202);
            } catch (ParseException e) {
                ctx.status(400);
            }
        });
    }

    void block(Context ctx) {
        runAdmin(ctx, () -> {
            var ip = ctx.pathParam("ip");
            var mask = ctx.pathParam("mask");
            if (blacklistedIPs.contains(ip, mask)) {
                ctx.status(409);
                return;
            }
            blacklistedIPs.add(ip, mask);
            ctx.status(201);
        });
    }

    void unblock(Context ctx) {
        runAdmin(ctx, () -> {
            var ip = ctx.pathParam("ip");
            var mask = ctx.pathParam("mask");
            if (!blacklistedIPs.contains(ip, mask)) {
                ctx.status(404);
                return;
            }
            blacklistedIPs.remove(ip, mask);
            ctx.status(204);
        });
    }

    void blockUser(Context ctx) {
        runAdmin(ctx, () -> {
            var login = ctx.pathParam(LOGIN);
            var user = users.get(login);
            if (user == null) {
                ctx.status(404);
                return;
            }
            var ip = IPRange.ip(ctx.header(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                ctx.status(409);
                return;
            }
            if (!blacklisted.add(login)) {
                ctx.status(409);
                return;
            }
            ctx.status(201);
        });
    }

    void unblockUser(Context ctx) {
        runAdmin(ctx, () -> {
            var login = ctx.pathParam(LOGIN);
            var user = users.get(login);
            if (user == null) {
                ctx.status(404);
                return;
            }
            if (!blacklisted.remove(login)) {
                ctx.status(404);
                return;
            }
            ctx.status(204);
        });
    }

    void runUser(Context ctx, Consumer<User> operation) {
        try {
            DecodedJWT jwt = JWT.require(hs256).build().verify(ctx.header(X_API_KEY));
            var payload = new String(Base64.getDecoder().decode(jwt.getPayload()));
            var login = jsonValue(payload, QLOGIN);
            var user = users.get(login);
            if (user == null) {
                ctx.status(403);
                return;
            }
            if (blacklisted.contains(login)) {
                ctx.status(403);
                return;
            }
            var ip = IPRange.ip(ctx.header(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                ctx.status(403);
                return;
            }
            var country = countries.country(ip);
            if (country != user.country()) {
                ctx.status(403);
                return;
            }
            operation.accept(user);
        } catch (JWTVerificationException e) {
            ctx.status(403);
        }
    }

    void runAdmin(Context ctx, Runnable operation) {
        try {
            DecodedJWT jwt = JWT.require(hs256).build().verify(ctx.header(X_API_KEY));
            var payload = new String(Base64.getDecoder().decode(jwt.getPayload()));
            var login = jsonValue(payload, QLOGIN);
            var admin = users.get(login);
            if (admin == null) {
                ctx.status(403);
                return;
            }
            if (!admin.isAdmin()) {
                ctx.status(403);
                return;
            }
            if (blacklisted.contains(login)) {
                ctx.status(403);
                return;
            }
            var ip = IPRange.ip(ctx.header(X_FORWARDED_FOR));
            if (blacklistedIPs.contains(ip)) {
                ctx.status(403);
                return;
            }
            var country = countries.country(ip);
            if (country != admin.country()) {
                ctx.status(403);
                return;
            }
            operation.run();
        } catch (JWTVerificationException e) {
            ctx.status(403);
        }
    }

    String jsonValue(String json, String qkey) {
        int i = json.indexOf(qkey);
        if (i == -1) return null;
        int start = i + qkey.length() + 2;
        int end = json.indexOf('"', start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
