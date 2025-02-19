package org.cpuinfofetcher

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardCopyOption

import java.time.temporal.ChronoUnit

import org.dflib.DataFrame
import org.dflib.csv.Csv

/**
 * Fetch AMD specifications
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class AMDSpecificationsFetcher extends SpecificationsFetcher {

    Path snap_path
    InteractiveHTMLScraper scraper
    AMDSpecificationsFetcher(int days_until_update) {
        this.days_until_update = days_until_update

        String script_path = getClass().protectionDomain.codeSource.location.path
        this.snap_path = Paths.get(script_path, '..', '..', '..', 'resources', 'main', 'assets', 'AMD')
            .toAbsolutePath()
            .normalize()

        this.scraper = new InteractiveHTMLScraper(this.snap_path.toString())
    }


    DataFrame fetch_processor_specifications(String url, Path snap_path) {
        Path downloadPath = snap_path.resolve('Processor Specifications.csv')
        snap_path = snap_path.resolve('AMD_processor_specifications.csv')

        // Get snapshot & Update time
        def df = check_snap(snap_path, [])
        int days_since_update = check_last_update(df, ChronoUnit.DAYS)

        if (days_since_update > this.days_until_update || days_since_update < 0) {
            // divs with class "products processors" -> "a" elements with hrefs containing "processor"
            String xPath_query = './/button[contains(@class, "buttons-csv")]'
            String xPath_reject = './/button[@id=\"onetrust-reject-all-handler\"]'

            this.scraper.scrape(url, xPath_reject, xPath_query)

            Files.move(downloadPath, snap_path, StandardCopyOption.REPLACE_EXISTING)

            df = Csv.load(snap_path)
            df = add_metadata(df, url)

            Csv.save(df, snap_path)
        }

        return df
    }

    DataFrame main() {
        DataFrame specifications = fetch_processor_specifications(
            'https://www.amd.com/en/products/specifications/processors.html',
            snap_path
        )

        return specifications
    }

}
