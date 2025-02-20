/* groovylint-disable Indentation, MethodName */
package org.cpuinfofetcher

import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import spock.lang.Specification

/**
 * Test Webrequests
 */
class WebrequestsTest extends Specification {

    private final String example_domain = 'https://example.com/'

    private HTMLScraper scraper

    def 'html scraping of standard website'() {
        setup:
            this.scraper = new HTMLScraper()
            String xPath_query = './/a'

        when:
            Element queryResults = this.scraper.scrape(this.example_domain, xPath_query).first()

        then:
            queryResults.text() == 'More information...'
    }

    def 'interactive html scraping of standard website'() {
            setup:
                this.scraper = new InteractiveHTMLScraper('.')
                String xPath_reject = null
                String xPath_query = './/a'

            when:
                Document queryResults = this.scraper.scrape(this.example_domain, xPath_reject, xPath_query).first()
                Element firstHeader = queryResults.selectXpath('.//div[@class="help-article"]/h*').first()

            then:
                firstHeader.text() == 'Example Domains'
        }

}
