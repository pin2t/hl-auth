import java.io.*;

import static java.lang.System.out;

public class LogPrintWriter {
    final PrintWriter output;

    LogPrintWriter(PrintWriter out) {
        this.output = out;
    }

    void print(String s) {
        output.print(s);
        out.print("<<< " + s);
    }

    void flush() {
        output.flush();
    }
}
