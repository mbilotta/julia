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

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.nio.file.Path;

import org.altervista.mbilotta.julia.ApplicationInfo;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.Application;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;


@Command(name = "juliac",
	subcommands = { PluginInstallationCli.class, ImageGenerationCli.class, PluginPackagementCli.class, HelpCommand.class },
	header = { "Julia: The Fractal Generator", "Copyright (C) 2015 Maurizio Bilotta" },
	version = { "Julia: The Fractal Generator", "Version " + ApplicationInfo.VERSION, "Copyright (C) 2015 Maurizio Bilotta"},
	optionListHeading = "%nOptions:%n",
	commandListHeading = "%nCommands:%n",
	sortOptions = false)
public class MainCli implements Runnable {

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

	@Override
	public void run() {
		Utilities.debug.setEnabled(debugOutputEnabled);
		if (GraphicsEnvironment.isHeadless()) {
			throw new HeadlessException();
		} else {
			guiRunning = true;
			Application.run(this);
		}
	}

	public static void main(String[] args) {
		CommandLine cmd = new CommandLine(new MainCli());
		cmd.setExecutionExceptionHandler(new IExecutionExceptionHandler() {
			@Override
			public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult)
					throws Exception {
				if (ex instanceof HeadlessException) {
					commandLine.getErr().println(ex);
					commandLine.usage(commandLine.getOut());
					return ExitCode.SOFTWARE;
				}
				throw ex;
			}
		});

		int exitCode = cmd.execute(args);
		if (exitCode != 0) {
			System.exit(exitCode);
		}
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