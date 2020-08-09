package org.altervista.mbilotta.julia.program.cli;

import java.nio.file.Path;
import java.util.List;

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

    @Option(names = { "-w", "--width" })
    int width;

    @Option(names = { "-h", "--height" })
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

    public ImageGenerationCli(MainCli mainCli) {
        this.mainCli = mainCli;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

    public int execute(String[] args) {
        return new CommandLine(this).execute(args);
    }
}