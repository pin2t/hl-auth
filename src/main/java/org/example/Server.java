package org.example;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class Server {
    static final Logger log = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws FileNotFoundException {
        InputStream input;
        var file = new File("/storage/data/users.jsonl");
        if (file.exists()) {
            input = new FileInputStream(file);
        } else {
            input = Thread.currentThread().getContextClassLoader().getResourceAsStream("users.jsonl");
        }
        new BufferedReader(new InputStreamReader(input)).lines().forEach(line -> {
            log.info(line);
        });
        log.info("starting...");
        Javalin.create(config -> config.useVirtualThreads = true)
            .get("/user", ctx -> ctx.result("Hello World"))
            .put("/user", ctx -> ctx.result("Hello World"))
            .patch("/user", ctx -> ctx.result("Hello World"))
            .start(8080);
    }
}
