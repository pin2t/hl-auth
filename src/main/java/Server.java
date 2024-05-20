import java.io.*;
import java.util.concurrent.*;

import static java.lang.System.in;
import static java.lang.System.out;

public class Server {
    final Users users = new Users("/storage/data/users.jsonl", "data/users.jsonl");
    final Countries countries = new Countries();

    public static void main(String[] args) throws IOException {
        new Server().run();
    }

    void run() throws IOException {
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        pool.submit(users::load);
        pool.submit(countries::load);
//        var server = new JavalinServer(users, countries);
        var server = new JLServer(users, countries, pool);
        server.start();
        out.println("LIstening on localhost:8080...");
        out.println("Press any key to stop");
        in.read();
        server.stop();
        pool.shutdown();
    }
}
