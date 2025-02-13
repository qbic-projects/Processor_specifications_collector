package org.cpuinfofetcher

import java.nio.file.Path
import java.nio.file.Paths

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import java.util.concurrent.Future

import org.jsoup.HttpStatusException
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import me.tongfei.progressbar.ProgressBar

import org.dflib.DataFrame
import org.dflib.Printers
import org.dflib.csv.Csv

public class Specifications_Fetcher {

    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
    LocalDateTime localTime = LocalDateTime.now()

}

class AMD_Specifications_Fetcher extends Specifications_Fetcher {

    void main() {
        println('Not implemented')
    }

}

class Intel_Specifications_Fetcher extends Specifications_Fetcher {

    // Scraper
    HTMLScraper scraper = new HTMLScraper()

    // Execution instances
    ProgressBar progressBar
    ExecutorService threadPool = Executors.newFixedThreadPool(256)


    // Extract processor family urls from main ark
    DataFrame fetch_processor_family_urls(String url, String snap_path) {
        def df = DataFrame
            .byArrayRow('name', 'url', 'time', 'source')
            .appender()
        try {
            // divs with class "products processors" -> "a" elements with hrefs containing "processor"
            String xPath_query = '//div[@class=\'products processors\']//a[contains(@href, "processor")]'
            Elements elements = this.scraper.scrape(url, xPath_query)

            // Make data matrix
            for (Element element : elements) {
                df.append(
                    element.ownText(),
                    element.attr('abs:href'),
                    timeFormat.format(this.localTime.now()),
                    url
                )
            }
            df = df.toDataFrame()

            // Save info
            Csv.save(df, snap_path)
        } catch (HttpStatusException e) {
            df = Csv.load(snap_path)
        }

        return df
    }


    // Extract processor urls from main ark
    DataFrame fetch_processor_urls(DataFrame family_url, String snap_path) {
        String url = family_url.cols('url').rows(0).select()
        def df = DataFrame
            .byArrayRow('product_id', 'name', 'url', 'time', 'source')
            .appender()
        try {
            // table with id "product-table" -> table body -> table rows
            String xPath_query = '//table[@id=\'product-table\']//tbody/tr'
            Elements elements = this.scraper.scrape(url, xPath_query)

            // Make data matrix
            for (Element element : elements) {
                String product_id = element.attr('abs:data-product-id')
                // table data with data-component "arkproductlink" -> "a" elements
                xPath_query = '//td[@data-component=\'arkproductlink\']//a'
                Element link_element = element.selectXpath(xPath_query).first()
                df.append(
                    product_id
                    link_element.ownText(),
                    link_element.attr('abs:href'),
                    timeFormat.format(this.localTime.now()),
                    url
                )
            }
            df = df.toDataFrame()

            // Save info
            Csv.save(df, snap_path)
        } catch (HttpStatusException e) {
            df = Csv.load(snap_path)
        }

        println(Printers.tabular.toString(df))
        return df
    }


    // Single run method
    DataFrame fetch_processor_specification(DataFrame processor_url, String snap_path) {
        try {
            // divs with class "products processors" -> "a" elements with hrefs containing "processor" -> hrefs
            String xPath_query = '//div[contains(@id, "spec")]//div[contains(@class, "tech-section")]'
            String url = processor_url.cols('url').rows(0).select()
            println(Printers.tabular.toString(url))
            Elements elements = this.scraper.scrape(url, xPath_query)

            // Extract data
            List<String> labels = []
            List<String> data = []
            for (Element element : elements) {
                xPath_query = '//div[contains(@class, "tech-label")]'
                Element label_element = element.selectXpath(xPath_query).first()
                xPath_query = '//div[contains(@class, "tech-data")]'
                Element data_element = element.selectXpath(xPath_query).first()

                labels.add(label_element.ownText())
                data.add(data_element.ownText())
            }

            // Merge info into url row
            DataFrame df = processor_url
                .cols('time', 'source', *labels)
                .merge(
                    timeFormat.format(this.localTime.now()),
                    processor_url['url'],
                    *data
                )

            // Save info
            Csv.save(df, snap_path)
        } catch (HttpStatusException e) {
            df = Csv.load(snap_path)
        }

        this.progressBar.step()

        println(Printers.tabular.toString(df))
        return df
    }

    // Single run Callable
    class FetchSpecification implements Callable<Void> {

        DataFrame processor_url
        String snap_path

        FetchSpecification(DataFrame processor_url, String snap_path) {
            this.processor_url = processor_url
            this.snap_path = snap_path
        }

        @Override
        Void call() { return fetch_specification(this.processor_url, this.snap_path) }

    }

    Map<String, String[]> specification_aliases = [
        'tgb': ['Processor Base Power', 'tgb', 'thermal design power', 'Scenario Design Power', 'SDP'],
        'cores': ['cores'],
        'threads': ['threads']
    ]


    // Execution list creator
    DataFrame fetch_specifications(DataFrame processor_urls, String snap_path) {
        // Get calls
        List<FetchSpecification> callables = []
        for (int i = this.lowerBoundaryIDs; i < this.upperBoundaryIDs; i++) {
            callables.add(
                new FetchSpecification(
                    processor_urls,
                    snap_path
                )
            )
        }

        // Invoke calls
        List<Future> futures = this.threadPool.invokeAll(callables)

        // Save results
        def specifications = DataFrame
            .byArrayRow('product_id', 'name', 'time', 'source', 'tdb', 'cores', 'threads')
            .appender()
        for (Future future : futures) {
            // Extract df from future
            df = future.get()

            // Find columns with desired info
            List<String> matched_cols = []
            specification_aliases.each { specification_key, aliases ->
                matched_cols.add(
                    df.getColumnsIndex().toArray().find { col_name ->
                        aliases.any { alias -> col_name.toLowerCase().contains(alias.toLowerCase()) }
                    }
                )
            }

            // Add specification
            specifications.append(
                *df.cols('product_id', 'name', 'time', 'source', *matched_cols),
            )
        }
        specifications = specifications.toDataFrame()

        Csv.save(df, snap_path)

        return specifications
    }


    // Main (invoker)
    void main() {
        String script_path = getClass().protectionDomain.codeSource.location.path
        Path snap_path = Paths.get(script_path, '..', '..', '..', 'resources', 'main', 'assets')
            .toAbsolutePath()
            .normalize()
        // Collect Processor family URLsp
        DataFrame family_urls = fetch_processor_family_urls(
            'https://www.intel.com/content/www/us/en/ark.html',
            snap_path.resolve('Intel_family_info.csv').toString(),
        )

        // Collect processor URLs
        List<DataFrame> processor_urls = []
        family_urls.rows().each { family_url ->
        // TODO: CONTINUE DEBUGGING HERE
            println(Printers.tabular.toString(family_url))
            String snap_name = family_url
                                    .rows(0)
                                    .cols('name')
                                    .select()
                                    .replace(' ', '_')
                                    .replaceAll('[^a-zA-Z0-9 ]+', '')
                                    + '.csv'
            processor_urls.add(
                fetch_processor_urls(
                    family_url,
                    snap_path.resolve(snap_name).toString(),
                )
            )
        }

        this.progressBar = new ProgressBar('Attempted IDs:', processor_urls.size())
        // Collect processor specifictations
        fetch_specifications(
            processor_urls,
            snap_path.resolve('Intel_processor_specifications.csv').toString(),
        )
    }

}
