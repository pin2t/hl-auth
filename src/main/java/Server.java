import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
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

public class Server {
    static final Logger log = LoggerFactory.getLogger(Server.class);

    Users users = new Users();
    JSONParser parser = new JSONParser();
    Algorithm hs256 = Algorithm.HMAC256(Base64.getDecoder().decode("CGWpjarkRIXzCIIw5vXKc+uESy5ebrbOyVMZvftj19k="));

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
                var o = (JSONObject)parser.parse(line);
                var login = (String)o.get("login");
                users.put(login, new User((String)o.get("name"), (String)o.get("password"), line));
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
            var token = JWT.create()
                .withHeader("{\"alg\":\"HS256\",\"typ\": \"JWT\"}")
                .withPayload("{\"login\":\"" + login + "\",\"nonce\": \"" + (String)json.get("nonce") + "\"}")
                .sign(hs256);
            ctx.contentType(ContentType.APPLICATION_JSON);
            ctx.result(token);
        } catch (ParseException e) {
            ctx.status(400);
        }
    }

    void getUser(Context ctx) {
    }

    void createUser(Context ctx) {
    }

    void updateUser(Context ctx) {
    }

    void block(Context ctx) {}
    void unblock(Context ctx) {}
    void blockUser(Context ctx) {}
    void unblockUser(Context ctx) {}
}
