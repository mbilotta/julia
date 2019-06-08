/*
 * Copyright (C) 2015 Maurizio Bilotta.
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

import java.nio.file.Path;

import org.altervista.mbilotta.julia.program.Application;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;


@Command(name = "juliafg",
  header = {"Julia: The Fractal Generator", "Copyright (C) 2015 Maurizio Bilotta" },
  version = { "Julia: The Fractal Generator", "Version " + Application.VERSION, "Copyright (C) 2015 Maurizio Bilotta"},
  optionListHeading = "%nOptions:%n",
  sortOptions = false)
public class MainCli {

  @ArgGroup(exclusive = true, multiplicity = "0..1")
  CliMode cliMode = new CliMode();

  static class CliMode {

    @Option(names = { "-j", "--jup" }, required = true, description = "Create a plugin archive. Add --help to read more about this option.")
    boolean jupCreationRequested = false;

    @Option(names = { "-p", "--profile" }, paramLabel = "PATH",
      required = true,
      description = "Run Julia using PATH as profile directory. Use this option to override the default profile (~/.juliafg). A new profile will be created if PATH is empty or nonexistent.")
    Path profilePath;

    @Override
    public String toString() {
      return "[jupCreationRequested=" + jupCreationRequested + ", profilePath=" + profilePath + "]";
    }
  }

  @Option(names = { "-h", "--help" }, usageHelp = true, description = "Print this help message and exit.")
  boolean helpRequested;

  @Option(names = { "-v", "--version" }, versionHelp = true, description = "Print version information and exit.")
  boolean versionRequested;

  public Integer execute(String[] args) {
    CommandLine cmd = new CommandLine(this);
    try {
      cmd.parseArgs(args);

      // Did user request usage help (--help)?
      if (cmd.isUsageHelpRequested()) {
        if (cliMode.jupCreationRequested) {
          return new JupCli().execute(args);
        }
        cmd.usage(cmd.getOut());
        return cmd.getCommandSpec().exitCodeOnUsageHelp();

        // Did user request version help (--version)?
      } else if (cmd.isVersionHelpRequested()) {
        cmd.printVersionHelp(cmd.getOut());
        return cmd.getCommandSpec().exitCodeOnVersionHelp();
      }

      // invoke the business logic
      if (cliMode.jupCreationRequested) {
        return new JupCli().execute(args);
      }
      Application.run(this);
      return null;

      // invalid user input: print error message and usage help
    } catch (ParameterException ex) {
      if (ex instanceof UnmatchedArgumentException) {
        if (cliMode.jupCreationRequested) {
          return new JupCli().execute(args);
        }
      }
      cmd.getErr().println(ex.getMessage());
      if (!UnmatchedArgumentException.printSuggestions(ex, cmd.getErr())) {
        ex.getCommandLine().usage(cmd.getErr());
      }
      return cmd.getCommandSpec().exitCodeOnInvalidInput();

      // exception occurred in business logic
    } catch (Exception ex) {
      ex.printStackTrace(cmd.getErr());
      return cmd.getCommandSpec().exitCodeOnExecutionException();
    }
  }

  public static void main(String[] args) {
    Integer exitCode = new MainCli().execute(args);
    if (exitCode != null) {
      System.exit(exitCode);
    }
  }

  public Path getProfilePath() {
    return cliMode.profilePath;
  }

  @Override
  public String toString() {
    return "[cliMode=" + cliMode + ", helpRequested=" + helpRequested + ", versionRequested=" + versionRequested + "]";
  }
}