package RDHDataVerification;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CaseStudiesDataMigrationVerification {
    public static void main(String[] args) {
        // Set up WebDriver
        System.setProperty("webdriver.chrome.driver", ".//Browser//chromedriver_132.exe");
        WebDriver driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();

        // Define URLs for Live and Dev
        String liveURL = "https://www.rdh.com/our-case-studies/crystallis";
        String devURL = "https://php2.spinxweb.net/rdh-final/our-case-studies/crystallis";

        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));

        // Fetch data from Live website
        driver.get(liveURL);
        String liveTitle = getTitle(wait);
        String liveDescription = getDescription(driver, "//*[@class='columns-8']");
        String liveImageNames = getImageNames(driver, wait, "//div[contains(@class, 'flex-image-wrapper')]/img[@src]");

        // Fetch data from Dev website
        driver.get(devURL);
        String devTitle = getTitle(wait);
        String devDescription = getDescription(driver, "//*[@class='col-lg-8']");
        String devImageNames = getImageNames(driver, wait, "//*[@class='slick-track']//img");

        // Compare Data
        System.out.println("Title Match: " + liveTitle.equals(devTitle));
        System.out.println("Description Match: " + liveDescription.equals(devDescription));
        System.out.println("Images Match: " + liveImageNames.equals(devImageNames));

        // Print the values for debugging
        System.out.println("Live Title: " + liveTitle);
        System.out.println("Dev Title: " + devTitle);
        System.out.println("Live Description: " + liveDescription);
        System.out.println("Dev Description: " + devDescription);
        System.out.println("Live Images: " + liveImageNames);
        System.out.println("Dev Images: " + devImageNames);

        // Close browser
        driver.quit();
    }

    // Method to fetch the title
    private static String getTitle(WebDriverWait wait) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h1"))).getText();
    }

    // Method to fetch the description
    private static String getDescription(WebDriver driver, String xpath) {
        WebElement descriptionElement = driver.findElement(By.xpath(xpath));
        return (descriptionElement != null) ? descriptionElement.getText() : "No Description Found";
    }

    // Method to fetch image names
    private static String getImageNames(WebDriver driver, WebDriverWait wait, String imageXpath) {
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath(imageXpath)));
        List<WebElement> images = driver.findElements(By.xpath(imageXpath));

        if (images.isEmpty()) return "No Images Found";

        StringBuilder imageNames = new StringBuilder();
        for (WebElement img : images) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", img);
            String imageName = img.getAttribute("src");
            imageNames.append(imageName.substring(imageName.lastIndexOf("/") + 1)).append(", ");
        }
        return imageNames.toString();
    }
}
