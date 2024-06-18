import java.util.*;
import java.util.concurrent.*;

public class TreeRanges {

    TreeRanges() {
    }

    boolean contains(long ip) {
        return false;
    }

    boolean contains(String ip, String mask) {
        return false;
    }

    void add(String ip, String mask) {
    }

    void remove(String ip, String mask) {
    }

    static class Node {
        static final Node LEAF = new Node();
        Node[] children;
        IPRange range;


    }
}
