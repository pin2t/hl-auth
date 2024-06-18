import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

class GeoLite2Countries {
    static final Logger log = LoggerFactory.getLogger(GeoLite2Countries.class);
    final File ips, locations;

    GeoLite2Countries(String ips, String locations) {
        this.ips = new File(ips);
        this.locations = new File(locations);
    }

    void forEach(BiConsumer<IPRange, Country> action) {
        if (!ips.exists() || !locations.exists()) {
            return;
        }
        try {
            var started = System.nanoTime();
            Map<String, String> ids = new HashMap<>(100000);
            try (var reader = new BufferedReader(new FileReader(locations))) {
                reader.readLine();
                reader.lines().forEach(line -> {
                    try {
                        var fields = csv(line);
                        var name = fields.get(5);
                        ids.put(fields.get(0), name);
                    } catch (Exception e) {
                        log.error("error parsing " + line, e);
                    }
                });
            }
            long[] count = new long[]{0};
            try (var reader = new BufferedReader(new FileReader(ips))) {
                reader.readLine();
                reader.lines().forEach(line -> {
                    try {
                        var fields = csv(line);
                        var name = ids.get(!fields.get(1).isEmpty() ? fields.get(1) : fields.get(2));
                        if (name == null || name.isEmpty()) {
                            return;
                        }
                        action.accept(new IPRange(fields.get(0)), Country.fromName(name));
                        count[0]++;
                    } catch (Exception e) {
                        log.error("error parsing " + line, e);
                    }
                });
            }
            log.info(String.format("Loaded %d ranges from %s in %.2f s", count[0], ips.getCanonicalPath(), (System.nanoTime() - started) / 1000000000.));
        } catch (Exception e) {
            log.error("unhandled exception", e);
        }
    }

    List<String> csv(String line) {
        char[] chars = line.toCharArray();
        List<String> result = new ArrayList<>(16);
        int p = 0;
        boolean inString = false;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '"') {
                inString = !inString;
            } else if (chars[i] == ',' && !inString) {
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
}
