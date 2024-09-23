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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.bonigarcia.wdm.WebDriverManager;


public class SeleniumTest {

    private WebDriver webDriver;
    private WebDriverWait wait;

  
@BeforeEach
public void setUp() {
    WebDriverManager.chromedriver().setup(); // Automatically manages driver version
    
    // Get file
    File file = new File("src/main/Callbacks.html");
    String path = "file://" + file.getAbsolutePath();

    // Create a new ChromeDriver instance
    ChromeOptions options = new ChromeOptions();
    options.addArguments("headless");
    webDriver = new ChromeDriver(options);
    wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
    // Open the HTML file
    webDriver.get(path);
}


    @AfterEach
    public void tearDown() {
        webDriver.quit();
    }
    
    @Test
    public void testOriginalArray() {
        WebElement originalElement = webDriver.findElement(By.id("original"));
        String originalText = originalElement.getText();
        List<String> expectedArray = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Principal Skinner", "Mr Burns", "Moe", "Ned Flanders");
        assertEquals(String.join(",", expectedArray), originalText);
    }

    @Test
    public void testFilteredArray() {
        WebElement filteredElement = webDriver.findElement(By.id("afterFilter"));
        String filteredText = filteredElement.getText();
        List<String> expectedFiltered = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Moe");
        assertEquals(String.join(",", expectedFiltered), filteredText);
    }

    @Test
    public void testMappedArray() {
        WebElement mappedElement = webDriver.findElement(By.id("afterMap"));
        String mappedText = mappedElement.getText();
        List<String> expectedMapped = Arrays.asList("HOMER", "MARGE", "BART", "LISA", "MAGGIE", "PRINCIPAL SKINNER", "MR BURNS", "MOE", "NED FLANDERS");
        assertEquals(String.join(",", expectedMapped), mappedText);
    }

    @Test
    public void testArrForEach() {
        // For console output, you would need to modify the HTML to display logs in a visible element.
        WebElement consoleOutputElement = webDriver.findElement(By.id("consoleOutput")); // Assuming there's an element for this.
        String consoleText = consoleOutputElement.getText();
        List<String> expectedArray = Arrays.asList("Homer", "Marge", "Bart", "Lisa", "Maggie", "Principal Skinner", "Mr Burns", "Moe", "Ned Flanders");
        
        for (String item : expectedArray) {
            assertTrue(consoleText.contains(item), "Console output should contain " + item);
        }
    } 
}
