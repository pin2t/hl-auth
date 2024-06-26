class TreeCountries {
    final Node root;

    TreeCountries() {
        this.root = new Node();
    }

    void put(IPRange range, Country country) {
        var node = root;
        for (int i = 31; i >= (32 - range.mask); i--) {
            int bit = (int)(range.first >> i) & 1;
            if (node.children[bit] == Node.LEAF) {
                node.children[bit] = new Node();
            }
            node = node.children[bit];
        }
        node.country = country;
    }

    Country get(long ip) {
        var country = Country.NO;
        var node = root;
        for (int i = 31; i >= 0; i--) {
            int bit = (int)(ip >> i) & 1;
            if (node.children[bit] == Node.LEAF) {
                return country;
            }
            node = node.children[bit];
            country = node.country;
        }
        return country;
    }

    static class Node {
        static final Node LEAF = new Node();
        final Node[] children;
        Country country;

        Node() {
            this.children = new Node[]{LEAF, LEAF};
            this.country = Country.NO;
        }
    }
}
