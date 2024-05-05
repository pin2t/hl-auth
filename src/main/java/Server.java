import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Server {
    static final Logger log = LoggerFactory.getLogger(Server.class);

    final Users users = new Users("/storage/data/users.jsonl", "data/users.jsonl");
    final JSONParser parser = new JSONParser();
    final Algorithm hs256 = Algorithm.HMAC256(Base64.getDecoder().decode("CGWpjarkRIXzCIIw5vXKc+uESy5ebrbOyVMZvftj19k="));
    final Set<String> blacklisted = Collections.newSetFromMap(new ConcurrentHashMap<>());
    final IPRanges blacklistedIPs = new IPRanges();
    final Countries countries = new Countries();

    public static void main(String[] args) throws FileNotFoundException {
        new Server().run();
    }

    void run() throws FileNotFoundException {
        Javalin.create(config -> {}/*config.useVirtualThreads = true*/)
            .post("/auth", this::auth)
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
            var json = (JSONObject)parser.parse(ctx.body());
            var login = (String)json.get("login");
            var user = users.get(login);
            var ip = ip(ctx.header("X-FORWARDED-FOR"));
            if (user == null || !user.password.equals((String)json.get("password")) || !countries.contains(user.country, ip)) {
                ctx.status(403);
                return;
            }
            ctx.json(JWT.create()
                .withHeader("{\"alg\":\"HS256\",\"typ\": \"JWT\"}")
                .withPayload("{\"login\":\"" + login + "\",\"nonce\": \"" + (String)json.get("nonce") + "\"}")
                .sign(hs256)
            );
        } catch (ParseException e) {
            ctx.status(400);
        }
    }

    void getUser(Context ctx) {
        user(ctx, user -> ctx.json(user.json));
    }

    void createUser(Context ctx) {
        try {
            var json = (JSONObject)parser.parse(ctx.body());
            var login = (String)json.get("login");
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
        user(ctx, user -> {
            try {
                var json = (JSONObject)parser.parse(ctx.body());
                json.putIfAbsent("is_admin", user.isAdmin);
                json.putIfAbsent("login", user.login);
                json.putIfAbsent("country", user.country);
                json.putIfAbsent("password", user.password);
                users.put(user.login, new User(json));
            } catch (ParseException e) {
                ctx.status(400);
            }
        });
    }

    void block(Context ctx) {
        admin(ctx, () -> {
            var ip = ip(ctx.pathParam("ip"));
            var mask = Integer.parseInt(ctx.pathParam("mask"));
            if (blacklistedIPs.contains(ip, mask)) {
                ctx.status(409);
                return;
            }
            blacklistedIPs.add(ip, mask);
            ctx.status(201);
        });
    }

    void unblock(Context ctx) {
        admin(ctx, () -> {
            var ip = ip(ctx.pathParam("ip"));
            var mask = Integer.parseInt(ctx.pathParam("mask"));
            if (!blacklistedIPs.contains(ip, mask)) {
                ctx.status(404);
                return;
            }
            blacklistedIPs.remove(ip, mask);
            ctx.status(204);
        });
    }

    void blockUser(Context ctx) {
        admin(ctx, () -> {
            var login = ctx.pathParam("login");
            var user = users.get(login);
            if (user == null) {
                ctx.status(404);
                return;
            }
            var ip = ip(ctx.header("X-FORWARDED-FOR"));
            if (!blacklisted.add(login) || blacklistedIPs.contains(ip)) {
                ctx.status(409);
                return;
            }
            ctx.status(201);
        });
    }

    void unblockUser(Context ctx) {
        admin(ctx, () -> {
            var login = ctx.pathParam("login");
            var user = users.get(login);
            if (user == null || !blacklisted.remove(login)) {
                ctx.status(404);
                return;
            }
            ctx.status(204);
        });
    }

    void user(Context ctx, Consumer<User> operation) {
        try {
            DecodedJWT jwt = JWT.require(hs256).build().verify(ctx.header("X-API-Key"));
            var json = (JSONObject)parser.parse(new String(Base64.getDecoder().decode(jwt.getPayload())));
            var login = (String)json.get("login");
            var user = users.get(login);
            var ip = ip(ctx.header("X-FORWARDED-FOR"));
            if (user == null || blacklisted.contains(login) || blacklistedIPs.contains(ip) || !countries.contains(user.country, ip)) {
                ctx.status(403);
                return;
            }
            operation.accept(user);
        } catch (JWTVerificationException | ParseException e) {
            ctx.status(403);
        }
    }

    void admin(Context ctx, Runnable operation) {
        try {
            DecodedJWT jwt = JWT.require(hs256).build().verify(ctx.header("X-API-Key"));
            var json = (JSONObject)parser.parse(new String(Base64.getDecoder().decode(jwt.getPayload())));
            var login = (String)json.get("login");
            var admin = users.get(login);
            var ip = ip(ctx.header("X-FORWARDED-FOR"));
            if (admin == null || !admin.isAdmin || blacklisted.contains(login) || blacklistedIPs.contains(ip) ||
                !countries.contains(admin.country, ip)) {
                ctx.status(403);
                return;
            }
            operation.run();
        } catch (JWTVerificationException | ParseException e) {
            ctx.status(403);
        }
    }

    long ip(String s) {
        var parts = s.split("\\.");
        assert parts.length == 4;
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result += Long.parseLong(parts[i]) << (24 - (8 * i));
        }
        return result;
    }
}
