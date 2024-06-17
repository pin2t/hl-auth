import countries.*;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserTest {
    @Test
    public void test() {
        var user = new User("{\"login\":\"gX8Dsx_XH\",\"password\":\"p6vDlqoAbwQwOVrhkEk\",\"name\":\"4mgjPbQ\",\"phone\":\"+763680801297\",\"country\":\"Tanzania\"}");
        assertTrue(user.isValid("p6vDlqoAbwQwOVrhkEk"));
        assertEquals(Country.TANZANIA, user.country());
    }
}
