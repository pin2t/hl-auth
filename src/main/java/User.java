import countries.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

public class User {
    static final String PASSWORD = "password";
    static final String LOGIN = "login";
    static final String COUNTRY = "country";
    static final String NAME = "name";
    static final String PHONE = "phone";
    static final String IS_ADMIN = "is_admin";
    final String password;
    final JSONObject json;

    public User(JSONObject json) {
        this.json = json;
        this.password = (String)json.get(PASSWORD);
    }

    public User(String json) {
        try {
            this.json = (JSONObject) new JSONParser().parse(json);
            this.password = (String)this.json.get(PASSWORD);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    String password()  { return this.password; }
    String login()     { return (String)json.get(LOGIN); }
    Country country()  { return Country.fromName((String)json.get(COUNTRY)); }
    String name()      { return (String)json.get(NAME); }
    String phone()     { return (String)json.get(PHONE); }
    boolean isAdmin()  { return (Boolean)json.getOrDefault(IS_ADMIN, false); }

    String toJSON()    {
        if (json.containsKey(IS_ADMIN)) {
            return "{" +
                    "\"is_admin\":" + json.get(IS_ADMIN)  + "," +
                    "\"country\":\"" + (String)json.get(COUNTRY) + "\"," +
                    "\"phone\":\"" + (String)json.get(PHONE) + "\"," +
                    "\"name\":\"" + (String)json.get(NAME) + "\"," +
                    "\"login\":\"" + (String)json.get(LOGIN) + "\"" +
                    "}";
        } else {
            return "{" +
                    "\"country\":\"" + (String)json.get(COUNTRY) + "\"," +
                    "\"phone\":\"" + (String)json.get(PHONE) + "\"," +
                    "\"name\":\"" + (String)json.get(NAME) + "\"," +
                    "\"login\":\"" + (String)json.get(LOGIN) + "\"" +
                    "}";
        }
    }
}
