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

    Connection session = Jsoup.newSession() 
    // 'https://www.intel.com/content/www/us/en/ark.html'
    // "https://intel.com/content/www/us/en/ark/products/${product_id}/C.html"

    // Intel is particularly wary of access via script. Be aware, that you may be blocked for accessing too many sites
    // too quickly and that you have to fake a convincing User-Agent to pass as a human actor
    String userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:135.0) Gecko/20100101 Firefox/135.0'

    Elements scrape(String request_url, String xPath_query) {
        Connection connection = session.url(request_url)
                                    .userAgent(this.userAgent)
                                    .header('Accept', '*/*')
                                    .header('Accept-Language', 'en-US,en;q=0.5')
        Document doc = connection.get()

        Elements processor_elements = doc.selectXpath(xPath_query)

        return processor_elements
    }

}

class JSONScraper {

    def mock

}
