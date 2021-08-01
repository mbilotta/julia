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

package org.altervista.mbilotta.julia.program;

import java.awt.Color;
import java.awt.Transparency;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Properties;



public class Preferences implements Serializable, Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int imageWidth;
	private int imageHeight;
	private int transparency;
	private int refreshDelay;
	private int numOfProducerThreads;
	private boolean loggingEnabled;
	private int maxLogLength;
	private Color selectionColor;

	private DefaultCloseBehaviour defaultCloseBehaviour;
	private boolean javaDesktopInteractionEnabled;
	private String browserCommand;

	private boolean startupCombinationEnabled;
	private String startupNumberFactory;
	private String startupFormula;
	private String startupRepresentation;
	private boolean startupJuliaSetFlag;
	private boolean startupForceEqualScalesFlag;
	private String startupImagePath;

	public Preferences() {
		imageWidth = 624;
		imageHeight = 575;
		transparency = Transparency.TRANSLUCENT;
		refreshDelay = 500;
		numOfProducerThreads = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
		loggingEnabled = false;
		maxLogLength = 5000;
		selectionColor = Color.BLUE;
		defaultCloseBehaviour = DefaultCloseBehaviour.ASK;
		javaDesktopInteractionEnabled = true;
		browserCommand = null;
		startupCombinationEnabled = true;
		String prefix = "org/altervista/mbilotta/";
		startupNumberFactory = prefix + "Double";
		startupFormula = prefix + "Quadratic";
		startupRepresentation = prefix + "EscapeTime";
		startupJuliaSetFlag = false;
		startupForceEqualScalesFlag = true;
		startupImagePath = null;
	}

	public Preferences(int imageWidth, int imageHeight, int transparency,
			int refreshDelay, int numOfProducerThreads,
			boolean loggingEnabled, int maxLogLength, Color selectionColor,
			DefaultCloseBehaviour defaultDocumentCloseOperation,
			boolean javaDesktopInteractionEnabled, String browserCommand,
			boolean startupCombinationEnabled, String startupNumberFactory,
			String startupFormula, String startupRepresentation,
			boolean startupJuliaSetFlag,
			boolean startupForceEqualScalesFlag,
			String startupImagePath) {
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.transparency = transparency;
		this.numOfProducerThreads = numOfProducerThreads;
		this.refreshDelay = refreshDelay;
		this.loggingEnabled = loggingEnabled;
		this.maxLogLength = maxLogLength;
		this.selectionColor = selectionColor;
		this.defaultCloseBehaviour = defaultDocumentCloseOperation;
		this.javaDesktopInteractionEnabled = javaDesktopInteractionEnabled;
		this.browserCommand = browserCommand;
		this.startupCombinationEnabled = startupCombinationEnabled;
		this.startupNumberFactory = startupNumberFactory;
		this.startupFormula = startupFormula;
		this.startupRepresentation = startupRepresentation;
		this.startupJuliaSetFlag = startupJuliaSetFlag;
		this.startupForceEqualScalesFlag = startupForceEqualScalesFlag;
		this.startupImagePath = startupImagePath;
	}

	@Override
	public Preferences clone() {
		try {
			return (Preferences) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	public String getBrowserCommand() {
		return browserCommand;
	}

	public DefaultCloseBehaviour getDefaultCloseBehaviour() {
		return defaultCloseBehaviour;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public int getImageWidth() {
		return imageWidth;
	}

	public int getMaxLogLength() {
		return maxLogLength;
	}

	public int getNumOfProducerThreads() {
		return numOfProducerThreads;
	}

	public int getRefreshDelay() {
		return refreshDelay;
	}

	public Color getSelectionColor() {
		return selectionColor;
	}

	public boolean getStartupForceEqualScalesFlag() {
		return startupForceEqualScalesFlag;
	}

	public String getStartupFormula() {
		return startupFormula;
	}

	public String getStartupImagePath() {
		return startupImagePath;
	}

	public boolean getStartupJuliaSetFlag() {
		return startupJuliaSetFlag;
	}

	public String getStartupNumberFactory() {
		return startupNumberFactory;
	}

	public String getStartupRepresentation() {
		return startupRepresentation;
	}

	public int getTransparency() {
		return transparency;
	}

	public boolean isJavaDesktopInteractionEnabled() {
		return javaDesktopInteractionEnabled;
	}

	public boolean isLoggingEnabled() {
		return loggingEnabled;
	}

	public boolean isStartupCombinationEnabled() {
		return startupCombinationEnabled;
	}

	public void writeTo(Properties p) {
		p.setProperty("imageWidth", Integer.toString(imageWidth));
		p.setProperty("imageHeight", Integer.toString(imageHeight));
		p.setProperty("transparency", convertTransparencyToString(transparency));
		p.setProperty("refreshDelay", Integer.toString(refreshDelay));
		p.setProperty("numOfProducerThreads", Integer.toString(numOfProducerThreads));
		p.setProperty("loggingEnabled", Boolean.toString(loggingEnabled));
		p.setProperty("maxLogLength", Integer.toString(maxLogLength));
		p.setProperty("selectionColor", convertColorToString(selectionColor));
		p.setProperty("defaultCloseBehaviour", defaultCloseBehaviour.name());
		p.setProperty("javaDesktopInteractionEnabled", Boolean.toString(javaDesktopInteractionEnabled));
		if (browserCommand != null) {
			p.setProperty("browserCommand", browserCommand);
		} else {
			p.setProperty("browserCommand", "");
		}
		p.setProperty("startupCombinationEnabled", Boolean.toString(startupCombinationEnabled));
		if (startupNumberFactory != null) {
			p.setProperty("startupNumberFactory", startupNumberFactory);
		} else {
			p.setProperty("startupNumberFactory", "");
		}
		if (startupFormula != null) {
			p.setProperty("startupFormula", startupFormula);
		} else {
			p.setProperty("startupFormula", "");
		}
		if (startupRepresentation != null) {
			p.setProperty("startupRepresentation", startupRepresentation);
		} else {
			p.setProperty("startupRepresentation", "");
		}
		p.setProperty("startupJuliaSetFlag", Boolean.toString(startupJuliaSetFlag));
		p.setProperty("startupForceEqualScalesFlag", Boolean.toString(startupForceEqualScalesFlag));
		if (startupImagePath != null) {
			p.setProperty("startupImagePath", startupImagePath);
		} else {
			p.setProperty("startupImagePath", "");
		}
	}

	public void readFrom(Properties p) {
		String imageWidth = p.getProperty("imageWidth");
		if (imageWidth != null) {
			setImageWidth(Integer.parseInt(imageWidth.trim()));
		}
		String imageHeight = p.getProperty("imageHeight");
		if (imageHeight != null) {
			setImageHeight(Integer.parseInt(imageHeight.trim()));
		}
		String transparency = p.getProperty("transparency");
		if (transparency != null) {
			setTransparency(convertStringToTransparency(transparency.trim()));
		}
		String refreshDelay = p.getProperty("refreshDelay");
		if (refreshDelay != null) {
			setRefreshDelay(Integer.parseInt(refreshDelay.trim()));
		}
		String numOfProducerThreads = p.getProperty("numOfProducerThreads");
		if (numOfProducerThreads != null) {
			setNumOfProducerThreads(Integer.parseInt(numOfProducerThreads.trim()));
		}
		String loggingEnabled = p.getProperty("loggingEnabled");
		if (loggingEnabled != null) {
			setLoggingEnabled(Boolean.parseBoolean(loggingEnabled.trim()));
		}
		String maxLogLength = p.getProperty("maxLogLength");
		if (maxLogLength != null) {
			setMaxLogLength(Integer.parseInt(maxLogLength.trim()));
		}
		String selectionColor = p.getProperty("selectionColor");
		if (selectionColor != null) {
			setSelectionColor(convertStringToColor(selectionColor.trim()));
		}
		String defaultCloseBehaviour = p.getProperty("defaultCloseBehaviour");
		if (defaultCloseBehaviour != null) {
			setDefaultCloseBehaviour(DefaultCloseBehaviour.valueOf(defaultCloseBehaviour.trim()));
		}
		String javaDesktopInteractionEnabled = p.getProperty("javaDesktopInteractionEnabled");
		if (javaDesktopInteractionEnabled != null) {
			setJavaDesktopInteractionEnabled(Boolean.parseBoolean(javaDesktopInteractionEnabled.trim()));
		}
		String browserCommand = p.getProperty("browserCommand");
		if (browserCommand != null) {
			browserCommand = browserCommand.trim();
			setBrowserCommand(browserCommand.isEmpty() ? null : browserCommand);
		}
		String startupCombinationEnabled = p.getProperty("startupCombinationEnabled");
		if (startupCombinationEnabled != null) {
			setStartupCombinationEnabled(Boolean.parseBoolean(startupCombinationEnabled.trim()));
		}
		String startupNumberFactory = p.getProperty("startupNumberFactory");
		if (startupNumberFactory != null) {
			startupNumberFactory = startupNumberFactory.trim();
			setStartupNumberFactory(startupNumberFactory.isEmpty() ? null : startupNumberFactory);
		}
		String startupFormula = p.getProperty("startupFormula");
		if (startupFormula != null) {
			startupFormula = startupFormula.trim();
			setStartupFormula(startupFormula.isEmpty() ? null : startupFormula);
		}
		String startupRepresentation = p.getProperty("startupRepresentation");
		if (startupRepresentation != null) {
			startupRepresentation = startupRepresentation.trim();
			setStartupRepresentation(startupRepresentation.isEmpty() ? null : startupRepresentation);
		}
		String startupJuliaSetFlag = p.getProperty("startupJuliaSetFlag");
		if (startupJuliaSetFlag != null) {
			setStartupJuliaSetFlag(Boolean.parseBoolean(startupJuliaSetFlag.trim()));
		}
		String startupForceEqualScalesFlag = p.getProperty("startupForceEqualScalesFlag");
		if (startupForceEqualScalesFlag != null) {
			setStartupForceEqualScalesFlag(Boolean.parseBoolean(startupForceEqualScalesFlag.trim()));
		}
		String startupImagePath = p.getProperty("startupImagePath");
		if (startupImagePath != null) {
			startupImagePath = startupImagePath.trim();
			setStartupImagePath(startupImagePath.isEmpty() ? null : startupImagePath);
		}
	}

	static String convertTransparencyToString(int transparency) {
		switch (transparency) {
			case Transparency.TRANSLUCENT: return "TRANSLUCENT";
			case Transparency.BITMASK: return "BITMASK";
			case Transparency.OPAQUE: return "OPAQUE";
			default: throw new IllegalArgumentException(Integer.toString(transparency));
		}
	}

	static int convertStringToTransparency(String transparency) {
		switch (transparency) {
			case "TRANSLUCENT": return Transparency.TRANSLUCENT;
			case "BITMASK": return Transparency.BITMASK;
			case "OPAQUE": return Transparency.OPAQUE;
			default: throw new IllegalArgumentException(transparency);
		}
	}

	static String convertColorToString(Color color) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		if (color.getAlpha() == 255) {
			return r + "," + g + "," + b;
		}
		int a = color.getAlpha();
		return r + "," + g + "," + b + "," + a;
	}

	static Color convertStringToColor(String color) {
		String[] components = color.split(",", 4);
		if (components.length < 3) {
			throw new IllegalArgumentException(color);
		}
		int r = Integer.parseInt(components[0]);
		int g = Integer.parseInt(components[1]);
		int b = Integer.parseInt(components[2]);
		int a = components.length > 3 ? Integer.parseInt(components[3]) : 255;
		return new Color(r, g, b, a);
	}

	private void readObject(ObjectInputStream in)
			throws ClassNotFoundException, IOException {
		in.defaultReadObject();

		if (imageWidth < 1 || imageHeight < 1 || refreshDelay < 1 || numOfProducerThreads < 1 ||
				defaultCloseBehaviour == null || selectionColor == null ||
				transparency < 1 || transparency > 3 ||
				maxLogLength < 1)
			throw new InvalidObjectException(toString());
	}

	void setBrowserCommand(String browserCommand) {
		this.browserCommand = browserCommand;
	}

	void setDefaultCloseBehaviour(DefaultCloseBehaviour defaultCloseBehaviour) {
		this.defaultCloseBehaviour = defaultCloseBehaviour;
	}

	void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
	}

	void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
	}

	void setJavaDesktopInteractionEnabled(boolean javaDesktopInteractionEnabled) {
		this.javaDesktopInteractionEnabled = javaDesktopInteractionEnabled;
	}

	void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
	}

	void setMaxLogLength(int maxLogLength) {
		this.maxLogLength = maxLogLength;
	}

	void setNumOfProducerThreads(int numOfProducerThreads) {
		this.numOfProducerThreads = numOfProducerThreads;
	}

	void setRefreshDelay(int refreshDelay) {
		this.refreshDelay = refreshDelay;
	}

	void setSelectionColor(Color selectionColor) {
		this.selectionColor = selectionColor;
	}

	void setStartupCombinationEnabled(boolean startupCombinationEnabled) {
		this.startupCombinationEnabled = startupCombinationEnabled;
	}

	void setStartupForceEqualScalesFlag(boolean startupForceEqualScalesFlag) {
		this.startupForceEqualScalesFlag = startupForceEqualScalesFlag;
	}

	void setStartupFormula(String startupFormula) {
		this.startupFormula = startupFormula;
	}

	void setStartupImagePath(String startupImagePath) {
		this.startupImagePath = startupImagePath;
	}

	void setStartupJuliaSetFlag(boolean startupJuliaSetFlag) {
		this.startupJuliaSetFlag = startupJuliaSetFlag;
	}

	void setStartupNumberFactory(String startupNumberFactory) {
		this.startupNumberFactory = startupNumberFactory;
	}

	void setStartupRepresentation(String startupRepresentation) {
		this.startupRepresentation = startupRepresentation;
	}

	void setTransparency(int transparency) {
		this.transparency = transparency;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getCanonicalName());
		builder.append("[imageWidth=");
		builder.append(imageWidth);
		builder.append(", imageHeight=");
		builder.append(imageHeight);
		builder.append(", transparency=");
		builder.append(transparency);
		builder.append(", refreshDelay=");
		builder.append(refreshDelay);
		builder.append(", numOfProducerThreads=");
		builder.append(numOfProducerThreads);
		builder.append(", loggingEnabled=");
		builder.append(loggingEnabled);
		builder.append(", maxLogLength=");
		builder.append(maxLogLength);
		builder.append(", selectionColor=");
		builder.append(selectionColor);
		builder.append(", defaultDocumentCloseOperation=");
		builder.append(defaultCloseBehaviour);
		builder.append(", javaDesktopInteractionEnabled=");
		builder.append(javaDesktopInteractionEnabled);
		builder.append(", browserCommand=");
		builder.append(browserCommand);
		builder.append(", startupCombinationEnabled=");
		builder.append(startupCombinationEnabled);
		builder.append(", startupNumberFactory=");
		builder.append(startupNumberFactory);
		builder.append(", startupFormula=");
		builder.append(startupFormula);
		builder.append(", startupRepresentation=");
		builder.append(startupRepresentation);
		builder.append(", startupJuliaSetFlag=");
		builder.append(startupJuliaSetFlag);
		builder.append(", startupForceEqualScalesFlag=");
		builder.append(startupForceEqualScalesFlag);
		builder.append(", startupImagePath=");
		builder.append(startupImagePath);
		builder.append("]");
		return builder.toString();
	}
}
