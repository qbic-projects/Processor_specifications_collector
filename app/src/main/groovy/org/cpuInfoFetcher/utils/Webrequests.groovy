package org.cpuinfofetcher

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import java.time.Duration

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.Connection
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import org.dflib.DataFrame

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.WebElement
import org.openqa.selenium.NoSuchElementException

import org.openqa.selenium.support.ui.Wait
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.JavascriptExecutor

/**
 * Scrape the web for elements
 * @author Josua Carl
 */
class HTMLScraper {

    Logger logger = Logger.getLogger('')

    // Intel is particularly wary of access via script. Be awsare, that you may be blocked for accessing too many sites
    // too quickly and that you have to fake a convincing User-Agent to pass as a human actor
    String userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:135.0) Gecko/20100101 Firefox/135.0'
    Connection session = Jsoup.newSession()
        .userAgent(this.userAgent)
        .header('Accept', '*/*')
        .header('Accept-Language', 'en-US,en;q=0.5')

    Elements scrape(String request_url, String xPath_query) {
        Connection connection = session.newRequest(request_url)
        Document doc = connection.get()

        Elements processor_elements = doc.selectXpath(xPath_query)

        return processor_elements
    }

    DataFrame parse_table(Element table){
        // Extract column headers
        Elements headers = table.selectXpath('.//thead/tr/th')
        List<String> columnNames = []
        for (Element head : headers) {
            columnNames.add(head.text())
        }

        // Extract info
        Elements rows = table.selectXpath('.//tbody/tr')
        def df = DataFrame.byArrayRow(*columnNames).appender()
        List<String> row_info = []
        for (Element row : rows) {
            row_info = []
            for (Element data : row.selectXpath('./td')) {
                row_info.add(data.text())
            }
            df.append(*row_info)
        }
        df = df.toDataFrame()

        return df
    }

}

/**
 * Scrape the web for elements requiring interactions
 * @author Josua Carl
 */
class InteractiveHTMLScraper extends HTMLScraper {

    Map<String, Object> preferences = [:]
    ChromeOptions driverOptions = new ChromeOptions()
    WebDriver driver

    InteractiveHTMLScraper(String download_directory) {
        this.preferences.put('profile.default_content_settings.popups', 0)
        this.preferences.put('download.default_directory', download_directory)

        this.driverOptions.setExperimentalOption('prefs', this.preferences)
        this.driverOptions.addArguments('--headless=new')
        this.driverOptions.addArguments('--disable-gpu')
        this.driverOptions.addArguments('--window-size=1920,1080')
        String uaStr = '--user-agent='  + this.userAgent
        this.driverOptions.addArguments(uaStr)

        this.driver = new ChromeDriver(driverOptions)
    }

    private void waitForPageLoad(WebDriver webDriver) {
        Wait<WebDriver> wait = new WebDriverWait(webDriver, Duration.ofSeconds(10))
        wait.until(webDriver1 ->
            ((JavascriptExecutor) webDriver).executeScript('return document.readyState') == 'complete'
        )
        // Wait for animations to complete
        TimeUnit.SECONDS.sleep(1)
    }

    Document scrape(String request_url, String xPath_reject, String xPath_query) {
        this.driver.get(request_url)
        waitForPageLoad(this.driver)

        try {
            WebElement reject_cookies_element = this.driver.findElement(By.xpath(xPath_reject))
            reject_cookies_element.click()
        } catch ( NoSuchElementException e) {
            logger.info('Cookie banner not displayed.')
        }

        WebElement element = this.driver.findElement(By.xpath(xPath_query))
        element.click()

        Document doc = Jsoup.parse(this.driver.getPageSource())
        driver.quit()
        return doc
    }

}
