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

package org.altervista.mbilotta.julia.program.parsers;

import static org.altervista.mbilotta.julia.Utilities.append;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.Buffer;
import org.altervista.mbilotta.julia.program.LockedFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


public abstract class Parser<T> {
	
	private final DocumentBuilder documentBuilder;
	
	private Path currentFile;
	private int warningCount = 0;
	private int errorCount = 0;
	private int fatalErrorCount = 0;
	private List<Problem> problems = Collections.emptyList();
	private boolean domValidationProblemsEncountered = false;

	private SAXParser saxParser;
	private boolean saxParserInstantiationFailed = false;

	protected Parser(String xmlSchemaName) throws SAXException, ParserConfigurationException {

		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(getClass().getResource(xmlSchemaName));
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setIgnoringComments(true);
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setSchema(schema);

		documentBuilder = dbf.newDocumentBuilder();
		documentBuilder.setErrorHandler(new ErrorHandler() {
			public void warning(SAXParseException exception) throws SAXException {
				warningCount++;
				problems.add(Problem.warning(relativize(currentFile), exception));
			}
			public void fatalError(SAXParseException exception) throws SAXException {
				fatalErrorCount++;
				problems.add(Problem.fatalError(relativize(currentFile), exception));
			}
			public void error(SAXParseException exception) throws SAXException {
				fatalErrorCount++;
				problems.add(Problem.fatalError(relativize(currentFile), exception));
			}
		});
	}

	protected void reset() {
		problems = new LinkedList<>();
		domValidationProblemsEncountered = false;
		warningCount = 0;
		errorCount = 0;
		fatalErrorCount = 0;
	}

	protected abstract T validate(Document dom) throws DomValidationException, ClassValidationException, InterruptedException;

	public T parse(LockedFile file, StringBuilder outputBuilder)
			throws SAXException, IOException, DomValidationException, ClassValidationException, InterruptedException {
		return parse(Buffer.readFully(file, null, null), file.getPath(), outputBuilder);
	}

	public T parse(Buffer bytes, Path source, StringBuilder outputBuilder)
			throws SAXException, IOException, DomValidationException, ClassValidationException, InterruptedException {
		currentFile = source;
		reset();
		Document dom = documentBuilder.parse(bytes.toInputStream(), source.toUri().toASCIIString());
		
		try {
			if (fatalErrorCount > 0) return null;
			T rv = validate(dom);
			return fatalErrorCount > 0 ? null : rv;
		} finally {
			appendProblems(bytes, outputBuilder);
		}
	}

	private void locateProblem(Buffer bytes, Problem problem) {
		if (problem instanceof DomValidationProblem) {
			DefaultHandler vpl =
					new DomValidationProblemLocator((DomValidationProblem) problem);
			try {
				saxParser.parse(bytes.toInputStream(), vpl);
			} catch (SAXException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void locateAndAppendProblems(Buffer bytes, StringBuilder outputBuilder) {
		if (outputBuilder != null) {
			for (Problem problem : problems) {
				locateProblem(bytes, problem);
				problem.appendTo(outputBuilder);
			}
		} else {
			for (Problem problem : problems) {
				locateProblem(bytes, problem);
			}
		}
	}

	private void appendProblems(StringBuilder outputBuilder) {
		if (outputBuilder != null) {
			for (Problem problem : problems) {
				problem.appendTo(outputBuilder);
			}
		}
	}

	private void appendProblems(Buffer bytes, StringBuilder outputBuilder) {
		if (!saxParserInstantiationFailed &&
				domValidationProblemsEncountered) {
			if (saxParser == null) {
				SAXParserFactory spf = SAXParserFactory.newInstance();
				spf.setNamespaceAware(true);

				try {
					saxParser = spf.newSAXParser();
					locateAndAppendProblems(bytes, outputBuilder);
				} catch (SAXException | ParserConfigurationException e) {
					saxParserInstantiationFailed = true;
					e.printStackTrace();
					appendProblems(outputBuilder);
				}
			} else {
				locateAndAppendProblems(bytes, outputBuilder);
			}
		} else {
			appendProblems(outputBuilder);
		}
	}

	public int getWarningCount() {
		return warningCount;
	}

	public int getErrorCount() {
		return errorCount;
	}

	public int getFatalErrorCount() {
		return fatalErrorCount;
	}

	public Path getCurrentFile() {
		return currentFile;
	}

	public List<Problem> getProblems() {
		return Collections.unmodifiableList(problems);
	}

	protected void warning(DomValidationException exception) throws DomValidationException {
		warningCount++;
		domValidationProblemsEncountered = true;
		problems.add(Problem.warning(relativize(currentFile), exception));
	}

	protected void fatalError(DomValidationException exception) throws DomValidationException {
		fatalErrorCount++;
		domValidationProblemsEncountered = true;
		problems.add(Problem.fatalError(relativize(currentFile), exception));
	}

	protected void error(DomValidationException exception) throws DomValidationException {
		errorCount++;
		domValidationProblemsEncountered = true;
		problems.add(Problem.error(relativize(currentFile), exception));
	}

	protected void warning(ClassValidationException exception) throws ClassValidationException {
		warningCount++;
		problems.add(Problem.warning(exception));
	}

	protected void fatalError(ClassValidationException exception) throws ClassValidationException {
		fatalErrorCount++;
		problems.add(Problem.fatalError(exception));
	}

	protected void error(ClassValidationException exception) throws ClassValidationException {
		errorCount++;
		problems.add(Problem.error(exception));
	}

	protected Path relativize(Path path) {
		return path;
	}

	public static void println(XmlPath path, Object value) {
		Utilities.println(path, " = ", value);
	}

	public static String getNodeValue(Node node) {
		Node child = node.getFirstChild();
		if (child == node.getLastChild())
			return child.getNodeValue();

		StringBuilder sb = new StringBuilder();
		for ( ; child != null; child = child.getNextSibling()) {
			sb.append(child.getNodeValue());
		}
		
		return sb.toString();
	}

	public static abstract class Problem {
		public static final int WARNING = 0;
		public static final int ERROR = 1;
		public static final int FATAL_ERROR = 2;

		private final int type;
		private final Path file;
		private final Exception exception;

		private Problem(int type, Path file, Exception exception) {
			this.type = type;
			this.file = file;
			this.exception = exception;
		}

		public int getType() {
			return type;
		}

		public Path getFile() {
			return file;
		}

		public Exception getException() {
			return exception;
		}

		public abstract int getLineNumber();

		public abstract int getColumnNumber();

		public String toString() {
			return getClass().getSimpleName() +
					"[type=" + toTypeString(type) +
					", file=" + file +
					", exception=" + exception + "]";
		}

		public static Problem warning(Path file, SAXParseException exception) {
			return new ParsingProblem(WARNING, file, exception);
		}

		public static Problem error(Path file, SAXParseException exception) {
			return new ParsingProblem(ERROR, file, exception);
		}

		public static Problem fatalError(Path file, SAXParseException exception) {
			return new ParsingProblem(FATAL_ERROR, file, exception);
		}

		public static Problem warning(Path file, DomValidationException exception) {
			return new DomValidationProblem(WARNING, file, exception);
		}

		public static Problem error(Path file, DomValidationException exception) {
			return new DomValidationProblem(ERROR, file, exception);
		}

		public static Problem fatalError(Path file, DomValidationException exception) {
			return new DomValidationProblem(FATAL_ERROR, file, exception);
		}

		public static Problem warning(ClassValidationException exception) {
			return new ClassValidationProblem(WARNING, exception);
		}

		public static Problem error(ClassValidationException exception) {
			return new ClassValidationProblem(ERROR, exception);
		}

		public static Problem fatalError(ClassValidationException exception) {
			return new ClassValidationProblem(FATAL_ERROR, exception);
		}

		public static String toTypeString(int type) {
			switch (type) {
			case WARNING: return "WARNING";
			case ERROR: return "ERROR";
			case FATAL_ERROR: return "FATAL_ERROR";
			default: throw new IllegalArgumentException("" + type);
			}
		}
	
		public String getCauseString() {
			Exception e = getException();
			Throwable cause = e.getCause();
			String rv;
			if (cause == null) {
				rv = "";
			} else {
				StringWriter sw = new StringWriter();
				cause.printStackTrace(new PrintWriter(sw));
				rv = sw.toString();
			}
			return rv;
		}

		public void appendTo(StringBuilder sb) {
			append(sb, "[", Problem.toTypeString(getType()), "] ",
					getFile(), ":",
					getLineNumber(), ":",
					getColumnNumber(), ": ",
					getException().getMessage(), "\n", getCauseString());
		}
	}

	public static final class ParsingProblem extends Problem {
		private ParsingProblem(int type, Path file, SAXParseException exception) {
			super(type, file, exception);
		}

		public SAXParseException getException() {
			return (SAXParseException) super.getException();
		}

		public int getLineNumber() {
			return getException().getLineNumber();
		}

		public int getColumnNumber() {
			return getException().getColumnNumber();
		}
	}

	public static final class DomValidationProblem extends Problem {
		private int lineNumber = -1;
		private int columnNumber = -1;

		private DomValidationProblem(int type, Path file, DomValidationException exception) {
			super(type, file, exception);
		}

		public DomValidationException getException() {
			return (DomValidationException) super.getException();
		}

		public int getLineNumber() {
			return lineNumber;
		}

		public int getColumnNumber() {
			return columnNumber;
		}

		public int getPosition() {
			return getException().getPosition();
		}

		public XmlPath getElementPath() {
			return getException().getElementPath();
		}

		@Override
		public void appendTo(StringBuilder sb) {
			if (getLineNumber() == -1 || getColumnNumber() == -1) {
				append(sb, "[", Problem.toTypeString(getType()), "] ",
						getFile(), ":",
						getElementPath(), ": ",
						getException().getMessage(), "\n", getCauseString());
			} else {
				super.appendTo(sb);
			}
		}
	}

	public static final class DomValidationProblemLocator extends DefaultHandler {
		private final ListIterator<ChildSelector> pathIterator;
		private ChildSelector currentSelector;
		private final int position;
		private boolean startFound = false;
		private boolean endFound = false;
		private int currentIndex = 1;
		private int parserDepth = 0;
		private int pathDepth = 0;
		private DomValidationProblem problem;
		private Locator locator;

		public DomValidationProblemLocator(DomValidationProblem problem) {
			this.pathIterator = problem.getElementPath().iterator();
			this.currentSelector = pathIterator.next();
			this.position = problem.getPosition();
			this.problem = problem;
		}

		@Override
		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			if (!startFound && parserDepth == pathDepth && localName.equals(currentSelector.getLocalName())) {
				String attributeName = currentSelector.getAttributeName();
				if (attributeName != null) {
					String attributeValue = currentSelector.getAttributeValue();
					if (attributes.getValue(attributeName).equals(attributeValue))
						forward();
				} else if (currentSelector.getIndex() == 0) {
					forward();
				} else if (currentSelector.getIndex() == currentIndex) {
					forward();
					currentIndex = 1;
				} else {
					currentIndex++;
				}
			}
			parserDepth++;
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			parserDepth--;
			if (!endFound) {
				if (startFound && parserDepth == pathDepth && localName.equals(currentSelector.getLocalName())) {
					endFound = true;
					if (position == DomValidationException.END_OF_ELEMENT) {
						problem.lineNumber = locator.getLineNumber();
						problem.columnNumber = locator.getColumnNumber();
					}
				} else if (parserDepth == pathDepth - 1) {
					back();
				}
			}
		}

		private void forward() {
			if (pathIterator.hasNext()) {
				pathDepth++;
				ChildSelector next = pathIterator.next();
				if (next == currentSelector)
					next = pathIterator.next();

				currentSelector = next;
			} else {
				startFound = true;
				if (position == DomValidationException.START_OF_ELEMENT) {
					problem.lineNumber = locator.getLineNumber();
					problem.columnNumber = locator.getColumnNumber();
				}
			}
		}

		private void back() {
			pathDepth--;
			ChildSelector previous = pathIterator.previous();
			if (previous == currentSelector)
				previous = pathIterator.previous();

			currentSelector = previous;
			currentIndex = 1;
		}
	}

	public static final class ClassValidationProblem extends Problem {

		private ClassValidationProblem(int type, ClassValidationException exception) {
			super(type, null, exception);
		}

		@Override
		public int getLineNumber() {
			return -1;
		}

		@Override
		public int getColumnNumber() {
			return -1;
		}

		@Override
		public ClassValidationException getException() {
			return (ClassValidationException) super.getException();
		}

		public String getClassName() {
			return this.getException().getClassName();
		}

		public String getPropertyName() {
			return this.getException().getPropertyName();
		}

		@Override
		public void appendTo(StringBuilder sb) {
			append(sb, "[", Problem.toTypeString(getType()), "] ",
					getClassName(), ".", getPropertyName(), ": ",
					getException().getMessage(), "\n", getCauseString());
		}
	}
}
