package org.cpuinfofetcher

import java.nio.file.Path
import java.nio.file.Paths

import java.time.temporal.ChronoUnit

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import java.util.concurrent.Future

import org.jsoup.nodes.FormElement
import org.jsoup.select.Elements

import org.dflib.DataFrame
import org.dflib.JoinType
import org.dflib.Printers
import org.dflib.csv.Csv

/**
 * Fetch AMD specifications
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class AMDSpecificationsFetcher extends SpecificationsFetcher {

    AMDSpecificationsFetcher(int days_until_update) {
        this.days_until_update = days_until_update
    }

    DataFrame fetch_processor_specifications(String url, Path snap_path) {
        snap_path = snap_path.resolve('AMD_processor_specifications.csv')
        int days_since_update = check_last_update(df, ChronoUnit.DAYS)

        if (days_since_update > this.days_until_update || days_since_update < 0) {
            // divs with class "products processors" -> "a" elements with hrefs containing "processor"
            String xPath_query = './/button[contains(@class="buttons-csv")]'
            FormElement downloadButton = this.scraper.scrape(url, xPath_query).first()

            downloadButton.submit()
        }

        return df
    }

    DataFrame main() {
        String script_path = getClass().protectionDomain.codeSource.location.path
        Path snap_path = Paths.get(script_path, '..', '..', '..', 'resources', 'main', 'assets')
            .toAbsolutePath()
            .normalize()

        DataFrame specifications = fetch_processor_specifications(
            'https://www.amd.com/en/products/specifications/processors.html'
            snap_path
        )

        return specifications
    }

}
