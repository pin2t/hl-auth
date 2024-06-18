import java.util.*;

public class TreeCountries {
    final Node root;

    public TreeCountries() {
        this.root = new Node();
    }

    public void put(IPRange range, Country country) {
        var node = root;
        for (int i = 31; i >= (32 - range.mask); i--) {
            int bit = (int)(range.first >> i) & 1;
            if (node.children[bit] == Node.LEAF) {
                node.children[bit] = new Node();
            }
            node = node.children[bit];
        }
        node.country = Optional.of(country);
    }

    public Country get(long ip) {
        var country = Country.NO;
        var node = root;
        for (int i = 31; i >= 0; i--) {
            int bit = (int)(ip >> i) & 1;
            if (node.children[bit] == Node.LEAF) {
                return country;
            }
            node = node.children[bit];
            if (node.country.isPresent()) {
                country = node.country.get();
            }
        }
        return country;
    }

    static class Node {
        static Node LEAF = new Node();
        Node[] children;
        Optional<Country> country;

        Node() {
            this.children = new Node[]{LEAF, LEAF};
            this.country = Optional.empty();
        }
    }
}
