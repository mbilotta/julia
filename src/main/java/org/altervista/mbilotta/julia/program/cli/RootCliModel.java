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


public class RootCliModel extends CliModel {

	public RootCliModel(String[] arguments) {
		super(arguments);
	}

	public void configure() {
    options = new Options()
      .addOptionGroup(
        group(
          Option.builder("p")
            .longOpt("jup")
            .desc("Create a plugin archive. Add --help to read more about this option.")
            .build(),
          Option.builder("u")
            .longOpt("profile")
            .hasArg()
            .argName("PATH")
            .desc("Run Julia using PATH as profile directory. A new profile will be created if PATH is empty or nonexistent.")
            .build()
        )
      )
      .addOption(
        Option.builder("v")
          .longOpt("version")
          .desc("Print version information and exit.")
          .build()
      )
      .addOption(
        Option.builder("h")
          .longOpt("help")
          .desc("Print this help message and exit.")
          .build()
      );
	}

  public boolean isVersionCli() {
    return model.hasOption("v");
  }

  public boolean isHelpCli() {
    return model.hasOption("h");
  }

  public boolean isJupCli() {
    return model.hasOption("p");
  }

  public String getProfile() {
    return model.getOptionValue("u");
  }

	public void populate() {
	}

	public CliModel refine() {
		return this;
	}
}