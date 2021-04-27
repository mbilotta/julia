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
        LockedFile profileLock = null;
        try {
            Utilities.debug.setEnabled(mainCli.debugOutputEnabled);
            // Lock profile
            Utilities.println("Locking profile...");
            Profile profile = mainCli.getProfilePath() == null ? Profile.getDefaultProfile() : new Profile(mainCli.getProfilePath());
            profileLock = profile.lock();

            // Open log file
            Printer printer = Printer.newPrinter(
                Files.newBufferedWriter(
                        profile.getInstallerOutputFile(),
                        Charset.defaultCharset(),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND
                ),
                false
            );

            // Install plugin
            PluginInstaller installer = profile.new CliPluginInstaller(jupFile, printer);
            installer.install();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (profileLock != null) {
                    profileLock.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
