import org.slf4j.*;

import java.util.*;

import static java.lang.Math.min;

public class IPRange {
    static final Logger log = LoggerFactory.getLogger(IPRange.class);
    static long minMask = 32L;
    final long first, last;
    final String network;

    IPRange(String network) {
        this.network = network;
        var slash = network.indexOf('/');
        assert slash > 0;
        var ip = ip(network.substring(0, slash));
        var mask = Long.parseLong(network.substring(slash + 1));
        minMask = min(minMask, mask);
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
        long result = 0;
        int p = 0, j = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '.') {
                result += Long.parseLong(s.substring(p, i)) << (24 - (8 * j));
                j++;
                p = i + 1;
            }
        }
        return result;
    }
}
