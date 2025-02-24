package org.cpuinfofetcher

import java.nio.file.Path
import java.nio.file.Paths

import java.time.temporal.ChronoUnit

import org.jsoup.nodes.Element

import org.dflib.DataFrame
import org.dflib.csv.Csv

/**
 * Fetch Ampera specifications
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class AmperaSpecificationsFetcher extends SpecificationsFetcher {

    Path snap_path
    HTMLScraper scraper
    AmperaSpecificationsFetcher(int days_until_update, Path snap_path=null) {
        this.days_until_update = days_until_update

        if (snap_path == null) {
            String script_path = getClass().protectionDomain.codeSource.location.path
            this.snap_path = Paths.get(script_path, '..', '..', '..', 'resources', 'main', 'assets', 'Ampera')
                .toAbsolutePath()
                .normalize()
        } else {
            this.snap_path = snap_path
        }

        this.scraper = new HTMLScraper()
    }


    DataFrame fetch_processor_specifications(String url, Path snap_path) {
        snap_path = snap_path.resolve('Ampera_processor_specifications.csv')

        // Get snapshot & Update time
        def df = check_snap(snap_path, [])
        int days_since_update = check_last_update(df, ChronoUnit.DAYS)

        if (days_since_update > this.days_until_update || days_since_update < 0) {
            // divs with class "products processors" -> "a" elements with hrefs containing "processor"
            String xPath_query = './/div/*[contains(text(), "Ampere Altra Family Products")]/..//table'

            Element table = this.scraper.scrape(url, xPath_query).first()
            df = this.scraper.parse_table(table)
            df = add_metadata(df, url)
            df = df.cols().selectAs(Map.of('ORDERING PART NUMBER', 'product_id'))
            df = df.cols().selectAs(Map.of('PRODUCT NAME', 'name'))

            Csv.save(df, snap_path)
        } else {
            df = Csv.load(snap_path)
        }

        return df
    }

    DataFrame main() {
        DataFrame specifications = fetch_processor_specifications(
            'https://amperecomputing.com/briefs/ampere-altra-family-product-brief',
            snap_path
        )

        return specifications
    }

}
