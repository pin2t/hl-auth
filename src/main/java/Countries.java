import org.slf4j.*;
import java.io.*;
import java.util.*;

class Countries {
    static final Logger log = LoggerFactory.getLogger(Countries.class);

    final Map<Integer, String> ids = new HashMap<>();
    final Map<String, IPRanges> ranges = new HashMap<>();
    final Bucket[] buckets;

    Countries() {
        this.buckets = new Bucket[256];
        for (var i = 0; i < buckets.length; i++) this.buckets[i] = new Bucket();
        var ipsFile = new File("/storages/data/GeoLite2-City-CSV/GeoLite2-City-Blocks-IPv4.csv");
        var locationsFile = new File("/storages/data/GeoLite2-City-CSV/GeoLite2-City-Locations-en.csv");
        if (!ipsFile.exists() || !locationsFile.exists()) {
            ipsFile = new File("data/GeoLite2-City-CSV/GeoLite2-City-Blocks-IPv4.csv");
            locationsFile = new File("data/GeoLite2-City-CSV/GeoLite2-City-Locations-en.csv");
        }
        if (locationsFile.exists() && ipsFile.exists()) {
            try {
                var started = System.nanoTime();
                var scanner = new Scanner(new BufferedReader(new FileReader(locationsFile)));
                scanner.nextLine();
                while (scanner.hasNext()) {
                    var line = scanner.nextLine();
                    try {
                        var fields = line.split("\\,");
                        var name = fields[5];
                        if (!name.isEmpty() && name.charAt(0) == '\"') {
                            var i = 6;
                            while (!name.endsWith("\"") && i < fields.length) {
                                name = name + "," + fields[i];
                                i++;
                            }
                            name = name.substring(1, name.length() - 1);
                        }
                        ids.put(Integer.valueOf(fields[0]), name);
                    } catch (Exception e) {
                        log.error("error parsing " + line, e);
                    }
                }
                scanner = new Scanner(new BufferedReader(new FileReader(ipsFile)));
                scanner.nextLine();
                var count = 0;
                while (scanner.hasNext()) {
                    var line = scanner.nextLine();
                    try {
                        var fields = line.split("\\,");
                        var range = new IPRange(fields[0]);
                        var name = ids.get(Integer.valueOf(!fields[1].isEmpty() ? fields[1] : fields[2]));
                        if (name == null || name.isEmpty()) continue;
                        ranges.merge(name, new IPRanges(range), (old, _new) -> {
                            old.merge(_new);
                            return old;
                        });
                        this.buckets[(int) (range.first / 0xffffff)].ranges.put(range, name);
                        count++;
                    } catch (Exception e) {
                        log.error("error parsing " + line, e);
                    }
                }
                log.info(String.format("Loaded %d ranges for %d countries from %s in %.2f s",
                        count, ranges.keySet().size(), ipsFile.getCanonicalPath(), (System.nanoTime() - started) / 1000000000.)
                );
                log.info("minimum mask found " + IPRange.minMask);
            } catch (Exception e) {
                log.error("unhandled exception", e);
            }
        }
    }

    boolean contains(String country, long ip) {
        var rs = ranges.get(country);
        return rs == null || rs.contains(ip);
    }

    String country(long ip) {
        for (var e : this.buckets[(int) (ip / 0xffffff)].ranges.entrySet()) {
            if (e.getKey().contains(ip)) return e.getValue();
        }
        return "";
    }

    static class Bucket {
        final Map<IPRange, String> ranges = new HashMap<>();
    }
}
