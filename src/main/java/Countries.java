import org.slf4j.*;
import java.io.*;
import java.util.*;

class Countries {
    static final Logger log = LoggerFactory.getLogger(Countries.class);
    final Bucket[] buckets;

    Countries() {
        this.buckets = new Bucket[256];
        for (var i = 0; i < buckets.length; i++) this.buckets[i] = new Bucket();
    }

    void load() {
        var ipsFile = new File("/storage/data/GeoLite2-City-CSV/GeoLite2-City-Blocks-IPv4.csv");
        var locationsFile = new File("/storage/data/GeoLite2-City-CSV/GeoLite2-City-Locations-en.csv");
        if (!ipsFile.exists() || !locationsFile.exists()) {
            ipsFile = new File("data/GeoLite2-City-CSV/GeoLite2-City-Blocks-IPv4.csv");
            locationsFile = new File("data/GeoLite2-City-CSV/GeoLite2-City-Locations-en.csv");
        }
        if (!locationsFile.exists() || !ipsFile.exists()) {
            log.info("GeoLite2 csv files not found");
            return;
        }
        try {
            var started = System.nanoTime();
            Map<String, String> ids = new HashMap<>(100000);
            try (var reader = new BufferedReader(new FileReader(locationsFile), 1000000)) {
                reader.readLine();
                reader.lines().forEach(line -> {
                    try {
                        var fields = csvFields(line);
                        var name = fields.get(5);
                        ids.put(fields.get(0), name);
                    } catch (Exception e) {
                        log.error("error parsing " + line, e);
                    }
                });
            }
            long[] count = new long[]{0};
            try (var reader = new BufferedReader(new FileReader(ipsFile), 1000000)) {
                reader.readLine();
                reader.lines().forEach(line -> {
                    try {
                        var fields = csvFields(line);
                        var name = ids.get(!fields.get(1).isEmpty() ? fields.get(1) : fields.get(2));
                        if (name == null || name.isEmpty()) {
                            return;
                        }
                        var range = new IPRange(fields.get(0));
                        this.buckets[(int) (range.first / 0x1000000)].ranges.put(range, Country.fromName(name));
                        count[0]++;
                    } catch (Exception e) {
                        log.error("error parsing " + line, e);
                    }
                });
            }
            log.info(String.format("Loaded %d ranges from %s in %.2f s", count[0], ipsFile.getCanonicalPath(), (System.nanoTime() - started) / 1000000000.));
        } catch (Exception e) {
            log.error("unhandled exception", e);
        }
    }

    List<String> csvFields(String line) {
        char[] chars = line.toCharArray();
        List<String> result = new ArrayList<>(16);
        int p = 0;
        boolean inside = false;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '"') {
                inside = !inside;
            } else if (chars[i] == ',' && !inside) {
                var s = line.substring(p, i);
                if (!s.isEmpty() && s.charAt(0) == '"') {
                    result.add(s.substring(1, s.length() - 1));
                } else {
                    result.add(s);
                }
                p = i + 1;
            }
        }
        return result;
    }

    Country country(long ip) {
        for (var e : this.buckets[(int) (ip / 0x1000000)].ranges.entrySet()) {
            if (e.getKey().contains(ip)) return e.getValue();
        }
        return Country.NO;
    }

    static class Bucket {
        final Map<IPRange, Country> ranges = new HashMap<>(15000);
    }
}
