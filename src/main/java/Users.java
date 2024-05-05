import org.json.simple.*;
import org.json.simple.parser.*;
import org.slf4j.*;

import java.io.*;
import java.util.Map;
import java.util.concurrent.*;

public class Users {
    static final Logger log = LoggerFactory.getLogger(Users.class);

    final JSONParser parser = new JSONParser();
    final Map<String, User> users = new ConcurrentHashMap<>();

    public Users(String... files) {
        for (var name : files) {
            var f = new File(name);
            if (!f.exists()) { continue; }
            var started = System.nanoTime();
            var prevSize = users.size();
            try {
                new BufferedReader(new InputStreamReader(new FileInputStream(f)), 10000000).lines().forEach(line -> {
                    try {
                        var json = (JSONObject)parser.parse(line);
                        var login = (String)json.get("login");
                        users.put(login, new User(json));
                    } catch (ParseException e) {
                        log.warn("Error parsing " + line, e);
                    }
                });
            } catch (FileNotFoundException e) {
                log.error("error loading users from " + name, e);
            }
            log.info(String.format("Loaded %d users from %s in %.2f s", users.size() - prevSize, name, (System.nanoTime() - started) / 1000000000.));
        }
    }

    public void put(String login, User user) {
        users.put(login, user);
    }

    public User get(String login) {
        return users.get(login);
    }
}
