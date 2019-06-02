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
import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@Command(name = "juliafg", 
  header = { "Julia: The Fractal Generator", "Copyright (C) 2015 Maurizio Bilotta" },
  description = "Create a JUP archive containing one or more Julia plugins.",
  descriptionHeading = "%n",
  parameterListHeading = "%nParameters:%n",
  optionListHeading = "%nOptions:%n",
  sortOptions = false)
public class JupCli implements Runnable {

  @Option(names = { "-j", "--jup" },
    required = true,
    description = "Enable JUP command line mode.")
  boolean jupCreationRequested;

  @Option(names = { "-l", "--license" }, paramLabel = "FILE",
    description = "License file for this JUP archive.")
  Path licensePath;

  @Option(names = { "-o", "--out" }, paramLabel = "OUTPUT_PATH",
    description = "Output file path. If this option is absent, a .jup file named as the last segment of PLUGIN_PATH will be created in the current directory. If OUTPUT_PATH is a directory, such file will be created there. If OUTPUT_PATH ends with \".jup\", OUTPUT_PATH will be the output file.")
  Path outputPath;

  @Option(names = { "-h", "--help" },
    usageHelp = true,
    description = "Print this help message and exit.")
  boolean helpRequested;

  @Parameters(index = "0",
    arity = "1",
    paramLabel = "PLUGIN_PATH",
    description = "Relative path that will be created inside the /xml, /bin, /doc subdirectories of the target profile when this plugin is installed.")
  String pluginPath;

  @Parameters(index = "1..*", arity = "1", paramLabel = "INPUT_PATH",
    description = "Each INPUT_PATH can be a file or a directory. Files will be added to the right entry in the archive based on their extension: plugin descriptors must end with       \".xml\"; JARs must end with \".jar\"; files that are none of the two will be treated as documentation resources. When INPUT_PATH is a directory, its contents will be added to the archive following the same logic. Subdirectories will be ignored.")
  List<Path> inputPaths;

  @Override
  public String toString() {
    return "[" +
      "pluginPath=" + pluginPath +
      ", licensePath=" + licensePath +
      ", outputPath=" + outputPath +
      ", helpRequested=" + helpRequested +
      ", inputPaths=" + inputPaths +
      "]";
  }

  public int execute(String[] args) {
    return new CommandLine(this).execute(args);
  }

  @Override
  public void run() {
    System.out.println(this);
  }
}