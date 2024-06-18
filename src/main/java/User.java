import org.json.simple.*;
import org.json.simple.parser.*;

class User {
    static final String PASSWORD = "password";
    static final String LOGIN = "login";
    static final String PASSWORD_PREF = "\"password\":";
    static final String COUNTRY_PREF = "\"country\":";
    static final String IS_ADMIN_PREF = "\"is_admin\":";
    static final String LOGIN_PREF = "\"login\":";
    final JSONString json;
    final long passwordHash;
    final Country country;
    final boolean isAdmin;

    User(String json) {
        var js = new JSONString(json);
        var phash = js.remove(PASSWORD_PREF);
        this.passwordHash = phash.second().hashCode();
        this.json = phash.first();
        this.country = Country.fromName(this.json.field(COUNTRY_PREF));
        var i = json.indexOf(IS_ADMIN_PREF);
        if (i > 0) {
            this.isAdmin = json.charAt(i + 11) == 't';
        } else {
            this.isAdmin = false;
        }
    }

    User(User user, String patch) throws ParseException {
        var parser = new JSONParser();
        var opatch = (JSONObject)parser.parse(patch);
        opatch.remove("is_admin");
        var othis = (JSONObject)parser.parse(user.toJSON());
        var name = opatch.containsKey("name") ? opatch.get("name").toString() : othis.get("name").toString();
        var phone = opatch.containsKey("phone") ? opatch.get("phone").toString() : othis.get("phone").toString();
        var c = opatch.containsKey("country") ? opatch.get("country").toString() : othis.get("country").toString();
        var fields = "\"login\":\"" + othis.get("login").toString() + "\"," +
            "\"name\":\"" + name + "\"," +
            "\"phone\":\"" + phone + "\"," +
            "\"country\":\"" + c + "\"";
        if (user.isAdmin()) {
            fields = fields + ",\"is_admin\":true";
        }
        this.json = new JSONString("{" + fields + "}");
        this.passwordHash = opatch.containsKey("password") ? opatch.get("password").hashCode() : user.passwordHash;
        this.country = opatch.containsKey("country") ? Country.fromName(opatch.get("country").toString()) : user.country;
        this.isAdmin = user.isAdmin;
    }

    Country country()  { return this.country; }
    boolean isAdmin()  { return this.isAdmin; }
    String toJSON()    { return json.toJSON(); }
    boolean isValid(String password) { return password.hashCode() == passwordHash; }
    String login()     { return json.field(LOGIN_PREF); }
}
