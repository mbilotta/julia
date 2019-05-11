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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


public class JupCliModel extends CliModel {

  public JupCliModel() {
    super();
  }

	public JupCliModel(String[] arguments) {
		super(arguments);
  }

  public JupCliModel(CliModel model) {
    super(model);
  }

	public void configure() {
    options = new Options()
      .addOption(
        Option.builder("p")
          .longOpt("jup")
          .hasArg()
          .optionalArg(true)
          .argName("INSTALLATION_PATH")
          .required()
          .desc("Plugin files will be installed in <PROFILE_PATH>/(xml|bin|doc)/<INSTALLATION_PATH> respectively.")
          .build()
      )
      .addOption(
        Option.builder("b")
          .longOpt("bin")
          .hasArg()
          .argName("PATH")
          .required()
          .desc("JAR file or directory containing one or more JARs.")
          .build()
      )
      .addOption(
        Option.builder("x")
          .longOpt("xml")
          .hasArg()
          .argName("PATH")
          .required()
          .desc("Descriptor file (must end with .xml) or directory containing one or more descriptors.")
          .build()
      )
      .addOptionGroup(
        group(
          Option.builder("d")
            .longOpt("doc")
            .hasArg()
            .argName("PATH")
            .desc("Documentation resource (CSS, font, image, JavaScript, etc.) or directory containing one or more such resources.")
            .build(),
          Option.builder("D")
            .longOpt("doc-tree")
            .hasArg()
            .argName("PATH")
            .desc("Same as --doc but if PATH is a directory, add subdirectory contents recursively.")
            .build()
        )
      )
      .addOption(
        Option.builder("l")
          .longOpt("license")
          .hasArg()
          .argName("PATH")
          .desc("License file.")
          .build()
      )
      .addOption(
        Option.builder("o")
          .longOpt("out")
          .hasArg()
          .argName("PATH")
          .desc("Output path.")
          .build()
      )
      .addOption(
        Option.builder("h")
          .longOpt("help")
          .desc("Print this help message and exit.")
          .build()
      );
	}

  public String getInstallationPath() {
    return model.getOptionValue("p", "");
  }

  public String getBinPath() {
    return model.getOptionValue("b");
  }

  public String getXmlPath() {
    return model.getOptionValue("x");
  }

  public String getDocPath() {
    return model.getOptionValue("d");
  }

  public String getDocTreePath() {
    return model.getOptionValue("D");
  }

  public String getLicensePath() {
    return model.getOptionValue("l");
  }

  public String getOutputPath() {
    return model.getOptionValue("o");
  }
}