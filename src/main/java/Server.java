import countries.*;

import java.io.*;
import java.util.concurrent.*;

import static java.lang.System.out;

public class Server {
    final Users users = new Users("/storage/data/users.jsonl", "data/users.jsonl");
    final TreeCountries countries = new TreeCountries();

    public static void main(String[] args) throws IOException {
        new Server().run();
    }

    void run() throws IOException {
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        pool.submit(users::load);
        pool.submit(() -> {
            var file = new GeoLite2Countries("/storage/data/GeoLite2-City-CSV/GeoLite2-City-Blocks-IPv4.csv", "/storage/data/GeoLite2-City-CSV/GeoLite2-City-Locations-en.csv");
            file.forEach((range, country) -> countries.put(range, country));
            file = new GeoLite2Countries("data/GeoLite2-City-CSV/GeoLite2-City-Blocks-IPv4.csv", "data/GeoLite2-City-CSV/GeoLite2-City-Locations-en.csv");
            file.forEach((range, country) -> countries.put(range, country));
        });
//        var server = new JavalinServer(users, countries);
        var server = new JLServer(users, countries, pool);
        server.start();
        out.println("Listening on localhost:8080...");
    }
}
