package org.altervista.mbilotta.julia.program.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import org.altervista.mbilotta.julia.program.JuliaExecutorService;
import org.altervista.mbilotta.julia.program.Loader;
import org.altervista.mbilotta.julia.program.LockedFile;
import org.altervista.mbilotta.julia.program.Profile;
import org.altervista.mbilotta.julia.program.parsers.FormulaPlugin;
import org.altervista.mbilotta.julia.program.parsers.NumberFactoryPlugin;
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
    int width;

    @Option(names = { "-H", "--height" })
    int height;

    @Option(names = {"-n", "--number-factory"})
    String numberFactory;

    @Option(names = {"-f", "--formula"})
    String formula;

    @Option(names = {"-r", "--representation"})
    String representation;

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

    private Profile profile;
	private Preferences preferences;
	private LockedFile preferencesFile;

	private List<NumberFactoryPlugin> numberFactories;
	private List<FormulaPlugin> formulas;
	private List<RepresentationPlugin> representations;

    public ImageGenerationCli(MainCli mainCli) {
        this.mainCli = mainCli;
    }

    @Override
    public void run() {
        try {
            Loader loader = new Loader(mainCli);
            loader.doInBackground();

            // List number factories
            loader.getAvailableNumberFactories().forEach(nf -> {
                System.out.println(nf.getId());
            });

            // List formulas
            loader.getAvailableFormulas().forEach(f -> {
                System.out.println(f.getId());
            });

            // List representations
            loader.getAvailableRepresentations().forEach(r -> {
                System.out.println(r.getId());
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public int execute(String[] args) {
        return new CommandLine(this).execute(args);
    }
}