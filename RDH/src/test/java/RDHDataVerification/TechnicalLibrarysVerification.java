package RDHDataVerification;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Arrays;

public class TechnicalLibrarysVerification {
    public static void main(String[] args) throws InterruptedException {
        // Initialize WebDriver with Chrome options
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        driver.manage().window().maximize();

        // Read URLs from Excel file
        // The Excel file should contain a list of URLs with columns LiveURL and DevURL
        List<String[]> urls = readExcelData(".//Excel//Technical LibrarysURLList-RDH.xlsx");
        List<String[]> reportData = new ArrayList<>();
        reportData.add(new String[]{"Live URL", "Dev URL", "Live Content", "Status"});

        // Loop through each pair of Live and Dev URLs
        for (String[] urlPair : urls) {
            String liveUrl = urlPair[0];
            String devUrl = urlPair[1];
            System.out.println("\n🔎 Checking content from Live: " + liveUrl + " on Dev: " + devUrl);

            List<String> liveContent = extractContent(driver, liveUrl);
            checkContentInDev(driver, devUrl, liveContent, liveUrl, reportData);
        }

        // Save verification results to Excel
        saveResultsToExcel(reportData);
        driver.quit();
    }

    // Method to read data from an Excel file
    public static List<String[]> readExcelData(String filePath) {
        List<String[]> urlPairs = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                Cell liveUrlCell = row.getCell(0);
                Cell devUrlCell = row.getCell(1);
                if (liveUrlCell != null && devUrlCell != null) {
                    urlPairs.add(new String[]{liveUrlCell.getStringCellValue(), devUrlCell.getStringCellValue()});
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error reading Excel file: " + e.getMessage());
        }
        return urlPairs;
    }

    // Method to extract content from the Live URL
    public static List<String> extractContent(WebDriver driver, String url) throws InterruptedException {
        driver.get(url);
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("window.scrollBy(0,950)");
        Thread.sleep(1000);

        List<String> content = new ArrayList<>();
        
        // Define XPath expressions to extract relevant content
        List<String> xpaths = Arrays.asList(
                "//*[@class='columns-12 column-center content-column']/p",
                "//*[@class='columns-12 column-center content-column']//li", "//*[@class='columns-12 column-center content-column']//em",
                "//*[@class='columns-12 column-center content-column']//h1", "//*[@class='columns-12 column-center content-column']//h2",
                "//*[@class='columns-12 column-center content-column']//h3", "//*[@class='columns-12 column-center content-column']//h4"
        );

        for (String xpath : xpaths) {
            List<WebElement> elements = driver.findElements(By.xpath(xpath));
            for (WebElement element : elements) {
                content.add(cleanText(element.getText()));
            }
        }
        return content;
    }

    // Method to check if extracted content exists on the Dev URL
    public static void checkContentInDev(WebDriver driver, String devUrl, List<String> liveContent, String liveUrl, List<String[]> reportData) throws InterruptedException {
        driver.get(devUrl);
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("window.scrollBy(0,1050)");
        Thread.sleep(1000);
        String devPageText = cleanText(driver.getPageSource());

        for (String liveItem : liveContent) {
            if (devPageText.contains(liveItem)) {
                System.out.println("✅ Found content in Dev: " + liveItem);
                reportData.add(new String[]{liveUrl, devUrl, liveItem, "Found"});
            } else {
                System.out.println("❌ Content from Live NOT found in Dev:");
                System.out.println("   Missing Content: " + liveItem);
                reportData.add(new String[]{liveUrl, devUrl, liveItem, "Not Found"});
            }
        }
    }

    // Method to clean and normalize text for comparison
    public static String cleanText(String text) {
        if (text == null) return "";
        Document doc = Jsoup.parse(text);
        return doc.text().replaceAll("\\s+", " ").trim().toLowerCase();
    }

    // Method to save verification results to an Excel file
    public static void saveResultsToExcel(List<String[]> data) {
        if (data.size() <= 1) {
            System.out.println("⚠️ No data to write in the Excel file. Check if content is being found.");
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Blog Verification Report");
            int rowNum = 0;

            for (String[] rowData : data) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < rowData.length; i++) {
                    row.createCell(i).setCellValue(rowData[i]);
                }
            }

            //Update name for report data file
            File file = new File(".//Excel//Technical LibrarysURLList-RDHVerificationReport.xlsx");
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }

            System.out.println("📊 Results successfully saved to: " + file.getCanonicalPath());
        } catch (IOException e) {
            System.out.println("⚠️ Error saving Excel file: " + e.getMessage());
        }
    }
}
