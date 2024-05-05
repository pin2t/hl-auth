import org.json.simple.JSONObject;

import java.util.Objects;

public class User {
    static final String PASSWORD = "password";
    static final String LOGIN = "login";
    static final String COUNTRY = "country";
    static final String NAME = "name";
    static final String PHONE = "phone";
    static final String IS_ADMIN = "is_admin";
    //    final String password, json, login, country, name, phone;
//    final boolean isAdmin;
    final JSONObject json;

    public User(JSONObject json) {
        this.json = json;
    }

    String password()  { return (String)json.get(PASSWORD); }
    String login()     { return (String)json.get(LOGIN); }
    Country country()  { return Country.fromName((String)json.get(COUNTRY)); }
    String name()      { return (String)json.get(NAME); }
    String phone()     { return (String)json.get(PHONE); }
    boolean isAdmin()  { return (Boolean)json.getOrDefault(IS_ADMIN, false); }
    String toJSON()    {
        var o = new JSONObject(json);
        o.remove(PASSWORD);
        return o.toJSONString();
    }
}
