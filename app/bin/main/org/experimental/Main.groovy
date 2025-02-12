package org.experimental

import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import org.jsoup.Connection
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.jsoup.HttpStatusException

class Main {

    static void main(String[] args) {
        def test = new Try()
        test.main()

    //def isf = new Intel_Specifications_Fetcher(236773, 236774)
    //isf.main()
    }

}

class Try {

    protected Connection session = Jsoup.newSession()

    void main() {
        def request_url = 'https://www.intel.com/content/www/us/en/ark.html' //'https://www.google.com'
        //'https://intel.com/content/www/us/en/ark/products/236773/C.html'
        Connection connection = this.session
                                    .url(request_url)
                                    .userAgent('Mozilla/5.0 \
                                    (Macintosh; Intel Mac OS X 10.15; rv:135.0) Gecko/20100101 Firefox/135.0')
                                    .header('Accept', '*/*')
                                    .header('Accept-Language', 'en-US,en;q=0.5')
        try {
            Document doc = connection.get()
            // divs with class "products processors" -> "a" elements with hrefs containing "processor" -> hrefs
            String xPath = '//div[@class=\'products processors\']//a[contains(@href, "processor")]'
            List<String> processor_elements = doc.selectXpath(xPath).eachAttr("abs:href")
            println processor_elements
        } catch (HttpStatusException e) {
            println('HTTPS Status Exception')
        }
    }

}