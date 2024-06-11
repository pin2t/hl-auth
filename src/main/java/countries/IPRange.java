package countries;

import java.util.*;

public class IPRange {
    final long first, last;
    final int mask;

    public IPRange(String network) {
        var slash = network.indexOf('/');
        assert slash > 0;
        var ip = ip(network.substring(0, slash));
        this.mask = Integer.parseInt(network.substring(slash + 1));
        var first = ip;
        for (var bit = 31 - mask; bit >= 0; bit--) { first &= ~(1L << bit); }
        var last = ip;
        for (var bit = 31 - mask; bit >= 0; bit--) { last |= 1L << bit; }
        this.first = first;
        this.last = last;
    }

    public boolean contains(long ip) {
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

    public static long ip(String s) {
        long result = 0;
        int p = 0, j = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '.') {
                result += Long.parseLong(s.substring(p, i)) << (24 - (8 * j));
                j++;
                p = i + 1;
            }
        }
        result += Long.parseLong(s.substring(p));
        return result;
    }
}
