package org.cpuinfofetcher

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardCopyOption

import java.time.temporal.ChronoUnit

import org.dflib.DataFrame
import org.dflib.JoinType
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
    AMDSpecificationsFetcher(int days_until_update, Path snap_path = null) {
        this.days_until_update = days_until_update

        String script_path = getClass().protectionDomain.codeSource.location.path
        if (snap_path == null) {
            this.snap_path = Paths.get(script_path, '..', '..', '..', '..', '..', 'snapshots', 'AMD')
                .toAbsolutePath()
                .normalize()
        } else {
            this.snap_path = snap_path
        }
        Files.createDirectories(this.snap_path)

        this.scraper = new InteractiveHTMLScraper(this.snap_path.toString())
    }


    DataFrame fetch_processor_specifications(String url, Path snap_path, Path downloadPath) {
        // Get snapshot & Update time
        def df = check_snap(snap_path, [])
        int days_since_update = check_last_update(df, ChronoUnit.DAYS)

        if (days_since_update > this.days_until_update || days_since_update < 0) {
            // divs with class "products processors" -> "a" elements with hrefs containing "processor"
            String xPath_query = './/button[contains(@class, "buttons-csv")]'
            String xPath_reject = './/button[text()=\"Accept Cookies\"]'

            this.scraper.download(url, xPath_reject, xPath_query)

            Files.move(downloadPath, snap_path, StandardCopyOption.REPLACE_EXISTING)
            removeBOM(snap_path)

            df = Csv.load(snap_path)
            df = add_metadata(df, url)
            df = df.cols().selectAs(Map.of('Name', 'name'))

            Csv.save(df, snap_path)
        }

        return df
    }

    Map<String,DataFrame> fetch_all_specifications(
        String base_url, Map<String, String> specification_sites, Path snap_path
        ) {
        Map<String,DataFrame> specifications = ['cpu': DataFrame.empty(), 'gpu': DataFrame.empty()]
        specification_sites.each { site, downloadFile ->
            String url = base_url + site + '.html'

            // Define paths
            Path downloadPath = snap_path.resolve(downloadFile)
            Path file_snap_path = snap_path.resolve("AMD_${String.join('_', *site.split('-'))}.csv")

            // get specifications
            String processor_type
            if (site.contains('graphics')) {
                processor_type = 'gpu'
            } else {
                processor_type = 'cpu'
            }
            specifications.replace(
                processor_type,
                specifications.get(processor_type).vConcat(
                    JoinType.full,
                    fetch_processor_specifications(url, file_snap_path, downloadPath)
                )
            )
        }

        specifications.each { type, df ->
            Csv.save(df, snap_path.resolve("AMD_${type}_specifications.csv"))
        }

        return specifications
    }

    DataFrame main() {
        String base_url = 'https://www.amd.com/en/products/specifications/'
        Map<String, String> processor_specification_sites = [
            'processors': 'Processor Specifications.csv',
            'server-processor': 'Server Processor Specifications.csv',
            'accelerators': 'Accelerator Specifications.csv',
            'embedded': 'Embedded Processor Specifications.csv',
            'professional-graphics': 'Professional Graphics Specifications.csv',
            'graphics': 'Graphics Specifications.csv',
        ]

        Map<String, DataFrame> specifications = fetch_all_specifications(
            base_url,
            processor_specification_sites,
            this.snap_path,
        )

        this.scraper.quit()

        return specifications.get('cpu')
    }

}
