import countries.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import static java.lang.System.out;

public class User {
    static final String PASSWORD = "password";
    static final String LOGIN = "login";
    static final String COUNTRY = "country";
    static final String NAME = "name";
    static final String PHONE = "phone";
    static final String IS_ADMIN = "is_admin";
    static final String IS_ADMIN_TRUE = "\"is_admin\":true,";
    static final String COUNTRY1 = "\"country\":\"";
    static final String PHONE1 = "\",\"phone\":\"";
    static final String NAME1 = "\",\"name\":\"";
    static final String LOGIN1 = "\",\"login\":\"";
    final String name, login, password, phone;
    final Country country;
    final boolean isAdmin;

    public User(JSONObject json) {
        this.name = (String)json.get(NAME);
        this.login = (String)json.get(LOGIN);
        this.password = (String)json.get(PASSWORD);
        this.phone = (String)json.get(PHONE);
        this.country = Country.fromName((String)json.get(COUNTRY));
        this.isAdmin = (Boolean)json.getOrDefault(IS_ADMIN, false);
    }

    String password()  { return this.password; }
    String login()     { return this.login; }
    Country country()  { return this.country; }
    String name()      { return this.name; }
    String phone()     { return this.phone; }
    boolean isAdmin()  { return this.isAdmin; }

    String toJSON()    {
        StringBuilder json = new StringBuilder(100);
        json.append('{');
        if (isAdmin) {
            json.append(IS_ADMIN_TRUE);
        }
        json.append(COUNTRY1).append( this.country.name)
                .append(PHONE1).append(this.phone)
                .append(NAME1).append(this.name)
                .append(LOGIN1).append(this.login)
                .append("\"}");
        return json.toString();
    }
}
