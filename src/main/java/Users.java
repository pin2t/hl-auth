import java.util.Map;
import java.util.concurrent.*;

public class Users {
    final Map<String, User> users = new ConcurrentHashMap<>();

    public Users() {
    }

    public void put(String login, User user) {
        users.put(login, user);
    }

    public User get(String login) {
        return users.get(login);
    }
}
