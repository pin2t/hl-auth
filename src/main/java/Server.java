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
        assert new IPRange("205.161.14.0/23").contains(IPRange.ip("205.161.15.2"));
        assert !new IPRange("46.32.0.0/19").contains(IPRange.ip("46.31.243.46"));
        assert countries.contains("American Samoa", IPRange.ip("205.161.15.2"));
        assert countries.contains("Bonaire, Sint Eustatius, and Saba", IPRange.ip("140.248.60.29"));
        assert !countries.contains("Iran", IPRange.ip("46.31.243.46"));
        log.info("41.174.13.223: " + IPRange.ip("41.174.13.223"));
        var bl = new IPRanges();
        bl.add("41.174.0.0", "16");
        assert bl.contains(IPRange.ip("41.174.13.223"));
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
            var ip = IPRange.ip(ctx.header("X-FORWARDED-FOR"));
            if (user == null) {
                log.info("403 user not found " + login);
                ctx.status(403);
                return;
            }
            if (!user.password.equals((String)json.get("password"))) {
                log.info("403 wrong password " + (String)json.get("password"));
                ctx.status(403);
                return;
            }
            if (blacklisted.contains(login)) {
                log.info("403 user blacklisted " + login);
                ctx.status(403);
                return;
            }
            if (blacklistedIPs.contains(ip)) {
                log.info("403 user ip blacklisted " + ctx.header("X-FORWARDED-FOR"));
                ctx.status(403);
                return;
            }
            var country = countries.country(ip);
            if (!country.isEmpty() && !country.equals(user.country)) {
                log.info("403 request country \"" + country + "\" does not match user country \"" + user.country + "\"");
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
        runUser(ctx, user -> {
            ctx.json(user.json);
            ctx.status(200);
        });
    }

    void createUser(Context ctx) {
        try {
            var json = (JSONObject)parser.parse(ctx.body());
            var login = (String)json.get("login");
            if (users.get(login) != null) {
                log.info("409 user " + login + " already exist");
                ctx.status(409);
                return;
            }
            var ip = IPRange.ip(ctx.header("X-FORWARDED-FOR"));
            if (blacklistedIPs.contains(ip)) {
                log.info("403 blocked ip " + ctx.header("X-FORWARDED-FOR"));
                ctx.status(403);
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
                var json = (JSONObject)parser.parse(ctx.body());
                json.putIfAbsent("is_admin", user.isAdmin);
                json.putIfAbsent("login", user.login);
                json.putIfAbsent("country", user.country);
                json.putIfAbsent("password", user.password);
                json.putIfAbsent("name", user.name);
                json.putIfAbsent("phone", user.phone);
                users.put(user.login, new User(json));
                ctx.status(202);
            } catch (ParseException e) {
                ctx.status(400);
            }
        });
    }

    void block(Context ctx) {
        runAdmin(ctx, () -> {
            if (blacklistedIPs.contains(ctx.pathParam("ip"), ctx.pathParam("mask"))) {
                ctx.status(409);
                return;
            }
            blacklistedIPs.add(ctx.pathParam("ip"), ctx.pathParam("mask"));
            ctx.status(201);
        });
    }

    void unblock(Context ctx) {
        runAdmin(ctx, () -> {
            if (!blacklistedIPs.contains(ctx.pathParam("ip"), ctx.pathParam("mask"))) {
                ctx.status(404);
                return;
            }
            blacklistedIPs.remove(ctx.pathParam("ip"), ctx.pathParam("mask"));
            ctx.status(204);
        });
    }

    void blockUser(Context ctx) {
        runAdmin(ctx, () -> {
            var login = ctx.pathParam("login");
            var user = users.get(login);
            if (user == null) {
                ctx.status(404);
                return;
            }
            var ip = IPRange.ip(ctx.header("X-FORWARDED-FOR"));
            if (blacklistedIPs.contains(ip)) { ctx.status(409); return; }
            if (!blacklisted.add(login)) { ctx.status(409); return; }
            ctx.status(201);
        });
    }

    void unblockUser(Context ctx) {
        runAdmin(ctx, () -> {
            var login = ctx.pathParam("login");
            var user = users.get(login);
            if (user == null || !blacklisted.remove(login)) {
                ctx.status(404);
                return;
            }
            ctx.status(204);
        });
    }

    void runUser(Context ctx, Consumer<User> operation) {
        try {
            DecodedJWT jwt = JWT.require(hs256).build().verify(ctx.header("X-API-Key"));
            var json = (JSONObject)parser.parse(new String(Base64.getDecoder().decode(jwt.getPayload())));
            var login = (String)json.get("login");
            var user = users.get(login);
            if (user == null) {
                log.info("403 user not found " + login);
                ctx.status(403);
                return;
            }
            if (blacklisted.contains(login)) {
                log.info("403 blocked user " + login);
                ctx.status(403);
                return;
            }
            var ip = IPRange.ip(ctx.header("X-FORWARDED-FOR"));
            if (blacklistedIPs.contains(ip)) {
                log.info("403 blocked user ip " + ctx.header("X-FORWARDED-FOR"));
                ctx.status(403);
                return;
            }
            if (!countries.contains(user.country, ip)) {
                log.info("403 user country \"" + user.country + "\" does not contains IP " + ctx.header("X-FORWARDED-FOR"));
                ctx.status(403);
                return;
            }
            operation.accept(user);
        } catch (JWTVerificationException | ParseException e) {
            ctx.status(403);
        }
    }

    void runAdmin(Context ctx, Runnable operation) {
        try {
            DecodedJWT jwt = JWT.require(hs256).build().verify(ctx.header("X-API-Key"));
            var json = (JSONObject)parser.parse(new String(Base64.getDecoder().decode(jwt.getPayload())));
            var login = (String)json.get("login");
            var admin = users.get(login);
            var ip = IPRange.ip(ctx.header("X-FORWARDED-FOR"));
            if (admin == null ||
                !admin.isAdmin ||
                blacklisted.contains(login) ||
                blacklistedIPs.contains(ip) ||
                !countries.contains(admin.country, ip)) {
                ctx.status(403);
                return;
            }
            operation.run();
        } catch (JWTVerificationException | ParseException e) {
            ctx.status(403);
        }
    }
}
