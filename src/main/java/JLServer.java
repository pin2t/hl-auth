import com.auth0.jwt.algorithms.*;
import net.freeutils.httpserver.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class JLServer {
    static Executor pool = Executors.newVirtualThreadPerTaskExecutor();
    final Users users;
    final Algorithm hs256 = Algorithm.HMAC256(Base64.getDecoder().decode("CGWpjarkRIXzCIIw5vXKc+uESy5ebrbOyVMZvftj19k="));
    final Set<String> blacklisted = Collections.newSetFromMap(new ConcurrentHashMap<>());
    final IPRanges blacklistedIPs = new IPRanges();
    final Countries countries;

    JLServer(Users users, Countries countries) {
        this.users = users;
        this.countries = countries;
    }

    void run() {
        int port = 9000;
        HTTPServer server = new HTTPServer(port);
        try {
            server.setExecutor(pool);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
