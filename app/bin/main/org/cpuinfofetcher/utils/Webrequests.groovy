package org.cpuinfofetcher

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.Connection
import org.jsoup.select.Elements

/**
 * Scrape the web for elements
 * @author Josua Carl
 */
class HTMLScraper {

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

}

class JSONScraper {

    def mock

}
