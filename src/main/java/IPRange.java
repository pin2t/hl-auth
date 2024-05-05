import java.util.*;

public class IPRange {
    final long first, last;

    IPRange(long ip, int mask) {
        this.first = ip - (2L << (32 - mask)) / 2 + 1;
        this.last = ip + (2L << (32 - mask)) / 2;
    }

    IPRange(String network) {
        String[] pair = network.split("/");
        assert pair.length == 2;
        var parts = pair[0].split("\\.");
        assert parts.length == 4;
        long ip = 0;
        for (int i = 0; i < 4; i++) {
            ip += Long.parseLong(parts[i]) << (24 - (8 * i));
        }
        var mask = Long.parseLong(pair[1]);
        this.first = ip - (2L << (32 - mask)) / 2 + 1;
        this.last = ip + (2L << (32 - mask)) / 2;
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
}
