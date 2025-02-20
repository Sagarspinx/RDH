package RDHDataVerification;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.*;
import java.time.Duration;
import java.util.Iterator;

public class Resource {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Setup ChromeDriver using WebDriverManager
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Define Excel file path?
        String excelFilePath = ".//Excel//Projects.xlsx";
        FileInputStream fis = new FileInputStream(excelFilePath);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);
        fis.close();

        // Iterate over each row in Excel (excluding header)
        Iterator<Row> rowIterator = sheet.iterator();
        rowIterator.next(); // Skip header row

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String url = getCellValue(row.getCell(52)); // Column BA (52 index in zero-based count?)

            if (url.isEmpty()) {
                System.out.println("Skipping empty URL row...");
                continue;
            }

            // Open the page URL from Excel
            driver.get(url);
            System.out.println("Opened URL: " + url);

            // Verify data in specific columns and update status columns
            for (int i = 0; i < row.getLastCellNum(); i += 2) {
                Cell dataCell = row.getCell(i);
                Cell statusCell = row.getCell(i + 1);

                if (dataCell == null || getCellValue(dataCell).isEmpty()) continue;

                // Ensure the status cell exists
                if (statusCell == null) {
                    statusCell = row.createCell(i + 1, CellType.STRING);
                }

                String columnName = sheet.getRow(0).getCell(i).getStringCellValue();
                String expectedValue = getCellValue(dataCell);
                String xpath = getXPathForColumn(columnName);

                // Wait 2 seconds before each section verification
                Thread.sleep(2000);
                
                boolean result = verifyContent(driver, xpath, expectedValue);
                setCellStatus(statusCell, result);

                System.out.println("Verified: " + columnName + " -> " + expectedValue + " | Found: " + result);
                
                // Save the Excel file after each cell update
                saveExcelFile(workbook, excelFilePath);
            }
        }

        // Close the browser
        driver.quit();
        workbook.close();
        System.out.println("Verification Completed!");
    }

    // Get text from an Excel cell
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return "";
        }
    }

    // Set cell status
    private static void setCellStatus(Cell cell, boolean status) {
        if (cell == null) return;
        cell.setCellValue(status ? "Found" : "Not Found");
    }

    // Save the Excel file
    private static void saveExcelFile(Workbook workbook, String filePath) throws IOException {
        FileOutputStream fos = new FileOutputStream(filePath);
        workbook.write(fos);
        fos.close();
    }

    // Verify content using defined XPath
    private static boolean verifyContent(WebDriver driver, String xpath, String expectedValue) {
        if (xpath.isEmpty()) return false;
        try {
            return driver.findElement(By.xpath(xpath)).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    // Define XPaths for each section manually?
    private static String getXPathForColumn(String columnName) {
        switch (columnName.toLowerCase()) {
            case "title": return "//*[@class='h2 m-0 mb-2']";
            case "excerpt": return "//*[@class='fw-light mt-5 mt-lg-0 mb-5']";
            case "date": return "//*[@class='d-block text-18']";
            case "page banner sub text": return "//*[@class='d-block text-18']";
            case "image featured name": return "//*[@class='project-hero p-0']/img";
            case "image name1":
            case "image name2":
            case "image name3":
            case "image name4":
            case "image name5":
            case "image name6": return "//*[@class='slick-track']/*/img";
            case "intro title": return "//*[@class='fw-light mt-5 mt-lg-0 mb-5']";
            case "info content": return "//*[@id='collapseExample']";
            case "related case studies": return "(//*[@class='card-data'])[1]";
            case "related case studies1": return "(//*[@class='card-data'])[2]";
            case "related case studies2": return "(//*[@class='card-data'])[3]";
            case "case study info_title1":
            case "case study info_title2":
            case "case study info_title3":
            case "case study info_title4":
            case "case study info_title5":
            case "case study info_content1":
            case "case study info_content2":
            case "case study info_content3":
            case "case study info_content4":
            case "case study info_content5": return "//*[@class='list-unstyled p-0 mb-5 text-16']";
            default: return "//*[contains(text(), '" + columnName + "')]";
        }
    }
}
