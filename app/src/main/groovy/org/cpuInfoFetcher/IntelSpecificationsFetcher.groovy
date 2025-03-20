package org.cpuinfofetcher

import org.cpuinfofetcher.utils.HTMLScraper
import org.cpuinfofetcher.utils.InteractiveHTMLScraper

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

import java.time.temporal.ChronoUnit

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import java.util.concurrent.Future

import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.jsoup.nodes.Document

import me.tongfei.progressbar.ProgressBar

import org.dflib.DataFrame
import org.dflib.JoinType
import org.dflib.csv.Csv

/**
 * Fetch Intel specifications of processors
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class IntelSpecificationsFetcher extends SpecificationsFetcher {

    int numThreads = 1
    ProgressBar progressBar
    ExecutorService threadPool = Executors.newFixedThreadPool(numThreads)
    Path snap_path
    HTMLScraper scraper = new HTMLScraper()
    IntelSpecificationsFetcher(int days_until_update, int numThreads, Path snap_path=null) {
        this.numThreads = numThreads
        this.days_until_update = days_until_update

        // Add path for assets
        if (snap_path == null) {
            String script_path = getClass().protectionDomain.codeSource.location.path
            this.snap_path = Paths.get(script_path, '..', '..', '..', '..', '..', 'snapshots', 'Intel')
                .toAbsolutePath()
                .normalize()
        } else {
            this.snap_path = snap_path
        }
        Files.createDirectories(this.snap_path)

        this.progressBar = new ProgressBar('Waiting', 1)
    }


    // Extract processor family urls from main ark
    protected DataFrame fetch_processor_family_urls(String url, Path snap_path, Map<String, String> intended_usage_map) {
        snap_path = snap_path.resolve('Intel_family_info.csv') // sets path for saving the snapshot

        // Get snapshot & Update time
        def df = check_snap(snap_path, [*this.standard_cols, 'url'])
        int days_since_update = check_last_update(df, ChronoUnit.DAYS)

        if (days_since_update > this.days_until_update || days_since_update < 0) {
            // divs with class "products processors" -> "a" elements with hrefs containing "processor"
            String xPath_query = './/div[@class=\'products processors\']//a[contains(@href, "processor")]'
            Elements elements = this.scraper.scrape(url, xPath_query)

            // Make data matrix
            df = DataFrame.byArrayRow(df.getColumnsIndex()).appender()
            for (Element element : elements) {
                String processor_family_name = element.ownText()
                String intended_usage = intended_usage_map.find { key, _ -> processor_family_name.contains(key) }?.value ?: 'unknown'
                df.append(
                    processor_family_name,
                    processor_family_name,
                    timeFormat.format(this.localTime.now()),
                    url,
                    intended_usage,
                    element.attr('abs:href'),
                )
            }
            df = df.toDataFrame()

            // Save info
            Csv.save(df, snap_path)
        }

        this.progressBar.step()
        return df
    }


    // Extract processor urls from main ark
    protected DataFrame fetch_processor_urls(DataFrame family_url, Path snap_path) {
        String url = family_url.get('url', 0)
        String name = family_url.get('name', 0)
        name = name.replaceAll('[^a-zA-Z0-9_ ]+', '').replace(' ', '_')
        snap_path = snap_path.resolve(name + '.csv')

        // Get snapshot & Update time
        def df = check_snap(snap_path, [*this.standard_cols, 'url'])
        int days_since_update = check_last_update(df, ChronoUnit.DAYS)

        if (days_since_update > this.days_until_update || days_since_update < 0) {
            Document webpage =  this.scraper.getDoc(url)

            // table with id "product-table" -> table body -> table rows
            String xPath_query = './/table[@id=\'product-table\']//tbody//tr'
            Elements elements = webpage.selectXpath(xPath_query)

            // Sanity check with products count and interactive scraping, if not present
            Integer productsCount = Integer.parseInt(
                    webpage.selectXpath('.//span[@class=\'products-count\']').first().ownText()
            )
            if (productsCount != elements.size()) {
                String xPath_reject = './/button[text()=\"Accept Cookies\"]'
                elements = new InteractiveHTMLScraper().scrape(url, xPath_reject, xPath_query)
                assert productsCount == elements.size()
            }

            // Make data matrix
            df = DataFrame.byArrayRow(df.getColumnsIndex()).appender()
            for (Element element : elements) {
                String product_id = element.attr('data-product-id')
                // table data with data-component "arkproductlink" -> "a" elements
                xPath_query = './/td[@data-component=\'arkproductlink\']//a'
                Element link_element = element.selectXpath(xPath_query).first()
                if (link_element) {
                    df.append(
                        product_id,
                        link_element.ownText(),
                        timeFormat.format(this.localTime.now()),
                        url,
                        family_url.get("intended_usage", 0),
                        link_element.attr('abs:href'),
                    )
                }
            }
            df = df.toDataFrame()

            // Save info
            Csv.save(df, snap_path)
        }

        this.progressBar.step()
        return df
    }


    // Single run specification fetching
    protected DataFrame fetch_processor_specification(DataFrame processor_url, Path snap_path) {
        String name = processor_url.get('name', 0)
        String url = processor_url.get('url', 0)
        name = name.replaceAll('[^a-zA-Z0-9_ ]+', '').replace(' ', '_')
        snap_path = snap_path.resolve(name + '.csv')

        // Get snapshot & Update time
        DataFrame df = check_snap(snap_path, [])
        int days_since_update = check_last_update(df, ChronoUnit.DAYS)
        if (days_since_update > this.days_until_update || days_since_update < 0) {
            // divs with id "spec" -> "divs containing "tech-section" in class
            String xPath_query = './/div[contains(@id, "spec")]//div[contains(@class, "tech-section")]'
            Elements elements = this.scraper.scrape(url, xPath_query)

            // Extract data
            List<String> labels = []
            List<String> data = []
            for (Element element : elements) {
                xPath_query = './/div[contains(@class, "tech-label")]'
                Element label_element = element.selectXpath(xPath_query).first()
                xPath_query = './/div[contains(@class, "tech-data")]'
                Element data_element = element.selectXpath(xPath_query).first()
                if (label_element != null && data_element != null) {
                    if (!labels.contains(label_element.text())) {
                        labels.add(label_element.text())
                        data.add(data_element.text())
                    }
                }
            }

            // Merge info into url row
            df = DataFrame
                .foldByColumn(*this.standard_cols, *labels)
                .of(
                    processor_url.get('product_id', 0),
                    name,
                    timeFormat.format(this.localTime.now()),
                    url,
                    processor_url.get('intended_usage', 0),
                    *data
                )

            // Save info
            Csv.save(df, snap_path)
        }

        this.progressBar.step()

        return df
    }

    // Single run Callable
    protected class FetchSpecification implements Callable<DataFrame> {

        DataFrame processor_url
        Path snap_path

        FetchSpecification(DataFrame processor_url, Path snap_path) {
            this.processor_url = processor_url
            this.snap_path = snap_path
        }

        @Override
        DataFrame call() { return fetch_processor_specification(this.processor_url, this.snap_path) }

    }


    // Multiple run specification fetching
    protected DataFrame fetch_specifications(DataFrame processor_urls, Path snap_path) {
        // Get calls
        Files.createDirectories(snap_path.resolve('processor_infos'))
        List<FetchSpecification> callables = []
        for (int i = 0; i < processor_urls.height(); i++) {
            callables.add(
                new FetchSpecification(
                    processor_urls.rows(i).select(),
                    snap_path.resolve('processor_infos')
                )
            )
        }

        // Invoke calls
        List<Future> futures = []
        try {
            futures = this.threadPool.invokeAll(callables)
        } catch (InterruptedException e) {
            e.printStackTrace()
            this.threadPool.shutdownNow()
        }
        this.threadPool.shutdown()

        // Extract and save results
        snap_path = snap_path.resolve('Intel_cpu_specifications.csv')
        DataFrame specifications = DataFrame.empty()
        for (Future future : futures) {
            // Extract df from future
            DataFrame df = (DataFrame) future.get()

            // Add specification to collection dataframe
            specifications = specifications.vConcat(
                JoinType.full,
                df
            )
        }

        Csv.save(specifications, snap_path)

        return specifications
    }


    // Main (invoker)
    DataFrame main() {

        // Define intended usage mapping (order is important!)
        Map<String, String> intended_usage_map = [
        'Intel® Core™ Ultra'    : 'local',
        'Intel® Core™'          : 'local',
        'Intel® Xeon®'          : 'server',
        'Intel Atom®'           : 'local',
        'Intel® Pentium®'       : 'local',
        'Intel® Celeron®'       : 'local',
        'Intel®'                : 'local',
        ]

        // Collect Processor family URLs
        this.progressBar = new ProgressBar('Fetching processor family URLs:', 1)
        Files.createDirectories(this.snap_path.resolve('processor_family_urls'))
        DataFrame family_urls = fetch_processor_family_urls(
            'https://www.intel.com/content/www/us/en/ark.html',
            this.snap_path.resolve('processor_family_urls'),
            intended_usage_map
        )

        // Collect processor URLs
        this.progressBar = new ProgressBar('Fetching processor URLs:', family_urls.height())
        Files.createDirectories(this.snap_path.resolve('processor_urls'))
        DataFrame processor_urls = DataFrame
            .byArrayRow(*this.standard_cols, 'url')
            .appender()
            .toDataFrame()
        // Iterate over family urls and collect processor urls
        for (int i = 0; i < family_urls.height(); i++) {
            DataFrame family_url = family_urls.rows(i).select()

            processor_urls = processor_urls.vConcat(
                fetch_processor_urls(
                    family_url,
                    this.snap_path.resolve('processor_urls')                )
            )
        }

        // Collect processor specifications
        this.progressBar = new ProgressBar('Fetching Processor Info:', processor_urls.size())
        DataFrame specifications = fetch_specifications(
            processor_urls,
            this.snap_path,
        )

        return specifications
    }

}
