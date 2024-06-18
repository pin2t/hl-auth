import java.io.*;
import java.util.concurrent.*;

import static java.lang.System.out;

public class Server {
    final Users users = new Users("/storage/data/users.jsonl", "data/users.jsonl");
    final TreeCountries countries = new TreeCountries();

    public static void main(String[] args) throws IOException {
        new Server().run();
    }

    void run() {
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        pool.submit(users::load);
        pool.submit(() -> {
            var geolite = new GeoLite2Countries("/storage/data/GeoLite2-City-CSV/GeoLite2-City-Blocks-IPv4.csv", "/storage/data/GeoLite2-City-CSV/GeoLite2-City-Locations-en.csv");
            geolite.forEach(countries::put);
            geolite = new GeoLite2Countries("data/GeoLite2-City-CSV/GeoLite2-City-Blocks-IPv4.csv", "data/GeoLite2-City-CSV/GeoLite2-City-Locations-en.csv");
            geolite.forEach(countries::put);
        });
//        var server = new JavalinServer(users, countries);
        var server = new JLServer(users, countries, pool);
        server.start();
        out.println("Listening on localhost:8080...");
    }
}
