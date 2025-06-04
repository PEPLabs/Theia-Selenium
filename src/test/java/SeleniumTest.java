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
        // Set up ChromeDriver path - pointing to driver folder
        System.setProperty("webdriver.chrome.driver", "driver/chromedriver");
        
        // Get file
        File file = new File("src/main/Callbacks.html");
        String path = "file://" + file.getAbsolutePath();

        // Create ChromeOptions and configure for ARM Ubuntu
        ChromeOptions options = new ChromeOptions();
        
        // Set the binary path to chromium-browser (installed via apt)
        options.setBinary("/usr/bin/chromium-browser");
        
        // Add necessary flags for ARM and containerized environments
        options.addArguments("--headless=new"); // Use new headless mode
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-images");
        options.addArguments("--remote-debugging-port=9222");
        options.addArguments("--window-size=1920,1080");
        
        // Additional ARM-specific optimizations
        options.addArguments("--memory-pressure-off");
        options.addArguments("--max_old_space_size=4096");
        
        // Create ChromeDriver with configured options
        webDriver = new ChromeDriver(options);
        
        // Initialize WebDriverWait for more robust element finding
        wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
        
        // Open the HTML file
        webDriver.get(path);
        
        // Wait for page to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    @AfterEach
    public void tearDown() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }
    
    @Test
    public void testOriginalArray() {
        WebElement originalElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("original")));
        String originalText = originalElement.getText();
        List<String> expectedArray = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Principal Skinner", "Mr Burns", "Moe", "Ned Flanders");
        assertEquals(String.join(",", expectedArray), originalText);
    }

    @Test
    public void testFilteredArray() {
        WebElement filteredElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("afterFilter")));
        String filteredText = filteredElement.getText();
        List<String> expectedFiltered = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Moe");
        assertEquals(String.join(",", expectedFiltered), filteredText);
    }

    @Test
    public void testMappedArray() {
        WebElement mappedElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("afterMap")));
        String mappedText = mappedElement.getText();
        List<String> expectedMapped = Arrays.asList("HOMER", "MARGE", "BART", "LISA", "MAGGIE", "PRINCIPAL SKINNER", "MR BURNS", "MOE", "NED FLANDERS");
        assertEquals(String.join(",", expectedMapped), mappedText);
    }

    @Test
    public void testArrForEach() {
        // Wait for console output element to be present and populated
        WebElement consoleOutputElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("consoleOutput")));
        
        // Wait a bit more for JavaScript to execute and populate the console output
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBe(By.id("consoleOutput"), "")));
        
        String consoleText = consoleOutputElement.getText();
        List<String> expectedArray = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Principal Skinner", "Mr Burns", "Moe", "Ned Flanders");
        
        for (String item : expectedArray) {
            assertTrue(consoleText.contains(item), "Console output should contain " + item);
        }
    } 
}