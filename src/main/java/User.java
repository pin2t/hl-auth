import org.json.simple.JSONObject;

public class User {
    final String password;
    final String json;
    final boolean isAdmin;

    public User(JSONObject json) {
        this.password = (String)json.get("password");
        this.isAdmin = (Boolean)json.getOrDefault("is_admin", false);
        json.remove("password");
        this.json = json.toJSONString();
    }
}
