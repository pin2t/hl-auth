import java.util.*;
import java.util.concurrent.*;

class IPRanges {
    final Set<IPRange> ranges = Collections.newSetFromMap(new ConcurrentHashMap<>());

    IPRanges() {
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
}
