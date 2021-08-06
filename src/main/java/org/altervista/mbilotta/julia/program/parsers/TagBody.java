package org.altervista.mbilotta.julia.program.parsers;

public interface TagBody {
    public static final TagBody EMPTY = () -> {};
    void write();
}
