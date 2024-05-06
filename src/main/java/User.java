import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import java.util.Objects;

public class User {
    static final String PASSWORD = "password";
    static final String LOGIN = "login";
    static final String COUNTRY = "country";
    static final String NAME = "name";
    static final String PHONE = "phone";
    static final String IS_ADMIN = "is_admin";
    final String password;
    final JSONObject json;
    final String response;

    public User(JSONObject json) {
        this.json = json;
        this.password = (String)json.get(PASSWORD);
        if (json.containsKey(IS_ADMIN)) {
            this.response = "{" +
                    "\"country\":\"" + (String)json.get(COUNTRY) + "\"," +
                    "\"is_admin\":\"" + json.get(IS_ADMIN) + "\"," +
                    "\"login\":\"" + (String)json.get(LOGIN) + "\"," +
                    "\"name\":\"" + (String)json.get(NAME) + "\"," +
                    "\"phone\":\"" + (String)json.get(PHONE) + "\"" +
                    "}";
        } else {
            this.response = "{" +
                    "\"country\":\"" + (String)json.get(COUNTRY) + "\"," +
                    "\"login\":\"" + (String)json.get(LOGIN) + "\"," +
                    "\"name\":\"" + (String)json.get(NAME) + "\"," +
                    "\"phone\":\"" + (String)json.get(PHONE) + "\"" +
                    "}";
        }
    }

    public User(String json) {
        try {
            this.json = (JSONObject) new JSONParser().parse(json);
            this.password = (String)this.json.get(PASSWORD);
            if (this.json.containsKey(IS_ADMIN)) {
                this.response = "{" +
                        "\"country\":\"" + (String)this.json.get(COUNTRY) + "\"," +
                        "\"is_admin\":\"" + this.json.get(IS_ADMIN) + "\"," +
                        "\"login\":\"" + (String)this.json.get(LOGIN) + "\"," +
                        "\"name\":\"" + (String)this.json.get(NAME) + "\"," +
                        "\"phone\":\"" + (String)this.json.get(PHONE) + "\"" +
                        "}";
            } else {
                this.response = "{" +
                        "\"country\":\"" + (String)this.json.get(COUNTRY) + "\"," +
                        "\"login\":\"" + (String)this.json.get(LOGIN) + "\"," +
                        "\"name\":\"" + (String)this.json.get(NAME) + "\"," +
                        "\"phone\":\"" + (String)this.json.get(PHONE) + "\"" +
                        "}";
            }
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
        return response;
    }
}
