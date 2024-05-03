import java.util.HashMap;
import java.util.Map;

public class Users {
    final Map<String, User> users = new HashMap<>();

    public Users() {
    }

    public void put(String login, User user) {
        users.put(login, user);
    }

    public User get(String login) {
        return users.get(login);
    }
}
