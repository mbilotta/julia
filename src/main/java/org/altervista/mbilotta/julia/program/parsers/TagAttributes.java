package org.altervista.mbilotta.julia.program.parsers;

import java.util.ArrayList;
import java.util.List;

public class TagAttributes {

    private List<String> attrs = new ArrayList<>();

    public TagAttributes attr(String name, String value) {
        attrs.add(name);
        attrs.add(value);
        return this;
    }

    public TagAttributes style(String value) {
        return attr("style", value);
    }

    public TagAttributes clazz(String value) {
        return attr("class", value);
    }

    public TagAttributes type(String value) {
        return attr("type", value);
    }

    public TagAttributes id(String value) {
        return attr("id", value);
    }

    public TagAttributes name(String value) {
        return attr("name", value);
    }

    public String[] toArray() {
        return attrs.toArray(new String[0]);
    }
}
