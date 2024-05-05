import org.slf4j.*;
import java.io.*;
import java.util.*;

class Countries {
    static final Logger log = LoggerFactory.getLogger(Countries.class);
    final Bucket[] buckets;

    Countries() {
        final Map<Integer, String> ids = new HashMap<>(100000);
        this.buckets = new Bucket[256];
        for (var i = 0; i < buckets.length; i++) this.buckets[i] = new Bucket();
        var ipsFile = new File("/storage/data/GeoLite2-City-CSV/GeoLite2-City-Blocks-IPv4.csv");
        var locationsFile = new File("/storage/data/GeoLite2-City-CSV/GeoLite2-City-Locations-en.csv");
        if (!ipsFile.exists() || !locationsFile.exists()) {
            log.info("GeoLite2 csv files not found, trying ./data");
            ipsFile = new File("data/GeoLite2-City-CSV/GeoLite2-City-Blocks-IPv4.csv");
            locationsFile = new File("data/GeoLite2-City-CSV/GeoLite2-City-Locations-en.csv");
        }
        if (!locationsFile.exists() || !ipsFile.exists()) {
            log.info("GeoLite2 csv files not found, trying ./data");
            return;
        }
        try {
            var started = System.nanoTime();
            try (var reader = new BufferedReader(new FileReader(locationsFile), 5000000)) {
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    try {
                        var fields = csvFields(line);
                        var name = fields.get(5);
                        if (!name.isEmpty() && name.charAt(0) == '\"') {
                            var i = 6;
                            while (!name.endsWith("\"") && i < fields.size()) {
                                name = name + "," + fields.get(i);
                                i++;
                            }
                            name = name.substring(1, name.length() - 1);
                        }
                        ids.put(Integer.valueOf(fields.get(0)), name);
                    } catch (Exception e) {
                        log.error("error parsing " + line, e);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            long count = 0;
            try (var reader = new BufferedReader(new FileReader(ipsFile), 10000000)) {
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    try {
                        var fields = csvFields(line);
                        var range = new IPRange(fields.get(0));
                        var name = ids.get(Integer.valueOf(!fields.get(1).isEmpty() ? fields.get(1) : fields.get(2)));
                        if (name == null || name.isEmpty()) {
                            continue;
                        }
                        this.buckets[(int) (range.first / 0xffffff)].ranges.put(range, name);
                        count++;
                    } catch (Exception e) {
                        log.error("error parsing " + line, e);
                    }
                }
            }
            log.info(String.format("Loaded %d ranges from %s in %.2f s",
                count, ipsFile.getCanonicalPath(), (System.nanoTime() - started) / 1000000000.)
            );
        } catch (Exception e) {
            log.error("unhandled exception", e);
        }
    }

    List<String> csvFields(String line) {
        char[] chars = line.toCharArray();
        List<String> result = new ArrayList<>(10);
        int p = 0;
        boolean inside = false;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '"') {
                inside = !inside;
            } else if (chars[i] == ',' && !inside) {
                result.add(line.substring(p, i));
                p = i + 1;
            }
        }
        return result;
    }

    String country(long ip) {
        for (var e : this.buckets[(int) (ip / 0xffffff)].ranges.entrySet()) {
            if (e.getKey().contains(ip)) return e.getValue();
        }
        return "";
    }

    static class Bucket {
        final Map<IPRange, String> ranges = new HashMap<>(15000);
    }
}
