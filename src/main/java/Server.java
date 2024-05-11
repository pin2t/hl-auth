import java.io.*;
import java.util.concurrent.*;

public class Server {
    final Users users = new Users("/storage/data/users.jsonl", "data/users.jsonl");
    final Countries countries = new Countries();

    public static void main(String[] args) {
        new Server().run();
    }

    void run() {
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        pool.submit(users::load);
        pool.submit(countries::load);
        pool.shutdown();
//        try {
//            new JavalinServer(users, countries).run();
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
        new JLServer(users, countries).run();
    }
}
