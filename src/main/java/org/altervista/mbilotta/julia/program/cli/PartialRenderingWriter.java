package org.altervista.mbilotta.julia.program.cli;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.altervista.mbilotta.julia.IntermediateImage;
import org.altervista.mbilotta.julia.Utilities;
import org.altervista.mbilotta.julia.program.Application;
import org.altervista.mbilotta.julia.program.Application.Image;
import org.altervista.mbilotta.julia.program.JuliaExecutorService;
import org.altervista.mbilotta.julia.program.JuliaImageWriter;

public class PartialRenderingWriter extends Thread {

    private static final long TIMEOUT = 10L;
    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    private Thread mainThread;
    private Path outputPath;
    private Application.Image metadata;
    private IntermediateImage intermediateImage;
    private JuliaExecutorService executorService;
    

    public PartialRenderingWriter(Thread mainThread, Path outputPath, Image metadata, IntermediateImage intermediateImage,
            JuliaExecutorService executorService) {
        this.mainThread = mainThread;
        this.outputPath = outputPath;
        this.metadata = metadata;
        this.intermediateImage = intermediateImage;
        this.executorService = executorService;
    }

    @Override
    public void run() {
        mainThread.interrupt();
        try {
            mainThread.join();
            if (executorService.awaitTermination(TIMEOUT, TIMEOUT_UNIT)) {
                if (!intermediateImage.isComplete()) {
                    Path outputDir = outputPath.getParent();
                    if (outputDir == null) {
                        outputDir = Paths.get("");
                    }
                    File outputFile = File.createTempFile("wip", ".jim", outputDir.toFile());
                    JuliaImageWriter jimWriter = new JuliaImageWriter(outputFile, metadata, intermediateImage);
                    jimWriter.setGuiRunning(false);
                    jimWriter.write();
                    Utilities.println("Partial rendering saved to: ", outputFile);
                }
            } else {
                Utilities.println("Error: rendering process is not responding. Partial work will be lost!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
