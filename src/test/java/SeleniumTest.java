import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeleniumTest {
    private WebDriver webDriver;
    private WebDriverWait wait;
    private static final Logger logger = Logger.getLogger(SeleniumTest.class.getName());
  
    @BeforeEach
    public void setUp() {
        try {
            // Debug: Print environment information
            System.out.println("=== ENVIRONMENT DEBUG INFO ===");
            System.out.println("Java version: " + System.getProperty("java.version"));
            System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            System.out.println("Working directory: " + System.getProperty("user.dir"));
            System.out.println("User home: " + System.getProperty("user.home"));
            
            // Debug: Check for Chrome/Chromium binaries
            String[] possibleChromePaths = {
                "/usr/bin/chromium-browser",
                "/usr/bin/chromium", 
                "/usr/bin/google-chrome",
                "/snap/bin/chromium",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
            };
            
            System.out.println("\n=== CHROME BINARY CHECK ===");
            String foundChromePath = null;
            for (String path : possibleChromePaths) {
                File chromeFile = new File(path);
                System.out.println("Checking " + path + ": " + chromeFile.exists());
                if (chromeFile.exists() && foundChromePath == null) {
                    foundChromePath = path;
                }
            }
            
            // Debug: Check for ChromeDriver
            String[] possibleDriverPaths = {
                "/usr/bin/chromedriver",
                "/usr/local/bin/chromedriver",
                "/snap/bin/chromedriver",
                System.getProperty("user.home") + "/.cache/selenium/chromedriver/linux64/chromedriver",
                "/opt/chromedriver/chromedriver"
            };
            
            System.out.println("\n=== CHROMEDRIVER CHECK ===");
            String foundDriverPath = null;
            for (String path : possibleDriverPaths) {
                File driverFile = new File(path);
                System.out.println("Checking " + path + ": " + driverFile.exists());
                if (driverFile.exists() && foundDriverPath == null) {
                    foundDriverPath = path;
                    // Verify it's executable
                    if (!driverFile.canExecute()) {
                        System.out.println("WARNING: " + path + " exists but is not executable");
                        try {
                            driverFile.setExecutable(true);
                            System.out.println("Made " + path + " executable");
                        } catch (Exception e) {
                            System.out.println("Could not make " + path + " executable: " + e.getMessage());
                            foundDriverPath = null;
                            continue;
                        }
                    }
                }
            }
            
            // CRITICAL: Set ChromeDriver path to bypass selenium-manager
            if (foundDriverPath != null) {
                System.setProperty("webdriver.chrome.driver", foundDriverPath);
                System.out.println("Using ChromeDriver: " + foundDriverPath);
                
                // Disable selenium-manager to prevent ARM64 compatibility issues
                System.setProperty("webdriver.chrome.driver", foundDriverPath);
                System.setProperty("selenium.manager.debug", "false");
                
            } else {
                throw new RuntimeException("No compatible ChromeDriver found for ARM64. Please install ChromeDriver manually.");
            }
            
            // Debug: Check HTML file
            File file = new File("src/main/Callbacks.html");
            String path = "file://" + file.getAbsolutePath();
            System.out.println("\n=== HTML FILE CHECK ===");
            System.out.println("HTML file path: " + file.getAbsolutePath());
            System.out.println("HTML file exists: " + file.exists());
            System.out.println("HTML file URL: " + path);
            
            if (!file.exists()) {
                // Try alternative paths
                String[] altPaths = {
                    "Callbacks.html",
                    "src/test/resources/Callbacks.html",
                    "test-resources/Callbacks.html"
                };
                
                System.out.println("Trying alternative HTML paths:");
                for (String altPath : altPaths) {
                    File altFile = new File(altPath);
                    System.out.println("  " + altPath + ": " + altFile.exists());
                    if (altFile.exists()) {
                        file = altFile;
                        path = "file://" + file.getAbsolutePath();
                        System.out.println("Using alternative path: " + path);
                        break;
                    }
                }
            }
            
            System.out.println("\n=== CREATING CHROME OPTIONS ===");
            ChromeOptions options = new ChromeOptions();
            
            // Set Chrome binary if found
            if (foundChromePath != null) {
                options.setBinary(foundChromePath);
                System.out.println("Using Chrome binary: " + foundChromePath);
            }
            
            // Add arguments with debug output
            String[] args = {
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--disable-extensions",
                "--disable-web-security",
                "--single-process",
                "--no-zygote",
                "--disable-setuid-sandbox",
                "--user-data-dir=/tmp/chrome-test-" + System.currentTimeMillis(),
                "--verbose",
                "--enable-logging=stderr",
                "--log-level=0",
                "--disable-background-timer-throttling",
                "--disable-backgrounding-occluded-windows",
                "--disable-renderer-backgrounding",
                "--disable-features=TranslateUI",
                "--disable-ipc-flooding-protection",
                "--force-device-scale-factor=1",
                "--disable-hang-monitor",
                "--disable-prompt-on-repost",
                "--disable-domain-reliability"
            };
            
            System.out.println("Chrome arguments:");
            for (String arg : args) {
                options.addArguments(arg);
                System.out.println("  " + arg);
            }
            
            // Enable logging
            LoggingPreferences logPrefs = new LoggingPreferences();
            logPrefs.enable(LogType.BROWSER, Level.ALL);
            logPrefs.enable(LogType.DRIVER, Level.ALL);
            logPrefs.enable(LogType.PERFORMANCE, Level.INFO);
            options.setCapability("goog:loggingPrefs", logPrefs);
            
            // Create ChromeDriverService with explicit path (bypass selenium-manager)
            ChromeDriverService.Builder serviceBuilder = new ChromeDriverService.Builder();
            
            // MUST set explicit driver path to avoid selenium-manager on ARM64
            if (foundDriverPath != null) {
                serviceBuilder.usingDriverExecutable(new File(foundDriverPath));
                System.out.println("ChromeDriverService using explicit path: " + foundDriverPath);
            } else {
                throw new RuntimeException("ChromeDriver path is required but not found");
            }
            
            serviceBuilder.withVerbose(true);
            serviceBuilder.withTimeout(Duration.ofSeconds(30));
            
            ChromeDriverService service = serviceBuilder.build();
            
            System.out.println("\n=== CREATING WEBDRIVER ===");
            webDriver = new ChromeDriver(service, options);
            System.out.println("WebDriver created successfully");
            
            // Initialize WebDriverWait
            wait = new WebDriverWait(webDriver, Duration.ofSeconds(15));
            
            // Set timeouts
            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            
            System.out.println("\n=== NAVIGATING TO PAGE ===");
            System.out.println("Navigating to: " + path);
            webDriver.get(path);
            System.out.println("Navigation completed");
            
            // Debug: Print page info
            System.out.println("Page title: " + webDriver.getTitle());
            System.out.println("Current URL: " + webDriver.getCurrentUrl());
            System.out.println("Page source length: " + webDriver.getPageSource().length());
            
            // Wait for page to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            System.out.println("Body element found - page loaded");
            
            // Debug: Print browser logs
            System.out.println("\n=== BROWSER LOGS ===");
            try {
                webDriver.manage().logs().get(LogType.BROWSER).forEach(entry -> {
                    System.out.println("BROWSER: " + entry);
                });
            } catch (Exception e) {
                System.out.println("Could not retrieve browser logs: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("\n=== SETUP FAILED ===");
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getSimpleName());
            System.err.println("Stack trace:");
            e.printStackTrace();
            
            // Additional debug for specific exceptions
            if (e.getMessage() != null) {
                if (e.getMessage().contains("chrome not reachable")) {
                    System.err.println("\nDEBUG: Chrome not reachable - this usually means:");
                    System.err.println("  1. Chrome binary not found or not executable");
                    System.err.println("  2. Chrome crashed during startup");
                    System.err.println("  3. Permission issues");
                } else if (e.getMessage().contains("ChromeDriver")) {
                    System.err.println("\nDEBUG: ChromeDriver issue - check:");
                    System.err.println("  1. ChromeDriver is installed and in PATH");
                    System.err.println("  2. ChromeDriver version matches Chrome version");
                    System.err.println("  3. ChromeDriver has execute permissions");
                }
            }
            
            // Cleanup on failure
            if (webDriver != null) {
                try {
                    webDriver.quit();
                } catch (Exception cleanupEx) {
                    System.err.println("Cleanup error: " + cleanupEx.getMessage());
                }
            }
            throw new RuntimeException("Setup failed", e);
        }
    }

    @AfterEach
    public void tearDown() {
        System.out.println("\n=== TEARDOWN ===");
        if (webDriver != null) {
            try {
                // Print final browser logs
                try {
                    System.out.println("Final browser logs:");
                    webDriver.manage().logs().get(LogType.BROWSER).forEach(entry -> {
                        System.out.println("BROWSER: " + entry);
                    });
                } catch (Exception e) {
                    System.out.println("Could not retrieve final browser logs: " + e.getMessage());
                }
                
                // Force close all windows first
                try {
                    webDriver.close();
                    System.out.println("WebDriver windows closed");
                } catch (Exception e) {
                    System.out.println("Error closing windows: " + e.getMessage());
                }
                
                // Then quit the driver
                webDriver.quit();
                System.out.println("WebDriver quit successfully");
                
                // Force null the reference
                webDriver = null;
                wait = null;
                
                // Force garbage collection to clean up any lingering references
                System.gc();
                
                // Small delay to allow cleanup
                Thread.sleep(100);
                
            } catch (Exception e) {
                System.err.println("Error closing WebDriver: " + e.getMessage());
                // Force quit even on error
                try {
                    if (webDriver != null) {
                        webDriver.quit();
                        webDriver = null;
                    }
                } catch (Exception ignored) {}
            }
        }
        System.out.println("Teardown completed");
    }

    @Test
    public void testOriginalArray() {
        System.out.println("\n=== TEST: Original Array ===");
        WebElement originalElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("original"))
        );
        String originalText = originalElement.getText();
        System.out.println("Found original text: " + originalText);
        
        List<String> expectedArray = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Principal Skinner", "Mr Burns", "Moe", "Ned Flanders");
        String expectedText = String.join(",", expectedArray);
        System.out.println("Expected text: " + expectedText);
        
        assertEquals(expectedText, originalText);
        System.out.println("Original array test PASSED");
    }

    @Test
    public void testFilteredArray() {
        System.out.println("\n=== TEST: Filtered Array ===");
        WebElement filteredElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("afterFilter"))
        );
        String filteredText = filteredElement.getText();
        System.out.println("Found filtered text: " + filteredText);
        
        List<String> expectedFiltered = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Moe");
        String expectedText = String.join(",", expectedFiltered);
        System.out.println("Expected text: " + expectedText);
        
        assertEquals(expectedText, filteredText);
        System.out.println("Filtered array test PASSED");
    }

    @Test
    public void testMappedArray() {
        System.out.println("\n=== TEST: Mapped Array ===");
        WebElement mappedElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("afterMap"))
        );
        String mappedText = mappedElement.getText();
        System.out.println("Found mapped text: " + mappedText);
        
        List<String> expectedMapped = Arrays.asList("HOMER", "MARGE", "BART", "LISA", "MAGGIE", "PRINCIPAL SKINNER", "MR BURNS", "MOE", "NED FLANDERS");
        String expectedText = String.join(",", expectedMapped);
        System.out.println("Expected text: " + expectedText);
        
        assertEquals(expectedText, mappedText);
        System.out.println("Mapped array test PASSED");
    }

    @Test
    public void testArrForEach() {
        System.out.println("\n=== TEST: ForEach Array ===");
        try {
            // Wait for console output element to be present and populated
            WebElement consoleOutputElement = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("consoleOutput"))
            );
            System.out.println("Console output element found");
            
            // Wait for the element to have non-empty text content
            wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBe(By.id("consoleOutput"), "")
            ));
            
            String consoleText = consoleOutputElement.getText();
            System.out.println("Console output text: " + consoleText);
            
            List<String> expectedArray = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Principal Skinner", "Mr Burns", "Moe", "Ned Flanders");
            
            for (String item : expectedArray) {
                boolean contains = consoleText.contains(item);
                System.out.println("Checking for '" + item + "': " + contains);
                assertTrue(contains, 
                    "Console output should contain " + item + ". Actual output: " + consoleText);
            }
            
            System.out.println("ForEach array test PASSED");
            
        } catch (Exception e) {
            System.err.println("ForEach test failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    } 
}