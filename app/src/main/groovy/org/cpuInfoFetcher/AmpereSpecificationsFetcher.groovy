package org.cpuinfofetcher

import org.cpuinfofetcher.utils.HTMLScraper

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

import java.time.temporal.ChronoUnit

import org.jsoup.nodes.Element

import org.dflib.DataFrame
import org.dflib.csv.Csv
import org.dflib.JoinType

/**
 * Fetch Ampere specifications
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class AmpereSpecificationsFetcher extends SpecificationsFetcher {

    Path snap_path
    HTMLScraper scraper
    AmpereSpecificationsFetcher(int days_until_update, Path snap_path=null) {
        this.days_until_update = days_until_update

        if (snap_path == null) {
            String script_path = getClass().protectionDomain.codeSource.location.path
            this.snap_path = Paths.get(script_path, '..', '..', '..', '..', '..', 'snapshots', 'Ampere')
                .toAbsolutePath()
                .normalize()
        } else {
            this.snap_path = snap_path
        }
        Files.createDirectories(this.snap_path)

        this.scraper = new HTMLScraper()
    }


    DataFrame fetch_processor_specifications(String url, Path snap_path) {
        snap_path = snap_path.resolve('Ampere_cpu_specifications.csv')

        // Get snapshot & Update time
        def df = check_snap(snap_path, [])
        int days_since_update = check_last_update(df, ChronoUnit.DAYS)

        if (days_since_update > this.days_until_update || days_since_update < 0) {
            // divs with class "products processors" -> "a" elements with hrefs containing "processor"
            String xPath_query = './/div/*[contains(text(), "Ampere Altra Family Products")]/..//table'

            Element table = this.scraper.scrape(url, xPath_query).first()
            String intended_usage = 'compute cluster' // ampere processors intended for compute cluster use in general
            df = this.scraper.parse_table(table)
            df = add_metadata(df, url, intended_usage)
            df = df.cols().selectAs(Map.of('ORDERING PART NUMBER', 'product_id'))
            df = df.cols().selectAs(Map.of('PRODUCT NAME', 'name'))

            Csv.save(df, snap_path)
        } else {
            df = Csv.load(snap_path)
        }

        return df
    }

    DataFrame manually_add_processor_specifications(DataFrame existing_df, DataFrame new_df) {
        snap_path = snap_path.resolve('Ampere_cpu_specifications.csv')
        DataFrame df = existing_df.vConcat(JoinType.inner, new_df)
        Csv.save(df, snap_path)
        return df
    }

    DataFrame main() {
        DataFrame specifications = fetch_processor_specifications(
            'https://amperecomputing.com/briefs/ampere-altra-family-product-brief',
            snap_path
        )

        // Load local specifications file as it is only available as .png on the website 
        DataFrame spec_file = Csv.load(
                Paths.get(this.class.getResource('/Ampere_One_Family_Specifications_2025-03-20.csv').toURI())
        )
        specifications = manually_add_processor_specifications(specifications, spec_file)

        return specifications
    }

}
