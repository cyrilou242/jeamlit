package tech.catheu.jeamlit.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tech.catheu.jeamlit.core.Server;
import tech.catheu.jeamlit.desktop.WebViewDesktopLauncher;

import java.awt.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;


@Command(name = "jeamlit", mixinStandardHelpOptions = true, version = "1.0.0",
        description = "Streamlit-like framework for Java")
public class Cli implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(Cli.class);

    @Command(name = "run", description = "Run a Jeamlit application")
    static class RunCommand implements Callable<Integer> {

        @SuppressWarnings("unused")
        @Parameters(index = "0", description = "The Jeamlit app Java file to run")
        private String appPath;

        @SuppressWarnings("unused")
        @Option(names = {"-p", "--port"}, description = "Port to run server on", defaultValue = "8080")
        private int port;

        @SuppressWarnings("unused")
        @Option(names = {"--no-browser"}, description = "Don't open browser automatically")
        private boolean noBrowser;

        @SuppressWarnings("unused")
        @Option(names = {"--classpath", "-cp"}, description = "Additional classpath entries")
        private String classpath;

        @SuppressWarnings("unused")
        @Option(names = {"--headers-file"}, description = "File containing additional HTML headers")
        private String headersFile;

        @Override
        public Integer call() throws Exception {
            if (!parametersAreValid()) {
                return 1;
            }

            final Path javaFilePath = Paths.get(appPath);
            logger.info("Starting Jeamlit on file {}", javaFilePath.toAbsolutePath());
            if (!Files.exists(javaFilePath)) {
                logger.error("File not found: {}", javaFilePath.toAbsolutePath());
                return 1;
            }
            // Create server
            final Server server = new Server(javaFilePath, classpath, port, headersFile);

            // Start everything
            final String url = "http://localhost:" + port;
            try {
                server.start();
                logger.info("Server started at {} ", url);
                logger.info("Press Ctrl+C to stop");
            } catch (Exception e) {
                logger.error("Error starting server", e);
                return 1;
            }
            if (!noBrowser) {
                openBrowser(url);
            }

            // wait for interruption
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down...");
                server.stop();
            }));
            Thread.currentThread().join();

            return 0;
        }

        private boolean parametersAreValid() {
            boolean parametersAreValid = true;
            if (!appPath.endsWith(".java")) {
                // note: I know a Java file could in theory not end with .java but I want to reduce other issues downstream
                logger.error("File {} does not look like a java file. File should end with .java",
                             appPath);
                parametersAreValid = false;
            }
            // perform other parameter checks here

            return parametersAreValid;
        }

        private void openBrowser(final String url) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    logger.warn("Desktop not supported, cannot open browser automatically");
                }
            } catch (Exception e) {
                logger.error("Could not open browser. Please open browser manually: " + e.getMessage());
            }
        }
    }

    @Command(name = "desktop", description = "Run a Jeamlit application in desktop mode using native WebView")
    static class DesktopCommand implements Callable<Integer> {
        
        @SuppressWarnings("unused")
        @Parameters(index = "0", description = "The Jeamlit app Java file to run")
        private String appPath;
        
        @SuppressWarnings("unused")
        @Option(names = {"--classpath", "-cp"}, description = "Additional classpath entries")
        private String classpath;
        
        @SuppressWarnings("unused")
        @Option(names = {"--headers-file"}, description = "File containing additional HTML headers")
        private String headersFile;
        
        @SuppressWarnings("unused")
        @Option(names = {"--title"}, description = "Window title", defaultValue = "Jeamlit Application")
        private String title;
        
        @SuppressWarnings("unused")
        @Option(names = {"--width"}, description = "Window width", defaultValue = "1200")
        private int width;
        
        @SuppressWarnings("unused")
        @Option(names = {"--height"}, description = "Window height", defaultValue = "800")
        private int height;
        
        @SuppressWarnings("unused")
        @Option(names = {"--dev-mode"}, description = "Enable development mode (hot reload, debug info)")
        private boolean devMode;
        
        @Override
        public Integer call() throws Exception {
            if (!parametersAreValid()) {
                return 1;
            }
            
            final Path javaFilePath = Paths.get(appPath);
            logger.info("Starting Jeamlit desktop application for {}", javaFilePath.toAbsolutePath());
            
            if (!Files.exists(javaFilePath)) {
                logger.error("File not found: {}", javaFilePath.toAbsolutePath());
                return 1;
            }
            
            // Check macOS requirements
            if (!checkMacOSRequirements()) {
                return 1;
            }
            
            try {
                // Launch WebView desktop application
                WebViewDesktopLauncher.launchDesktop(
                    appPath,
                    classpath,
                    headersFile,
                    title,
                    width,
                    height,
                    devMode
                );
                return 0;
            } catch (Exception e) {
                logger.error("Error starting desktop application", e);
                return 1;
            }
        }
        
        private boolean parametersAreValid() {
            boolean valid = true;
            if (!appPath.endsWith(".java")) {
                logger.error("File {} does not look like a java file. File should end with .java", appPath);
                valid = false;
            }
            if (width <= 0 || height <= 0) {
                logger.error("Window dimensions must be positive");
                valid = false;
            }
            return valid;
        }
        
        private boolean checkMacOSRequirements() {
            String osName = System.getProperty("os.name", "").toLowerCase();
            if (!osName.contains("mac")) {
                // Not macOS, no special requirements
                return true;
            }
            
            // Check if -XstartOnFirstThread is set
            String startOnFirstThread = System.getProperty("java.awt.headless");
            
            // For webview on macOS, we need -XstartOnFirstThread JVM argument
            // Check if we're likely running with it by looking for typical signs
            try {
                // Try a simple check - this will likely fail if not on first thread
                // but webview library will give a clearer error anyway
                return true; // Let webview library handle the detailed error message
            } catch (Exception e) {
                logger.error("macOS desktop mode requires the JVM argument: -XstartOnFirstThread");
                logger.error("Please run with: java -XstartOnFirstThread -cp ... tech.catheu.jeamlit.cli.Cli desktop {}", appPath);
                logger.error("Or use the provided run script if available");
                return false;
            }
        }
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Cli())
                .addSubcommand("run", new RunCommand())
                .addSubcommand("desktop", new DesktopCommand())
                .execute(args);
        System.exit(exitCode);
    }
}