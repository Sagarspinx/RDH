package RDHDataVerification;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.*;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public class AuthorandVideo {
    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);
        
        List<String> urls = readURLsFromExcel("ResourceURLListLive.xlsx");
        List<Data> results = new ArrayList<>();
        
        for (String url : urls) {
            results.add(scrapeData(driver, url));
        }
        
        driver.quit();
        
        // Save results to Excel
        writeDataToExcel(results, "Report.xlsx");
    }
    
    public static List<String> readURLsFromExcel(String filePath) {
        List<String> urls = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                Cell cell = row.getCell(0);
                if (cell != null) {
                    urls.add(cell.getStringCellValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urls;
    }
    
    public static Data scrapeData(WebDriver driver, String url) {
        driver.get(url);
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        
        List<Author> authors = new ArrayList<>();
        try {
            List<WebElement> authorElements = driver.findElements(By.cssSelector("div.author"));
            for (WebElement authorElement : authorElements) {
                String name = authorElement.findElement(By.cssSelector("span.name")).getText();
                String position = authorElement.findElement(By.cssSelector("span.position")).getText();
                authors.add(new Author(name, position));
            }
        } catch (Exception e) {
            System.out.println("Error extracting authors: " + e.getMessage());
        }
        
        String videoUrl = null;
        try {
            WebElement videoElement = driver.findElement(By.tagName("video"));
            videoUrl = videoElement.getAttribute("src");
        } catch (NoSuchElementException e) {
            System.out.println("No video found on this page.");
        }
        
        return new Data(url, authors, videoUrl);
    }
    
    public static void writeDataToExcel(List<Data> data, String filePath) {
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(new File(filePath))) {
            Sheet sheet = workbook.createSheet("Scraped Data");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("URL");
            headerRow.createCell(1).setCellValue("Authors");
            headerRow.createCell(2).setCellValue("Video URL");
            
            int rowNum = 1;
            for (Data entry : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.url);
                row.createCell(1).setCellValue(entry.authors.toString());
                row.createCell(2).setCellValue(entry.videoUrl != null ? entry.videoUrl : "No Video");
            }
            workbook.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Author {
    String name;
    String position;
    
    public Author(String name, String position) {
        this.name = name;
        this.position = position;
    }
    
    @Override
    public String toString() {
        return name + " (" + position + ")";
    }
}

class Data {
    String url;
    List<Author> authors;
    String videoUrl;
    
    public Data(String url, List<Author> authors, String videoUrl) {
        this.url = url;
        this.authors = authors;
        this.videoUrl = videoUrl;
    }
    
    @Override
    public String toString() {
        return "URL: " + url + ", Authors: " + authors + ", Video URL: " + (videoUrl != null ? videoUrl : "No Video");
    }
}
