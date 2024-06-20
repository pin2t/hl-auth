import org.junit.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TreeRangesTest {
    @Test
    public void test() {
        var ranges = new TreeRanges();
        ranges.add(new IPRange("192.168.0.1/24"));
        assertTrue(ranges.contains(IPRange.ip("192.168.0.1")));
        assertTrue(ranges.contains(IPRange.ip("192.168.0.111")));
        assertTrue(ranges.contains(IPRange.ip("192.168.0.254")));
        assertFalse(ranges.contains(IPRange.ip("192.168.1.1")));
    }
}
