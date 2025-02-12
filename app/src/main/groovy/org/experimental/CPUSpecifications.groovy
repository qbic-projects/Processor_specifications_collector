package org.experimental

import java.io.File

import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.CompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Callable

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.Connection
import org.jsoup.HttpStatusException

import me.tongfei.progressbar.ProgressBar

public interface Specifications_Fetcher {

    void main(String[] args)

}

class AMD_Specifications_Fetcher implements Specifications_Fetcher {

    @Override
    void main(String[] args) {
        println('Not implemented')
    }

}

class Intel_Specifications_Fetcher implements Specifications_Fetcher {

    // Constructor
    int upperBoundaryIDs
    int lowerBoundaryIDs
    Intel_Specifications_Fetcher(int lowerBoundaryIDs, int upperBoundaryIDs){
        this.upperBoundaryIDs = upperBoundaryIDs
        this.lowerBoundaryIDs = lowerBoundaryIDs
    }

    // Execution instances
    ProgressBar progressBar = new ProgressBar('Attempted IDs:', upperBoundaryIDs - lowerBoundaryIDs)
    ExecutorService threadPool = Executors.newFixedThreadPool(256)
    CompletionService<String> executionService = new ExecutorCompletionService<>(threadPool)

    // Extract addresses from main ark
    List<String> fetch_processor_family_addresses(String request_url, String snapshot_path) {
        Connection session = Jsoup.newSession() // 'https://www.intel.com/content/www/us/en/ark.html'
        Document doc
        try {
            Connection connection = session.url(request_url)
                                        .userAgent('Mozilla/5.0 \
                                        (Macintosh; Intel Mac OS X 10.15; rv:135.0) Gecko/20100101 Firefox/135.0')
                                        .header('Accept', '*/*')
                                        .header('Accept-Language', 'en-US,en;q=0.5')
            doc = connection.get()
        } catch (HttpStatusException e) {
            File doc_snapshot = new File(snapshot_path) //'../resources/assets/Intel_Product_Specifications.html'
            doc = Jsoup.parse(doc_snapshot, 'UTF-8', request_url);
        }
        // divs with class "products processors" -> "a" elements with hrefs containing "processor" -> hrefs
        String xPath = '//div[@class=\'products processors\']//a[contains(@href, "processor")]'
        List<String> processor_elements = doc.selectXpath(xPath).eachAttr('abs:href')
        return processor_elements
    }

    // Extract addresses from main ark
    List<String> fetch_processor_addresses(String request_url) {
        Connection session = Jsoup.newSession()
        Document doc
        try {
            Connection connection = session.url(request_url)
                                        .userAgent('Mozilla/5.0 \
                                        (Macintosh; Intel Mac OS X 10.15; rv:135.0) Gecko/20100101 Firefox/135.0')
                                        .header('Accept', '*/*')
                                        .header('Accept-Language', 'en-US,en;q=0.5')
            doc = connection.get()
        } catch (HttpStatusException e) {
            File doc_snapshot = new File('../resources/assets/Intel_Product_Specifications.html')
            doc = Jsoup.parse(doc_snapshot, 'UTF-8', request_url);
        }
        // divs with class "products processors" -> "a" elements with hrefs containing "processor" -> hrefs
        String xPath = '//div[@class=\'products processors\']//a[contains(@href, "processor")]'
        List<String> processor_elements = doc.selectXpath(xPath).eachAttr('abs:href')
        return processor_elements
    }

    // Single run method
    Void fetch_specification(String product_id) {
        def request_url = "https://intel.com/content/www/us/en/ark/products/${product_id}/C.html"
        Connection connection = Jsoup.connect(request_url)
        def response = connection.response()
        if (response.statusCode() == 200) {
            connection.headerFields.each { key, value ->
                println "$key: $value"
            }
        }
        this.progressBar.setExtraMessage("ID:${product_id} R:${response.statusCode()}")
        connection.disconnect()
        this.progressBar.step()

        return null
    }

    // Single run Callable
    class FetchSpecifications implements Callable<Void> {

        String i
        FetchSpecifications(String i) {
            this.i  = i
        }

        @Override
        Void call() { return fetch_specification(this.i) }

    }

    // Execution list creator
    List<FetchSpecifications> fetch_specifications() {
        List<FetchSpecifications> callables = []
        for (int i = this.lowerBoundaryIDs; i < this.upperBoundaryIDs; i++) {
            FetchSpecifications callable = new FetchSpecifications(Integer.toString(i))
            callables.add(callable)
        }

        return callables
    }

    // Main (invoker)
    @Override
    void main(String[] args) {
        List<FetchSpecifications> callables = fetch_specifications()
        threadPool.invokeAll(callables)
    }

}
