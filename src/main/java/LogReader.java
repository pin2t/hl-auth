import java.io.*;

import static java.lang.System.out;

public class LogReader {
    final BufferedReader input;

    LogReader(BufferedReader input) {
        this.input = input;
    }

    String readLine() throws IOException {
        var line = input.readLine();
        out.println(">>>" + line);
        return line;
    }

    int read(char[] buf) throws IOException {
        var n = input.read(buf);
        if (n > 0) {
            out.println(">>> " + new String(buf, 0, n));
        }
        return n;
    }
}
