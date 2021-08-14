package org.altervista.mbilotta.julia.program;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JuliaZipOutputStream extends ZipOutputStream {

    private final Set<String> dirEntries = new HashSet<>();

    public JuliaZipOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void putNextEntry(ZipEntry e) throws IOException {
        String name = e.getName();
        boolean isDirectory = name.endsWith("/");
        String[] path = name.split("/");
        String[] parentPath = Arrays.copyOfRange(path, 0, path.length - 1);
        String ancestor = "";
        for (String segment : parentPath) {
            ancestor += segment + "/";
            if (dirEntries.add(ancestor)) {
                super.putNextEntry(new ZipEntry(ancestor));
                closeEntry();
            }
        }
        if (isDirectory) {
            if (dirEntries.add(name)) {
                super.putNextEntry(e);
            }
        } else {
            super.putNextEntry(e);
        }
    }
}
