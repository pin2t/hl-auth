import org.json.simple.JSONObject;

public class User {
    final String password, json, login, country;
    final boolean isAdmin;

    public User(JSONObject json) {
        this.password = (String)json.get("password");
        this.login = (String)json.get("login");
        this.country = (String)json.get("country");
        this.isAdmin = (Boolean)json.getOrDefault("is_admin", false);
        json.remove("password");
        this.json = json.toJSONString();
    }
}
