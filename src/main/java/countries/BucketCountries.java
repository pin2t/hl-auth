package countries;

import org.slf4j.*;

import java.util.*;

public class BucketCountries {
    static final Logger log = LoggerFactory.getLogger(BucketCountries.class);
    final Bucket[] buckets;

    public BucketCountries() {
        this.buckets = new Bucket[256];
        for (var i = 0; i < buckets.length; i++)
            this.buckets[i] = new Bucket();
    }

    public void put(IPRange range, Country country) {
        this.buckets[(int) (range.first / 0x1000000)].ranges.put(range, country);
    }

    Country get(long ip) {
        for (var e : this.buckets[(int) (ip / 0x1000000)].ranges.entrySet()) {
            if (e.getKey().contains(ip)) {
                return e.getValue();
            }
        }
        return Country.NO;
    }

    static class Bucket {
        final Map<IPRange, Country> ranges = new HashMap<>(15000);
    }
}
