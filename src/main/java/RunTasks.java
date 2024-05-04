import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static java.lang.System.in;

public class RunTasks {
    final JSONParser parser = new JSONParser();

    public static void main(String[] args) {
        new RunTasks().run();
    }

    void run() {
        new BufferedReader(new InputStreamReader(in)).lines().forEach(line -> {
            try {
                var json = (JSONObject)parser.parse(line);
                var login = (String)json.get("login");
            } catch (ParseException e) {
            }
        });
    }
}
