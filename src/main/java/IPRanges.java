import java.util.*;

public class IPRanges {
    final Set<IPRange> ranges = new HashSet<>();

    public IPRanges() {
    }

    public IPRanges(IPRange... initial) {
        ranges.addAll(Arrays.asList(initial));
    }

    boolean contains(long ip) {
        return ranges.stream().anyMatch(range -> range.contains(ip));
    }

    boolean contains(String ip, String mask) {
        return ranges.contains(new IPRange(ip + "/" + mask));
    }

    void add(String ip, String mask) {
        ranges.add(new IPRange(ip + "/" + mask));
    }

    void remove(String ip, String mask) {
        ranges.remove(new IPRange(ip + "/" + mask));
    }

    void merge(IPRanges other) {
        this.ranges.addAll(other.ranges);
    }
}
