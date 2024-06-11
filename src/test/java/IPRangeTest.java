import countries.*;
import org.junit.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IPRangeTest {
    @Test
    public void testAS() {
        var range = new IPRange("205.161.14.0/23");
        assertTrue(range.contains(ip("205.161.15.2")));
    }

    long ip(String s) {
        var parts = s.split("\\.");
        assert parts.length == 4;
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result += Long.parseLong(parts[i]) << (24 - (8 * i));
        }
        return result;
    }
}
