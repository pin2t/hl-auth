import java.util.*;
import java.util.concurrent.locks.*;

public class TreeRanges {
    final Node root = new Node();
    final ReadWriteLock lock = new ReentrantReadWriteLock();

    boolean contains(long ip) {
        lock.readLock().lock();
        try {
            var node = root;
            for (var b = 31; b >= 0; b--) {
                if (node.range.isPresent() && node.range.get().contains(ip)) {
                    return true;
                }
                var bit = (int) (ip >> b) & 1;
                if (node.children[bit] == Node.LEAF) {
                    return false;
                }
                node = node.children[bit];
            }
            return node.range.isPresent() && node.range.get().contains(ip);
        } finally {
            lock.readLock().unlock();
        }
    }

    boolean contains(IPRange range) {
        lock.readLock().lock();
        try {
            var node = root;
            for (var b = 31; b > (31 - range.mask); b--) {
                var bit = (int) (range.first >> b) & 1;
                if (node.children[bit] == Node.LEAF) {
                    return false;
                }
                node = node.children[bit];
            }
            return node.range.isPresent() && node.range.get().equals(range);
        } finally {
            lock.readLock().unlock();
        }
    }

    void add(IPRange range) {
        lock.writeLock().lock();
        try {
            var node = root;
            for (var b = 31; b > (31 - range.mask); b--) {
                var bit = (int) (range.first >> b) & 1;
                if (node.children[bit] == Node.LEAF) {
                    node.children[bit] = new Node();
                }
                node = node.children[bit];
            }
            node.range = Optional.of(range);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void remove(IPRange range) {
        lock.writeLock().lock();
        try {
            var node = root;
            for (var b = 31; b > (31 - range.mask); b--) {
                var bit = (int)(range.first >> b) & 1;
                if (node.children[bit] == Node.LEAF) {
                    return;
                }
                node = node.children[bit];
            }
            node.range = Optional.empty();
        } finally {
            lock.writeLock().unlock();
        }
    }

    static class Node {
        static final Node LEAF = new Node();
        Node[] children = new Node[]{LEAF, LEAF};
        Optional<IPRange> range = Optional.empty();
    }
}
