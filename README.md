# Upgrading Lab Infrastructure 
We need to make some changes to the way we do webdev labs. The new CDE environments will come with a chromium browser, and a chromium webdriver for selenium pre-installed. We need to switch to those and add some supporting code. We shouldn't need to change any of the test cases in any of the labs, just the setup and teardown stuff. 

For instance, in the [JS-CL-Classes](https://github.com/PEPLabs/JS-CL-CLASSES) lab there are two tests. We added 500+ lines of extra code to the file, but the tests were unchanged. We anticipate this should be the case for most if not all labs.

## AI Tools
We suggest using AI tools to make this quicker. Take a look at the changes below, but we should be able to have an AI make the changes we need by providing it the test file to edit, and the infrustructure to add in. I did this with [claude](https://claude.ai/) and it worked very well. Just watch out for it to do things like upgrade to Junit 5, the labs are currently using Junit 4 so we should stick with that for now. I copy/pasted into claude the JS-CL-Classes test class as well as the [prototype test file](https://github.com/PEPLabs/Theia-Selenium) and gave it the following prompt:
```
Take these two test files, and implement the new testing infrastructure from the larger one in the smaller one. The actual test logic should remain unchanged. Keep the new code on Junit 4, we aren't upgrading to Junit 5 today. Do not add any unnecessary code, don't make changes to the @Test annotated functions. 
```
This produced a [working lab](https://github.com/PEPLabs/Kyle-Testing-ChromeDriver).

Your mileage may vary, and this will use up all of your free daily claude tokens in short order.


### POM.xml
We should be able to use the same dependencies and plugins for most if not all labs. 

```xml

...

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
  </properties>

  <dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.11</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.seleniumhq.selenium</groupId>
      <artifactId>selenium-java</artifactId>
      <version>4.9.1</version>
    </dependency>
  </dependencies>

  <build>
    <pluginManagement><!-- lock down plugins versions to avoid using Maven defaults (may be moved to parent pom) -->
      <plugins>
        <!-- clean lifecycle, see https://maven.apache.org/ref/current/maven-core/lifecycles.html#clean_Lifecycle -->
        <plugin>
          <artifactId>maven-clean-plugin</artifactId>
          <version>3.1.0</version>
        </plugin>
        <!-- default lifecycle, jar packaging: see https://maven.apache.org/ref/current/maven-core/default-bindings.html#Plugin_bindings_for_jar_packaging -->
        <plugin>
          <artifactId>maven-resources-plugin</artifactId>
          <version>3.0.2</version>
        </plugin>
        <plugin>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.8.0</version>
        </plugin>
        <plugin>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>2.22.1</version>
        </plugin>
        <plugin>
          <artifactId>maven-jar-plugin</artifactId>
          <version>3.0.2</version>
        </plugin>
        <plugin>
          <artifactId>maven-install-plugin</artifactId>
          <version>2.5.2</version>
        </plugin>
        <plugin>
          <artifactId>maven-deploy-plugin</artifactId>
          <version>2.8.2</version>
        </plugin>
        <!-- site lifecycle, see https://maven.apache.org/ref/current/maven-core/lifecycles.html#site_Lifecycle -->
        <plugin>
          <artifactId>maven-site-plugin</artifactId>
          <version>3.7.1</version>
        </plugin>
        <plugin>
          <artifactId>maven-project-info-reports-plugin</artifactId>
          <version>3.0.0</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```


### We need to add the following:
#### `@Before`
Setting up the web driver for tests changes slightly. If there are other lab-specific steps in `@Before` we'll have to be careful to maintain that. Mostly we just set up selenium differently.
```Java
@Before
    public void setUp() {
        try {
            printEnvironmentInfo();
            
            BrowserConfig browserConfig = detectBrowserAndDriver();
            this.browserType = browserConfig.browserType;
            
            File htmlFile = findHtmlFile();
            String htmlUrl = determineHtmlUrl(htmlFile);
            
            webDriver = createWebDriver(browserConfig);
            
            wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
            
            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            System.out.println("\n=== NAVIGATING TO PAGE ===");
            System.out.println("Navigating to: " + htmlUrl);
            webDriver.get(htmlUrl);
            
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            System.out.println("Page loaded successfully");
            
            printPageInfo();
            
        } catch (Exception e) {
            System.err.println("\n=== SETUP FAILED ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            
            cleanup();
            throw new RuntimeException("Setup failed", e);
        }
    }
```
### `@After`
Some labs may already have an `@After` method. We probably keep what's there, and we want to include this as well. If the @After method is closing the webdriver, that's probably not necessary, calling `cleanup()` here should take care of that.

```Java
    @After
    public void tearDown() {
        System.out.println("\n=== TEARDOWN ===");
        cleanup();
        System.out.println("Teardown completed");
    }
```

### BrowserConfig class
Add this small class to the file, nested within the test class.
```Java
    private static class BrowserConfig {
        final String browserType;
        final String driverPath;
        final String binaryPath;
        
        BrowserConfig(String browserType, String driverPath, String binaryPath) {
            this.browserType = browserType;
            this.driverPath = driverPath;
            this.binaryPath = binaryPath;
        }
    }
```
### Finding HTML files
Take special note of the `findHtmlFile()` helper funciton. The location of the html files used in the lab should be in that list. 
```Java
    String[] possibleHtmlPaths = {
            "src/main/cat-facts.html",
            "cat-facts.html",
            "src/test/resources/cat-facts.html",
            "test-resources/cat-facts.html",
            "src/main/resources/cat-facts.html"
        };
```

### Helper Funcitons
There are a bunch of helper funcitons to add robustness and handle environments and edge cases a little more gracefully. We can jusst paste this all into the test class.

```Java
    private void printEnvironmentInfo() {
        System.out.println("=== ENVIRONMENT INFO ===");
        System.out.println("OS: " + OS_NAME + " (" + OS_ARCH + ")");
        System.out.println("Architecture: " + (IS_ARM ? "ARM64" : "x86/x64"));
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Working directory: " + System.getProperty("user.dir"));
    }

    private BrowserConfig detectBrowserAndDriver() {
        System.out.println("\n=== BROWSER AND DRIVER DETECTION ===");
        
        BrowserConfig projectDriverConfig = checkProjectDriverFolder();
        if (projectDriverConfig != null) {
            return projectDriverConfig;
        }
        
        BrowserConfig systemDriverConfig = checkSystemDrivers();
        if (systemDriverConfig != null) {
            return systemDriverConfig;
        }
        
        throw new RuntimeException("No compatible browser driver found");
    }
    
    private BrowserConfig checkProjectDriverFolder() {
        File driverFolder = new File("driver");
        if (!driverFolder.exists() || !driverFolder.isDirectory()) {
            System.out.println("No 'driver' folder found in project root");
            return null;
        }
        
        System.out.println("Found 'driver' folder, checking for executables...");
        
        String[] edgeDriverNames = IS_WINDOWS ? 
            new String[]{"msedgedriver.exe", "edgedriver.exe"} :
            new String[]{"msedgedriver", "edgedriver"};
            
        for (String driverName : edgeDriverNames) {
            File driverFile = new File(driverFolder, driverName);
            if (driverFile.exists()) {
                makeExecutable(driverFile);
                if (driverFile.canExecute()) {
                    System.out.println("Found Edge driver: " + driverFile.getAbsolutePath());
                    return new BrowserConfig("edge", driverFile.getAbsolutePath(), findEdgeBinary());
                }
            }
        }
        
        String[] chromeDriverNames = IS_WINDOWS ? 
            new String[]{"chromedriver.exe"} :
            new String[]{"chromedriver"};
            
        for (String driverName : chromeDriverNames) {
            File driverFile = new File(driverFolder, driverName);
            if (driverFile.exists()) {
                makeExecutable(driverFile);
                if (driverFile.canExecute()) {
                    System.out.println("Found Chrome driver: " + driverFile.getAbsolutePath());
                    return new BrowserConfig("chrome", driverFile.getAbsolutePath(), findChromeBinary());
                }
            }
        }
        
        System.out.println("No compatible drivers found in 'driver' folder");
        return null;
    }
    
    private BrowserConfig checkSystemDrivers() {
        System.out.println("Checking system-installed drivers...");
        
        String[] chromeDriverPaths = {
            "/usr/bin/chromedriver",
            "/usr/local/bin/chromedriver",
            "/snap/bin/chromedriver",
            System.getProperty("user.home") + "/.cache/selenium/chromedriver/linux64/chromedriver",
            "/opt/chromedriver/chromedriver"
        };
        
        if (IS_WINDOWS) {
            chromeDriverPaths = new String[]{
                "C:\\Program Files\\Google\\Chrome\\Application\\chromedriver.exe",
                "C:\\ChromeDriver\\chromedriver.exe",
                "chromedriver.exe"
            };
        }
        
        for (String driverPath : chromeDriverPaths) {
            File driverFile = new File(driverPath);
            if (driverFile.exists() && driverFile.canExecute()) {
                System.out.println("Found system Chrome driver: " + driverPath);
                return new BrowserConfig("chrome", driverPath, findChromeBinary());
            }
        }
        
        if (IS_WINDOWS) {
            String[] edgeDriverPaths = {
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedgedriver.exe",
                "msedgedriver.exe"
            };
            
            for (String driverPath : edgeDriverPaths) {
                File driverFile = new File(driverPath);
                if (driverFile.exists() && driverFile.canExecute()) {
                    System.out.println("Found system Edge driver: " + driverPath);
                    return new BrowserConfig("edge", driverPath, findEdgeBinary());
                }
            }
        }
        
        return null;
    }
    
    private String findChromeBinary() {
        String[] chromePaths;
        
        if (IS_WINDOWS) {
            chromePaths = new String[]{
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
            };
        } else if (IS_MAC) {
            chromePaths = new String[]{
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
            };
        } else {
            chromePaths = new String[]{
                "/usr/bin/chromium-browser",
                "/usr/bin/chromium",
                "/usr/bin/google-chrome",
                "/snap/bin/chromium"
            };
        }
        
        for (String path : chromePaths) {
            if (new File(path).exists()) {
                System.out.println("Found Chrome binary: " + path);
                return path;
            }
        }
        
        System.out.println("Chrome binary not found, using default");
        return null;
    }
    
    private String findEdgeBinary() {
        if (IS_WINDOWS) {
            String[] edgePaths = {
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe"
            };
            
            for (String path : edgePaths) {
                if (new File(path).exists()) {
                    System.out.println("Found Edge binary: " + path);
                    return path;
                }
            }
        }
        
        System.out.println("Edge binary not found, using default");
        return null;
    }
    
    private void makeExecutable(File file) {
        if (!file.canExecute()) {
            try {
                file.setExecutable(true);
                System.out.println("Made executable: " + file.getAbsolutePath());
            } catch (Exception e) {
                System.out.println("Could not make executable: " + e.getMessage());
            }
        }
    }
    
    private File findHtmlFile() {
        String[] possibleHtmlPaths = {
            "src/main/index.html",
            "index.html",
            "src/test/resources/index.html",
            "test-resources/index.html",
            "src/main/resources/index.html"
        };
        
        for (String htmlPath : possibleHtmlPaths) {
            File testFile = new File(htmlPath);
            if (testFile.exists()) {
                System.out.println("Found HTML file: " + testFile.getAbsolutePath());
                return testFile;
            }
        }
        
        throw new RuntimeException("Could not find index.html in any expected location: " + 
            Arrays.toString(possibleHtmlPaths));
    }
    
    private String determineHtmlUrl(File htmlFile) {
        if (isPython3Available()) {
            try {
                return startHttpServer(htmlFile);
            } catch (Exception e) {
                System.out.println("HTTP server failed, falling back to file URL: " + e.getMessage());
            }
        } else {
            System.out.println("Python3 not available, using file URL");
        }
        
        return "file://" + htmlFile.getAbsolutePath();
    }
    
    private boolean isPython3Available() {
        try {
            Process process = new ProcessBuilder("python3", "--version").start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                System.out.println("Python3 is available");
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }
        
        if (IS_WINDOWS) {
            try {
                Process process = new ProcessBuilder("python", "--version").start();
                boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    System.out.println("Python is available");
                    return true;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        System.out.println("Python3/Python not available");
        return false;
    }
    
    private String startHttpServer(File htmlFile) throws Exception {
        int port = 8000 + (int)(Math.random() * 1000);
        String directory = htmlFile.getParent();
        String fileName = htmlFile.getName();
        
        System.out.println("Starting HTTP server on port " + port);
        
        String pythonCmd = IS_WINDOWS ? "python" : "python3";
        ProcessBuilder pb = new ProcessBuilder(pythonCmd, "-m", "http.server", String.valueOf(port));
        pb.directory(new File(directory));
        pb.redirectErrorStream(true);
        
        httpServerProcess = pb.start();
        
        Thread.sleep(3000);
        
        if (!httpServerProcess.isAlive()) {
            throw new RuntimeException("HTTP server failed to start");
        }
        
        String url = "http://localhost:" + port + "/" + fileName;
        
        for (int i = 0; i < 10; i++) {
            try {
                java.net.URL testUrl = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) testUrl.openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                
                if (responseCode == 200) {
                    System.out.println("HTTP server ready: " + url);
                    return url;
                }
            } catch (Exception e) {
                if (i == 9) {
                    throw new RuntimeException("HTTP server not responding: " + e.getMessage());
                }
                Thread.sleep(1000);
            }
        }
        
        throw new RuntimeException("HTTP server failed to respond");
    }
    
    private WebDriver createWebDriver(BrowserConfig config) {
        System.out.println("\n=== CREATING WEBDRIVER ===");
        System.out.println("Browser: " + config.browserType);
        System.out.println("Driver: " + config.driverPath);
        System.out.println("Binary: " + config.binaryPath);
        
        if ("edge".equals(config.browserType)) {
            return createEdgeDriver(config);
        } else {
            return createChromeDriver(config);
        }
    }
    
    private WebDriver createChromeDriver(BrowserConfig config) {
        System.setProperty("webdriver.chrome.driver", config.driverPath);
        
        ChromeOptions options = new ChromeOptions();
        
        if (config.binaryPath != null) {
            options.setBinary(config.binaryPath);
        }
        
        options.addArguments(getChromeArguments());
        
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logPrefs);
        
        ChromeDriverService.Builder serviceBuilder = new ChromeDriverService.Builder()
            .usingDriverExecutable(new File(config.driverPath))
            .withTimeout(Duration.ofSeconds(30));
        
        ChromeDriverService service = serviceBuilder.build();
        
        return new ChromeDriver(service, options);
    }
    
    private WebDriver createEdgeDriver(BrowserConfig config) {
        System.setProperty("webdriver.edge.driver", config.driverPath);
        
        EdgeOptions options = new EdgeOptions();
        
        if (config.binaryPath != null) {
            options.setBinary(config.binaryPath);
        }
        
        options.addArguments(getEdgeArguments());
        
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("ms:loggingPrefs", logPrefs);
        
        EdgeDriverService.Builder serviceBuilder = new EdgeDriverService.Builder()
            .usingDriverExecutable(new File(config.driverPath))
            .withTimeout(Duration.ofSeconds(30));
        
        EdgeDriverService service = serviceBuilder.build();
        
        return new EdgeDriver(service, options);
    }
    
    private String[] getChromeArguments() {
        return getCommonBrowserArguments();
    }
    
    private String[] getEdgeArguments() {
        return getCommonBrowserArguments();
    }
    
    private String[] getCommonBrowserArguments() {
        String[] baseArgs = {
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-extensions",
            "--disable-web-security",
            "--allow-file-access-from-files",
            "--allow-running-insecure-content",
            "--user-data-dir=/tmp/browser-test-" + System.currentTimeMillis(),
            "--disable-features=TranslateUI,VizDisplayCompositor",
            "--disable-background-timer-throttling",
            "--disable-backgrounding-occluded-windows",
            "--disable-renderer-backgrounding"
        };
        
        if (IS_ARM) {
            String[] armArgs = {
                "--disable-features=VizDisplayCompositor",
                "--use-gl=swiftshader",
                "--disable-software-rasterizer"
            };
            
            String[] combined = new String[baseArgs.length + armArgs.length];
            System.arraycopy(baseArgs, 0, combined, 0, baseArgs.length);
            System.arraycopy(armArgs, 0, combined, baseArgs.length, armArgs.length);
            return combined;
        }
        
        return baseArgs;
    }
    
    private void printPageInfo() {
        System.out.println("Page title: " + webDriver.getTitle());
        System.out.println("Current URL: " + webDriver.getCurrentUrl());
        System.out.println("Page source length: " + webDriver.getPageSource().length());
    }
    
    private void stopHttpServer() {
        if (httpServerProcess != null) {
            try {
                System.out.println("Stopping HTTP server...");
                httpServerProcess.destroy();
                
                boolean terminated = httpServerProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                if (!terminated) {
                    httpServerProcess.destroyForcibly();
                }
                
                httpServerProcess = null;
                System.out.println("HTTP server stopped");
            } catch (Exception e) {
                System.out.println("Warning: Error stopping HTTP server: " + e.getMessage());
                try {
                    httpServerProcess.destroyForcibly();
                } catch (Exception ignored) {}
                httpServerProcess = null;
            }
        }
    }
    
    private void cleanup() {
        stopHttpServer();
        if (webDriver != null) {
            try {
                webDriver.quit();
                webDriver = null;
            } catch (Exception e) {
                System.err.println("Error cleaning up WebDriver: " + e.getMessage());
            }
        }
    }
```
