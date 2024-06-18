class JSONString {
    final String json;

    JSONString(String json) {
        this.json = json;
    }

    String field(String prefix) {
        var i = json.indexOf(prefix);
        var j = json.indexOf('\"', i + prefix.length() + 1);
        return json.substring(i + prefix.length() + 1, j);
    }

    Pair<JSONString, String> remove(String prefix) {
        var i = json.indexOf(prefix);
        if (i < 0) {
            return new Pair<>(this, "");
        }
        var j = json.indexOf('\"', i + prefix.length() + 1);
        var val = json.substring(i + prefix.length() + 1, j);
        return new Pair<>(new JSONString(json.substring(0, i) + json.substring(j + 2)), val);
    }

    Pair<JSONString, String> removeBoolean(String prefix) {
        var i = json.indexOf(prefix);
        if (i < 0) {
            return new Pair<>(this, "");
        }
        if (json.charAt(i + prefix.length()) == 't') {
            return new Pair<>(new JSONString(json.substring(0, i - 1) + json.substring(i + prefix.length() + 4)), "true");
        } else {
            return new Pair<>(new JSONString(json.substring(0, i - 1) + json.substring(i + prefix.length() + 5)), "false");
        }
    }

    String toJSON() { return json; }
}
