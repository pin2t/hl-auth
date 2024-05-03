import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class Server {
    static final Logger log = LoggerFactory.getLogger(Server.class);

    final Users users = new Users();
    final JSONParser parser = new JSONParser();
    final Algorithm hs256 = Algorithm.HMAC256(Base64.getDecoder().decode("CGWpjarkRIXzCIIw5vXKc+uESy5ebrbOyVMZvftj19k="));
    final Set<String> blacklisted = new HashSet<>();

    public static void main(String[] args) throws FileNotFoundException {
        new Server().run();
    }

    void run() throws FileNotFoundException {
        InputStream input;
        var source = "/storage/data/users.jsonl";
        var file = new File(source);
        if (file.exists()) {
            input = new FileInputStream(file);
        } else {
            source = "users.jsonl";
            input = Thread.currentThread().getContextClassLoader().getResourceAsStream(source);
        }
        assert input != null;
        var started = System.nanoTime();
        new BufferedReader(new InputStreamReader(input)).lines().forEach(line -> {
            try {
                var json = (JSONObject)parser.parse(line);
                var login = (String)json.get("login");
                users.put(login, new User(json));
            } catch (ParseException e) {
            }
        });
        log.info(String.format("Loaded %d users from %s in %.2f s", users.users.size(), source, (System.nanoTime() - started) / 1000000000.));
        log.info("starting...");
        Javalin.create(config -> config.useVirtualThreads = true)
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
            if (user == null || !user.password.equals((String)json.get("password"))) {
                ctx.status(403);
                return;
            }
            ctx.contentType(ContentType.APPLICATION_JSON);
            ctx.result(JWT.create()
                .withHeader("{\"alg\":\"HS256\",\"typ\": \"JWT\"}")
                .withPayload("{\"login\":\"" + login + "\",\"nonce\": \"" + (String)json.get("nonce") + "\"}")
                .sign(hs256)
            );
        } catch (ParseException e) {
            ctx.status(400);
        }
    }

    void getUser(Context ctx) {
        try {
            DecodedJWT jwt = JWT.require(hs256).build().verify(ctx.header("X-API-Key"));
            var json = (JSONObject)parser.parse(new String(Base64.getDecoder().decode(jwt.getPayload())));
            var user = users.get((String)json.get("login"));
            if (user == null) {
                ctx.status(403);
                return;
            }
            ctx.contentType(ContentType.APPLICATION_JSON);
            ctx.result(user.json);
        } catch (JWTVerificationException | ParseException e) {
            ctx.status(403);
        }
    }

    void createUser(Context ctx) {
    }

    void updateUser(Context ctx) {
    }

    void block(Context ctx) {}
    void unblock(Context ctx) {}

    void blockUser(Context ctx) {
        try {
            DecodedJWT jwt = JWT.require(hs256).build().verify(ctx.header("X-API-Key"));
            var json = (JSONObject)parser.parse(new String(Base64.getDecoder().decode(jwt.getPayload())));
            var adminLogin = (String)json.get("login");
            var admin = users.get(adminLogin);
            if (admin == null || !admin.isAdmin || blacklisted.contains(adminLogin)) {
                ctx.status(403);
                return;
            }
            var login = ctx.pathParam("login");
            var user = users.get(login);
            if (user == null) {
                ctx.status(404);
                return;
            }
            if (blacklisted.contains(login)) {
                ctx.status(409);
                return;
            }
            blacklisted.add(login);
            ctx.status(201);
        } catch (JWTVerificationException | ParseException e) {
            ctx.status(403);
        }
    }

    void unblockUser(Context ctx) {
        try {
            DecodedJWT jwt = JWT.require(hs256).build().verify(ctx.header("X-API-Key"));
            var json = (JSONObject)parser.parse(new String(Base64.getDecoder().decode(jwt.getPayload())));
            var adminLogin = (String)json.get("login");
            var admin = users.get(adminLogin);
            if (admin == null || !admin.isAdmin || blacklisted.contains(adminLogin)) {
                ctx.status(403);
                return;
            }
            var login = ctx.pathParam("login");
            var user = users.get(login);
            if (user == null || !blacklisted.remove(login)) {
                ctx.status(404);
                return;
            }
            ctx.status(204);
        } catch (JWTVerificationException | ParseException e) {
            ctx.status(403);
        }
    }
}