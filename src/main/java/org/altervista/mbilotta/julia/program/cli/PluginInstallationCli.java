/*
 * Copyright (C) 2020 Maurizio Bilotta.
 * 
 * This file is part of Julia. See <http://mbilotta.altervista.org/>.
 * 
 * Julia is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Julia is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Julia. If not, see <http://www.gnu.org/licenses/>.
 *
 */

 package org.altervista.mbilotta.julia.program.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.altervista.mbilotta.julia.Printer;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.LockedFile;
import org.altervista.mbilotta.julia.program.Profile;
import org.altervista.mbilotta.julia.program.Profile.PluginInstaller;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "install",
    description = "Install a Julia plugin (*.jup).",
	descriptionHeading = "%n",
	parameterListHeading = "%nParameters:%n",
	optionListHeading = "%nOptions:%n",
    sortOptions = false)
public class PluginInstallationCli implements Runnable {

    @ParentCommand
	MainCli mainCli;

    @Parameters(index = "0")
    File jupFile;

    @ArgGroup(exclusive = true)
    FileOverwriteBehaviour fileOverwriteBehaviour;

    static class FileOverwriteBehaviour {
        @Option(names = "--preserve-existing", required = true)
        boolean preserve;

        @Option(names = "--overwrite-existing", required = true)
        boolean overwrite;

        @Option(names = "--ask-if-existing", required = true)
        boolean ask;
    }

    @Override
    public void run() {
        Utilities.debug.setEnabled(mainCli.debugOutputEnabled);

        Utilities.print("Locking profile...");
        Utilities.flush();
        Profile profile = mainCli.getProfilePath() == null ? Profile.getDefaultProfile() : new Profile(mainCli.getProfilePath());
        try (LockedFile lock = profile.lock()) {
            Utilities.println(" done.");

            Utilities.print("Opening log file...");
            Utilities.flush();
            try (
                Printer printer = Printer.newPrinter(
                    Files.newBufferedWriter(
                            profile.getInstallerOutputFile(),
                            Charset.defaultCharset(),
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND
                    ),
                    false
                );
            ) {
                Utilities.println(" done.");

                Utilities.println("Installing plugins...");
                PluginInstaller installer = profile.new CliPluginInstaller(jupFile, printer);
                if (fileOverwriteBehaviour != null) {
                    if (fileOverwriteBehaviour.preserve) {
                        installer.setFileOverwriteBehaviour(Profile.FileOverwriteBehaviour.PRESERVE_EXISTING);
                    } else if (fileOverwriteBehaviour.overwrite) {
                        installer.setFileOverwriteBehaviour(Profile.FileOverwriteBehaviour.REPLACE_EXISTING);
                    }
                }
                try {
                    installer.install();
                } catch (Exception e) {
                    // Should not throw in CLI mode
                    throw new AssertionError(e);
                }
            }

            Utilities.print("Unlocking profile...");
            Utilities.flush();
        } catch (IOException e) {
            Utilities.println(" failure: ", e);
            return;
        }

        Utilities.println(" done.");
    }
}
