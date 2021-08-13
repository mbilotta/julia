package org.altervista.mbilotta.julia.program;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JuliaZipOutputStream extends ZipOutputStream {

    private Set<String> dirEntries = new HashSet<>();

    public JuliaZipOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void putNextEntry(ZipEntry e) throws IOException {
        String name = e.getName();
        String[] path = name.split("/");
        if (!name.endsWith("/")) {
            path = Arrays.copyOfRange(path, 0, path.length - 1);
        }
        String parent = "";
        for (String segment : path) {
            parent += segment + "/";
            if (dirEntries.add(parent)) {
                super.putNextEntry(new ZipEntry(parent));
                closeEntry();
            }
        }
        super.putNextEntry(e);
    }
}
