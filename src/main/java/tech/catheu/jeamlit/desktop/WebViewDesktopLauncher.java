package tech.catheu.jeamlit.desktop;

import dev.webview.Webview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jeamlit.core.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Native WebView desktop launcher for Jeamlit applications.
 * Uses system's native webview (Edge on Windows, WebKit on macOS/Linux) 
 * to create a lightweight desktop application.
 */
public class WebViewDesktopLauncher {
    private static final Logger logger = LoggerFactory.getLogger(WebViewDesktopLauncher.class);
    
    // Configuration
    private final String appPath;
    private final String classpath;
    private final String headersFile;
    private final String windowTitle;
    private final int windowWidth;
    private final int windowHeight;
    private final boolean devMode;
    
    // Runtime components
    private Server server;
    private Webview webview;
    private int serverPort;
    private final CountDownLatch serverStartLatch = new CountDownLatch(1);
    
    public WebViewDesktopLauncher(String appPath, String classpath, String headersFile,
                                  String windowTitle, int windowWidth, int windowHeight, boolean devMode) {
        this.appPath = appPath;
        this.classpath = classpath;
        this.headersFile = headersFile;
        this.windowTitle = windowTitle != null ? windowTitle : "Jeamlit Application";
        this.windowWidth = windowWidth > 0 ? windowWidth : 1200;
        this.windowHeight = windowHeight > 0 ? windowHeight : 800;
        this.devMode = devMode;
    }
    
    /**
     * Launch the desktop application.
     * This method blocks until the window is closed.
     */
    public void launch() {
        try {
            // Find available port for embedded server
            serverPort = findAvailablePort();
            logger.info("Using port {} for embedded server", serverPort);
            
            // Start server in background thread
            startServerAsync();
            
            // Wait for server to start (max 10 seconds)
            if (!serverStartLatch.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Server failed to start within timeout");
            }
            
            // Create and configure webview
            createWebView();
            
            // Load the application
            String appUrl = "http://localhost:" + serverPort;
            logger.info("Loading application from {}", appUrl);
            webview.loadURL(appUrl);
            
            // Run webview event loop (blocks until window is closed)
            logger.info("Starting desktop application");
            webview.run();
            
        } catch (Exception e) {
            logger.error("Error launching desktop application", e);
            showError("Failed to start application: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private void createWebView() {
        // Create webview with debug mode enabled if in dev mode
        webview = new Webview(devMode);
        
        // Configure window
        webview.setTitle(getWindowTitle());
        webview.setSize(windowWidth, windowHeight);
        
        // Bind utility functions for JavaScript interaction
        bindUtilityFunctions();
        
        logger.info("WebView created: {}x{} - {}", windowWidth, windowHeight, getWindowTitle());
    }
    
    private void bindUtilityFunctions() {
        // TODO: Add utility functions when API is better understood
        // For now, keeping it simple to get the basic webview working
    }
    
    private String getWindowTitle() {
        Path appFile = Paths.get(appPath);
        return windowTitle + " - " + appFile.getFileName();
    }
    
    private void startServerAsync() {
        Thread serverThread = new Thread(() -> {
            try {
                Path javaFilePath = Paths.get(appPath);
                logger.info("Starting embedded server for {}", javaFilePath.toAbsolutePath());
                
                // Create desktop-specific server
                server = new WebViewServer(javaFilePath, classpath, serverPort, headersFile, !devMode);
                server.start();
                
                logger.info("Embedded server started on port {}", serverPort);
                serverStartLatch.countDown();
                
            } catch (Exception e) {
                logger.error("Failed to start embedded server", e);
                showError("Failed to start server: " + e.getMessage());
                serverStartLatch.countDown();
            }
        });
        
        serverThread.setDaemon(true);
        serverThread.setName("Jeamlit-Server");
        serverThread.start();
    }
    
    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
    
    private void showError(String message) {
        logger.error("Showing error to user: {}", message);
        
        if (webview == null) {
            // Create a minimal webview just to show the error
            webview = new Webview(false);
            webview.setTitle("Jeamlit - Error");
            webview.setSize(600, 400);
        }
        
        // For now, just load a simple error URL
        // TODO: Use proper HTML loading once API is clarified
        webview.loadURL("data:text/html," + java.net.URLEncoder.encode(createErrorHtml(message), java.nio.charset.StandardCharsets.UTF_8));
    }
    
    private String createErrorHtml(String message) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Jeamlit Error</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
                        background: linear-gradient(135deg, #f5f7fa 0%%, #c3cfe2 100%%);
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        height: 100vh;
                        margin: 0;
                        padding: 20px;
                        box-sizing: border-box;
                    }
                    .error-container {
                        background: white;
                        border-radius: 12px;
                        padding: 2rem;
                        box-shadow: 0 10px 25px rgba(0,0,0,0.1);
                        max-width: 500px;
                        text-align: center;
                    }
                    h1 {
                        color: #e74c3c;
                        margin-top: 0;
                        font-size: 1.5rem;
                        font-weight: 600;
                    }
                    p {
                        color: #666;
                        line-height: 1.6;
                        margin: 1rem 0;
                    }
                    .details {
                        background: #f8f9fa;
                        border: 1px solid #e9ecef;
                        border-radius: 8px;
                        padding: 1rem;
                        font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
                        font-size: 0.85rem;
                        color: #495057;
                        text-align: left;
                        margin-top: 1rem;
                        word-break: break-word;
                    }
                    .icon {
                        font-size: 3rem;
                        margin-bottom: 1rem;
                    }
                </style>
            </head>
            <body>
                <div class="error-container">
                    <div class="icon">⚠️</div>
                    <h1>Application Error</h1>
                    <p>The Jeamlit desktop application encountered an error and cannot continue.</p>
                    <div class="details">%s</div>
                    <p style="font-size: 0.9rem; color: #999; margin-top: 1.5rem;">
                        You can close this window and try running the application again.
                    </p>
                </div>
            </body>
            </html>
            """, escapeHtml(message));
    }
    
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
    
    private void cleanup() {
        logger.info("Cleaning up desktop application resources");
        
        // Stop server
        if (server != null) {
            try {
                server.stop();
                logger.info("Server stopped");
            } catch (Exception e) {
                logger.error("Error stopping server", e);
            }
        }
        
        // Close webview
        if (webview != null) {
            try {
                webview.close();
                logger.info("WebView closed");
            } catch (Exception e) {
                logger.error("Error closing webview", e);
            }
        }
    }
    
    /**
     * Static launcher method for command line usage.
     */
    public static void launchDesktop(String appPath, String classpath, String headersFile,
                                     String title, int width, int height, boolean devMode) {
        WebViewDesktopLauncher launcher = new WebViewDesktopLauncher(
            appPath, classpath, headersFile, title, width, height, devMode
        );
        launcher.launch();
    }
}