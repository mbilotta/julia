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

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;

import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.Application;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Option;


@Command(name = "juliac",
	subcommands = { PluginInstallationCli.class, ImageGenerationCli.class, PluginPackagementCli.class, HelpCommand.class },
	header = { "Julia: The Fractal Generator", "Copyright (C) 2015 Maurizio Bilotta" },
	version = { "Julia: The Fractal Generator", "Version " + Application.VERSION, "Copyright (C) 2015 Maurizio Bilotta"},
	optionListHeading = "%nOptions:%n",
	commandListHeading = "%nCommands:%n",
	sortOptions = false)
public class MainCli implements Runnable, IExitCodeGenerator {

	@Option(names = { "-d", "--debug" },
		description = "Enable debug console output")
	boolean debugOutputEnabled;

	@Option(names = { "-p", "--profile" }, paramLabel = "PATH",
		description = "Run Julia using PATH as profile directory. Use this option to override the default profile (~/.juliafg). A new profile will be created if PATH is empty or nonexistent.")
	Path profilePath;

	@Option(names = "--refresh-cache", description = "Force regeneration of cache and documentation for every installed plugin.")
	boolean cacheRefreshRequested;

	@Option(names = { "-v", "--version" }, versionHelp = true,
		description = "Print version information and exit.")
	boolean versionRequested;

	private boolean guiRunning = false;

	private int exitCode = 0;

	static final int HEADLESS_ERROR_EXIT_CODE = 123;

	@Override
	public void run() {
		guiRunning = true;
		Utilities.debug.setEnabled(debugOutputEnabled);
		if (GraphicsEnvironment.isHeadless()) {
			exitCode = HEADLESS_ERROR_EXIT_CODE;
		} else {
			Application.run(this);
		}
	}

	public static void main(String[] args) {
		CommandLine cmd = new CommandLine(new MainCli());
		int exitCode = cmd.execute(args);
		if (exitCode != 0) {
			if (exitCode == HEADLESS_ERROR_EXIT_CODE) {
				cmd.usage(System.out);
			}
			System.exit(exitCode);
		}
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	public Path getProfilePath() {
		return profilePath;
	}

	public boolean isCacheRefreshRequested() {
		return cacheRefreshRequested;
	}

	public boolean isGuiRunning() {
		return guiRunning;
	}

	@Override
	public String toString() {
		return "[cacheRefreshRequested=" + cacheRefreshRequested + ", versionRequested=" + versionRequested + "]";
	}
}