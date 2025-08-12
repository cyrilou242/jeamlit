package tech.catheu.jeamlit.desktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jeamlit.core.Server;

import java.nio.file.Path;

/**
 * WebView-specific server implementation.
 * Extends the base Server with desktop-specific optimizations
 * and optional hot reloading control for packaged applications.
 */
public class WebViewServer extends Server {
    private static final Logger LOG = LoggerFactory.getLogger(WebViewServer.class);
    
    private final boolean disableHotReload;
    
    /**
     * Create a new WebView server instance.
     * 
     * @param appPath The path to the Jeamlit application file
     * @param classpath Additional classpath entries
     * @param port The port to run the server on
     * @param headersFile Optional file containing custom HTML headers
     * @param disableHotReload Whether to disable hot reloading (useful for packaged apps)
     */
    public WebViewServer(Path appPath, String classpath, int port, String headersFile, boolean disableHotReload) {
        super(appPath, classpath, port, headersFile);
        this.disableHotReload = disableHotReload;
        LOG.info("WebView server created - Hot reload: {}, Port: {}", !disableHotReload, port);
    }
    
    @Override
    public void start() {
        LOG.info("Starting WebView server on port {}", port);
        super.start();
        
        if (disableHotReload) {
            LOG.info("Hot reload disabled for desktop application");
            // Note: The file watcher will still run but notifyReload() will be a no-op
        } else {
            LOG.info("Hot reload enabled for development mode");
        }
    }
    
    @Override
    protected void notifyReload() {
        if (disableHotReload) {
            LOG.debug("Hot reload disabled, ignoring file change notification");
            return;
        }
        
        LOG.info("File changed, reloading application (dev mode)");
        super.notifyReload();
    }
    
    @Override
    public void stop() {
        LOG.info("Stopping WebView server");
        super.stop();
    }
    
    /**
     * Check if hot reload is enabled for this server instance.
     * 
     * @return true if hot reload is enabled, false otherwise
     */
    public boolean isHotReloadEnabled() {
        return !disableHotReload;
    }
}