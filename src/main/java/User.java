import org.json.simple.JSONObject;

import java.util.Objects;

public class User {
    final String password, json, login, country, name, phone;
    final boolean isAdmin;

    public User(JSONObject json) {
        this.password = Objects.requireNonNull((String)json.get("password"));
        this.login = Objects.requireNonNull((String)json.get("login"));
        this.country = Objects.requireNonNull((String)json.get("country"));
        this.name = Objects.requireNonNull((String)json.get("name"));
        this.phone = Objects.requireNonNull((String)json.get("phone"));
        this.isAdmin = (Boolean)json.getOrDefault("is_admin", false);
        json.remove("password");
        this.json = Objects.requireNonNull(json.toJSONString());
    }
}
