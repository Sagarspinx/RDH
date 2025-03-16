package RDHResource;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
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

public class ResourcePhara {
    public static void main(String[] args) {
        // Setup WebDriver with options
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();

        // Read URLs from Excel file
        List<String[]> urls = readExcelData(".//Excel//URLs-Resource.xlsx");
        List<String[]> reportData = new ArrayList<>();
        reportData.add(new String[]{"Live URL", "Dev URL", "Live Paragraph", "Status"});

        for (String[] urlPair : urls) {
            String liveUrl = urlPair[0];
            String devUrl = urlPair[1];

            System.out.println("\n🔎 Checking paragraphs from Live: " + liveUrl + " on Dev: " + devUrl);
            //Update live path incase for other project
            List<String> liveParagraphs = extractParagraphs(driver, liveUrl, "//*[@class='columns-12 column-center content-column']/p");
            List<String> liveBulletes = extractParagraphs(driver, liveUrl, "//*[@class='columns-12 column-center content-column']/*/li");

            checkParagraphsInDev(driver, devUrl, liveParagraphs, liveUrl, reportData);
        }

        // Save results to Excel
        saveResultsToExcel(reportData);
        driver.quit();
    }

    // Read Live and Dev URLs from Excel
    public static List<String[]> readExcelData(String filePath) {
        List<String[]> urlPairs = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header
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

    // Extract all paragraphs from Live site based on the provided XPath
    public static List<String> extractParagraphs(WebDriver driver, String url, String xpath) {
        driver.get(url);
        List<WebElement> paragraphs = driver.findElements(By.xpath(xpath));
        return paragraphs.stream()
                .map(WebElement::getText)
                .map(ResourcePhara::cleanText)
                .collect(java.util.stream.Collectors.toList());
    }

    // Check if each paragraph from Live exists anywhere on the Dev page and store results
    public static void checkParagraphsInDev(WebDriver driver, String devUrl, List<String> liveParagraphs, String liveUrl, List<String[]> reportData) {
        driver.get(devUrl);
        String devPageText = cleanText(driver.getPageSource());

        for (String liveParagraph : liveParagraphs) {
            if (devPageText.contains(liveParagraph)) {
                System.out.println("✅ Found paragraph in Dev: " + liveParagraph);
                reportData.add(new String[]{liveUrl, devUrl, liveParagraph, "Found"});
            } else {
                System.out.println("❌ Paragraph from Live NOT found in Dev:");
                System.out.println("   Missing Paragraph: " + liveParagraph);
                reportData.add(new String[]{liveUrl, devUrl, liveParagraph, "Not Found"});
            }
        }
    }

    // Normalize text by removing extra spaces, HTML tags, and special characters
    public static String cleanText(String text) {
        if (text == null) return "";
        Document doc = Jsoup.parse(text);
        return doc.text().replaceAll("\\s+", " ").trim().toLowerCase();
    }

    // Save results to an Excel file
    public static void saveResultsToExcel(List<String[]> data) {
        if (data.size() <= 1) {
            System.out.println("⚠️ No data to write in the Excel file. Check if paragraphs are being found.");
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

            File file = new File(".//Excel//BlogVerificationReport.xlsx");
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }

            System.out.println("📊 Results successfully saved to: " + file.getCanonicalPath());
        } catch (IOException e) {
            System.out.println("⚠️ Error saving Excel file: " + e.getMessage());
        }
    }
}
