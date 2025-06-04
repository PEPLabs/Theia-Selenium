import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeleniumTest {
    private WebDriver webDriver;
    private WebDriverWait wait;
  
    @BeforeEach
    public void setUp() {
        try {
            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
            
            // Get HTML file
            File file = new File("src/main/Callbacks.html");
            String path = "file://" + file.getAbsolutePath();
            
            System.err.println("Creating ChromeOptions...");
            // Create ChromeOptions for Ubuntu ARM
            ChromeOptions options = new ChromeOptions();
            
            // Try to use system chromium instead of downloaded one
            options.setBinary("/usr/bin/chromium-browser");
            
            // Essential arguments for Ubuntu ARM and containers
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");                  // Critical for root
            options.addArguments("--disable-dev-shm-usage");       // Critical for containers
            options.addArguments("--disable-gpu");                 
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-web-security");
            options.addArguments("--single-process");              // For ARM compatibility
            options.addArguments("--no-zygote");                   
            options.addArguments("--disable-setuid-sandbox");      
            options.addArguments("--user-data-dir=/tmp/chrome-test");
            
            // Create ChromeDriver
            webDriver = new ChromeDriver(options);
            
            // Initialize WebDriverWait with explicit waits
            wait = new WebDriverWait(webDriver, Duration.ofSeconds(15));
            
            // Set timeouts
            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            
            // Open the HTML file
            System.err.println("Navigating to HTML file...");
            webDriver.get(path);
            
            // Wait for page to load completely
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            
        } catch (Exception e) {
            System.err.println("SETUP FAILED: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            
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
        if (webDriver != null) {
            try {
                webDriver.quit();
            } catch (Exception e) {
                System.err.println("Error closing WebDriver: " + e.getMessage());
            }
        }
    }

    @Test
    public void testOriginalArray() {
        WebElement originalElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("original"))
        );
        String originalText = originalElement.getText();
        List<String> expectedArray = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Principal Skinner", "Mr Burns", "Moe", "Ned Flanders");
        assertEquals(String.join(",", expectedArray), originalText);
    }

    @Test
    public void testFilteredArray() {
        WebElement filteredElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("afterFilter"))
        );
        String filteredText = filteredElement.getText();
        List<String> expectedFiltered = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Moe");
        assertEquals(String.join(",", expectedFiltered), filteredText);
    }

    @Test
    public void testMappedArray() {
        WebElement mappedElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("afterMap"))
        );
        String mappedText = mappedElement.getText();
        List<String> expectedMapped = Arrays.asList("HOMER", "MARGE", "BART", "LISA", "MAGGIE", "PRINCIPAL SKINNER", "MR BURNS", "MOE", "NED FLANDERS");
        assertEquals(String.join(",", expectedMapped), mappedText);
    }

    @Test
    public void testArrForEach() {
        try {
            // Wait for console output element to be present and populated
            WebElement consoleOutputElement = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("consoleOutput"))
            );
            
            // Wait for the element to have non-empty text content
            wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBe(By.id("consoleOutput"), "")
            ));
            
            String consoleText = consoleOutputElement.getText();
            
            List<String> expectedArray = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Principal Skinner", "Mr Burns", "Moe", "Ned Flanders");
            
            for (String item : expectedArray) {
                assertTrue(consoleText.contains(item), 
                    "Console output should contain " + item + ". Actual output: " + consoleText);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    } 
}