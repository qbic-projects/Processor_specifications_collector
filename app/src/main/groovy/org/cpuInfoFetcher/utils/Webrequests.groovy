package org.cpuInfoFetcher

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.Connection
import org.jsoup.nodes.Elements

/**
 * Scrape the web for elements
 * @author Josua Carl
 */
class HTMLScraper{

    Connection session = Jsoup.newSession() // 'https://www.intel.com/content/www/us/en/ark.html'
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