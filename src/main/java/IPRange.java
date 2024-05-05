import org.slf4j.*;

import java.util.*;

public class IPRange {
    static final Logger log = LoggerFactory.getLogger(IPRange.class);
    final long first, last;
    final String network;

    IPRange(String network) {
        this.network = network;
        String[] pair = network.split("/");
        assert pair.length == 2;
        var ip = ip(pair[0]);
        var mask = Long.parseLong(pair[1]);
        var first = ip;
        for (var bit = 31 - mask; bit >= 0; bit--) { first &= ~(1L << bit); }
        var last = ip;
        for (var bit = 31 - mask; bit >= 0; bit--) { last |= 1L << bit; }
        this.first = first;
        this.last = last;
    }

    boolean contains(long ip) {
        return ip >= first && ip <= last;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IPRange ipRange = (IPRange) o;
        return first == ipRange.first && last == ipRange.last;
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, last);
    }

    static long ip(String s) {
        var parts = s.split("\\.");
        assert parts.length == 4;
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result += Long.parseLong(parts[i]) << (24 - (8 * i));
        }
        return result;
    }
}
