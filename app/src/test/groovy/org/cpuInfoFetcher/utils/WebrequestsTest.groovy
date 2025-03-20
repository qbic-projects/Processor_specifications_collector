/* groovylint-disable Indentation, MethodName */
package org.cpuinfofetcher.utils

import org.cpuinfofetcher.utils.*

import org.jsoup.nodes.Element
import org.jsoup.nodes.Document

import org.dflib.DataFrame

import spock.lang.Specification

/**
 * Test Webrequests
 */
class WebrequestsTest extends Specification {

    private final String exampleURL = 'https://example.com/'
    private final String exampleTableURL = 'https://www.htmltables.io/blog/beginners-guide-to-html-tables'


    def 'html scraping of standard website'() {
        setup:
        HTMLScraper scraper = new HTMLScraper()
            String xPath_query = './/a'

        when:
            Element queryResults = scraper.scrape(this.exampleURL, xPath_query).first()

        then:
            queryResults.text() == 'More information...'
    }

    def 'scraping table'() {
        setup:
            HTMLScraper scraper = new HTMLScraper()
            String xPath_query = './/table'

        when:
            Element table = scraper.scrape(this.exampleTableURL, xPath_query).first()
            DataFrame df = scraper.parse_table(table)

        then:
            df.getColumnsIndex().toList() == ['Team', 'Sport', 'City']
    }


    def 'interactive html scraping of standard website'() {
        setup:
            InteractiveHTMLScraper scraper = new InteractiveHTMLScraper()
            String xPath_reject = null
            String xPath_query = './/a'

        when:
            Document queryResults = scraper.download(this.exampleURL, xPath_reject, xPath_query)
            Element firstHeader = queryResults.selectXpath('.//h1').first()

        then:
            firstHeader.ownText() == 'Example Domain'

        cleanup:
            scraper.quit()
    }

}
