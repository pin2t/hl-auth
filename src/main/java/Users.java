import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

class Users {
    static final Logger log = LoggerFactory.getLogger(Users.class);

    final Map<String, User> users = new ConcurrentHashMap<>();
    final String[] files;

    Users(String... files) {
        this.files = files;
    }

    void load() {
        for (var name : files) {
            var f = new File(name);
            if (!f.exists()) { continue; }
            var started = System.nanoTime();
            var prevSize = users.size();
            try {
                new BufferedReader(new InputStreamReader(new FileInputStream(f))).lines().forEach(line -> {
                    var user = new User(line);
                    users.put(user.login(), user);
                });
            } catch (FileNotFoundException e) {
                log.error("error loading users from {}", name, e);
            }
            log.info(String.format("Loaded %d users from %s in %.2f s", users.size() - prevSize, name, (System.nanoTime() - started) / 1000000000.));
        }
    }

    void put(String login, User user) {
        users.put(login, user);
    }

    Optional<User> get(String login) {
        return Optional.ofNullable(users.get(login));
    }
}
