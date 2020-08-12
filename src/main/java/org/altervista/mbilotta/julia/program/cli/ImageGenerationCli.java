package org.altervista.mbilotta.julia.program.cli;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.altervista.mbilotta.julia.Decimal;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.JuliaExecutorService;
import org.altervista.mbilotta.julia.program.JuliaSetPoint;
import org.altervista.mbilotta.julia.program.Loader;
import org.altervista.mbilotta.julia.program.PluginInstance;
import org.altervista.mbilotta.julia.program.Preferences;
import org.altervista.mbilotta.julia.program.Rectangle;
import org.altervista.mbilotta.julia.program.parsers.DescriptorParser;
import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
import org.altervista.mbilotta.julia.program.parsers.Plugin;
import org.altervista.mbilotta.julia.program.parsers.RepresentationPlugin;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "juliac",
    header = { "Julia: The Fractal Generator", "Copyright (C) 2015 Maurizio Bilotta" },
    description = "Generate a fractal.",
    descriptionHeading = "%n",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    sortOptions = false)
public class ImageGenerationCli implements Runnable {
    
    final MainCli mainCli;
    
    @Option(names = { "-g", "--generate" },
        required = true,
        description = "Needed to enable image generation CLI mode.")
    boolean imageGenerationRequested;

    @Option(names = { "-W", "--width" })
    Integer width;

    @Option(names = { "-H", "--height" })
    Integer height;

    @Option(names = { "-q", "--force-equal-scales" })
    boolean forceEqualScales;

    @Option(names = { "-n", "--number-factory" })
    String numberFactoryId;

    @Option(names = { "-f", "--formula" })
    String formulaId;

    @Option(names = { "-r", "--representation" })
    String representationId;

    @Option(names = { "-o", "--output" }, paramLabel = "OUTPUT_PATH", required = true,
        description = "Output file path.")
    Path outputPath;

    @Option(names = { "-x", "--replace-existing" },
        description = "Use this flag to eventually replace an already existing file at output path.")
    boolean replaceExisting;

    @Option(names = { "-h", "--help" },
        usageHelp = true,
        description = "Print this help message and exit.")
    boolean helpRequested;

    @Parameters
    List<String> parameters;

    private PluginInstance<NumberFactoryPlugin> numberFactoryInstance;
    private PluginInstance<FormulaPlugin> formulaInstance;
    private PluginInstance<RepresentationPlugin> representationInstance;
    private Rectangle rectangle;
    private JuliaSetPoint juliaSetPoint;

    public ImageGenerationCli(MainCli mainCli) {
        this.mainCli = mainCli;
    }

    @Override
    public void run() {
        try {
            Loader loader = new Loader(mainCli);
            loader.loadProfile();
            // I can't see the need of keeping this lock active
            loader.getPreferencesFile().close();

            warnForParsingProblems(loader);

            if (!loader.hasMinimalSetOfPlugins()) {
                warnForInsufficientPluginsInstalled(loader);
                return;
            }

            // Find matching number factory
            List<NumberFactoryPlugin> matchingNumberFactories = findMatchingPlugins(loader.getAvailableNumberFactories(), numberFactoryId);
            if (matchingNumberFactories.isEmpty()) {
                warnForMatchNotFound("number factory", numberFactoryId);
                return;
            }
            if (matchingNumberFactories.size() > 1) {
                warnForMultipleMatches(matchingNumberFactories, "number factories", numberFactoryId);
                return;
            }

            // Find matching formula
            List<FormulaPlugin> matchingFormulas = findMatchingPlugins(loader.getAvailableFormulas(), formulaId);
            if (matchingFormulas.isEmpty()) {
                warnForMatchNotFound("formula", formulaId);
                return;
            }
            if (matchingFormulas.size() > 1) {
                warnForMultipleMatches(matchingFormulas, "formulas", formulaId);
                return;
            }

            // Find matching representation
            List<RepresentationPlugin> matchingRepresentations = findMatchingPlugins(loader.getAvailableRepresentations(), representationId);
            if (matchingRepresentations.isEmpty()) {
                warnForMatchNotFound("representation", representationId);
                return;
            }
            if (matchingRepresentations.size() > 1) {
                warnForMultipleMatches(matchingRepresentations, "representations", representationId);
                return;
            }

            Preferences preferences = loader.getPreferences();
            if (width == null) {
                width = preferences.getImageWidth();
            }
            if (height == null) {
                height = preferences.getImageHeight();
            }

            numberFactoryInstance = new PluginInstance<NumberFactoryPlugin>(matchingNumberFactories.get(0));
            formulaInstance = new PluginInstance<FormulaPlugin>(matchingFormulas.get(0));
            representationInstance = new PluginInstance<RepresentationPlugin>(matchingRepresentations.get(0));

            // TBC
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public int execute(String[] args) {
        return new CommandLine(this).execute(args);
    }

    private boolean parseParameters() {
        for (String token : parameters) {
            String lSide = token.substring(0, token.indexOf("="));
            String rSide = token.substring(lSide.length());
            if (lSide.equals("rectangle")) {
                parseRectangle(rSide);
            } else if (lSide.equals("c") || lSide.equals("C") || lSide.equals("juliaSetPoint")) {
                parseJuliaSetPoint(rSide);
            } else {

            }
        }
        return true;
    }

    private void parseRectangle(String rectangleString) {
        String[] components = rectangleString.split(",", 4);
        Decimal re0 = new Decimal(components[0]);
        Decimal im0 = new Decimal(components[1]);
        Decimal re1 = new Decimal(components[2]);
        Decimal im1 = new Decimal(components[3]);
        rectangle = new Rectangle(re0, im0, re1, im1);
    }

    private void parseJuliaSetPoint(String juliaSetPointString) {
        if (juliaSetPointString.equals("default")) {
            juliaSetPoint = formulaInstance.getPlugin().getDefaultJuliaSetPoint();
        } else {
            String[] components = juliaSetPointString.split(",", 2);
            Decimal re = new Decimal(components[0]);
            Decimal im = new Decimal(components[1]);
            juliaSetPoint = new JuliaSetPoint(re, im);
        }
    }

    private static <P extends Plugin> List<P> findMatchingPlugins(List<P> plugins, String searchString) {
        if (searchString == null || searchString.isEmpty()) {
            return plugins;
        }

        List<P> matches = plugins.stream()
            .filter(p -> p.getId().contains(searchString))
            .collect(Collectors.toList());
        Optional<P> exactMatch = matches.stream()
            .filter(p -> p.getId().equals(searchString))
            .findFirst();
        if (exactMatch.isPresent()) {
            return Collections.singletonList(exactMatch.get());
        }
        return matches;
    }

    private static void warnForParsingProblems(Loader loader) {
        if (loader.getParserOutput() != null) {
            Utilities.print("Warning: ");
            if (loader.getProblemCount(DescriptorParser.Problem.FATAL_ERROR) > 0) {
                Utilities.print("one or more plugins will not be available due to errors in their relative descriptors.");
            } else if (loader.getProblemCount(DescriptorParser.Problem.ERROR) > 0) {
                Utilities.print("one or more plugins will possibly miss feautures due to errors in their relative descriptors.");
            } else if (loader.getProblemCount(DescriptorParser.Problem.WARNING) > 0) {
                Utilities.print("one or more plugin descriptors were parsed with warnings.");
            }
            Utilities.println("See ", loader.getProfile().getDescriptorParserOutputFile(), " for details.");
        }
    }

    private static void warnForInsufficientPluginsInstalled(Loader loader) {
        Utilities.println("Error: at least 1 plugin for each of the 3 plugin families must be available in order to run the program.");
        Utilities.println("Details:");
        Utilities.println("- Available number factories: ", loader.getAvailableNumberFactories().size());
        Utilities.println("- Available formulas: ", loader.getAvailableFormulas().size());
        Utilities.println("- Available representations: ", loader.getAvailableRepresentations().size());
    }

    private static void warnForMatchNotFound(String pluginName, String searchString) {
        Utilities.println("Error: cannot find ", pluginName, " matching search string \"", Objects.toString(searchString, ""), "\".");
    }

    private static void warnForMultipleMatches(List<? extends Plugin> matches, String pluginName, String searchString) {
        Utilities.println("Error: multiple ", pluginName, " matching search string \"", Objects.toString(searchString, ""), "\":");
        matches.forEach(p -> {
            Utilities.println("- ", p.getId());
        });
    }
}