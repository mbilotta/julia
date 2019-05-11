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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public abstract class CliModel {

	protected final String[] arguments;
	protected Options options;
	protected CommandLine model;

	public CliModel(String[] arguments) {
		this.arguments = arguments;
	}

	public void configure() {
		options = new Options();
	}

	public void parse() throws ParseException {
		CommandLineParser parser = new DefaultParser();
		model = parser.parse(options, arguments);
	}

	public void populate() {
	}

	public CliModel refine() {
		return this;
	}

	public static OptionGroup group(Option o1, Option o2, Option... oN) {
		OptionGroup rv = new OptionGroup();
		rv.addOption(o1).addOption(o2);
		for (Option o : oN) {
			rv.addOption(o);
		}
		return rv;
	}

	public static OptionGroup require(OptionGroup rv) {
		rv.setRequired(true);
		return rv;
	}
}