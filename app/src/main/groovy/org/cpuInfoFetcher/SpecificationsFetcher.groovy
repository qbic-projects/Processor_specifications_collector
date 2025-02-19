package org.cpuinfofetcher

import java.util.logging.Logger

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.charset.StandardCharsets

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import org.dflib.DataFrame
import org.dflib.JoinType
import org.dflib.csv.Csv

/**
 * Execute Generalization class of Fetchers
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
public class SpecificationsFetcher {

    // Define common time format
    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
    LocalDateTime localTime = LocalDateTime.now()

    // DataFrame construction parameters
    int days_until_update = 28
    List<String> standard_cols = ['product_id', 'name', 'time', 'source']

    // TODO: MERGING


    // Check last snap of Dataframe
    DataFrame check_snap(Path path, List newColumns) {
        if (Files.isRegularFile(path)) {
            return Csv.load(path)
        } else if (newColumns.size() > 0) {
            return DataFrame.empty(*newColumns)
        } else {
            return DataFrame.empty()
        }
    }

    // Method for accessing time of snapshot
    int check_last_update(def df, ChronoUnit unit) {
        if (df instanceof DataFrame && df.height() > 0) {
            if (df.getColumnsIndex().toArray().contains('time')) {
                return LocalDateTime.parse(df.get('time', 0), timeFormat)
                    .until(this.localTime.now(), unit)
            }
        }
        return -1
    }

    DataFrame add_metadata(DataFrame df, String source) {
        def meta_df = DataFrame.byArrayRow('time', 'source').appender()
        for (int i = 0; i < df.height(); i++) {
            meta_df.append(
                timeFormat.format(this.localTime.now()),
                source
            )
        }
        meta_df = meta_df.toDataFrame()

        return meta_df.hConcat(df)
    }

    void removeBOM(Path path){
        byte[] bytes = Files.readAllBytes(path)
        String content = new String(bytes, StandardCharsets.UTF_8)
        if (content.startsWith('\uFEFF')) {
            content = content.substring(1)
        }
        Files.write(path, content.getBytes())
    }

}

/**
 * Execute Main function
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class Main {

    private static final Logger LOGGER = Logger.getLogger('Main')

    static DataFrame specifications

    static List<DataFrame> collectSpecifications(int days_until_outdated) {
        LOGGER.entering('Main', 'collectSpecifications')
        List<DataFrame> specificationsList = []

        // Intel
        LOGGER.info('Fetching Intel specifications.')
        IntelSpecificationsFetcher intelSF = new IntelSpecificationsFetcher(1, days_until_outdated)
        specificationsList.add(intelSF.main())

        // AMD
        LOGGER.info('Fetching AMD specifications.')
        AMDSpecificationsFetcher amdSF = new AMDSpecificationsFetcher(days_until_outdated)
        specificationsList.add(amdSF.main())

        // Ampera
        LOGGER.info('Fetching Ampera specifications.')
        AmperaSpecificationsFetcher amperaSF = new AmperaSpecificationsFetcher(days_until_outdated)
        specificationsList.add(amperaSF.main())

        LOGGER.exiting('Main', 'collectSpecifications')
        return specificationsList
    }

    static DataFrame mergeSpecifications(List<DataFrame> specificationsList) {
        DataFrame specifications = DataFrame.empty()

        for (specification : specificationsList) {
            specifications = specifications.vConcat(JoinType.full, specification)
        }

        return specifications
    }

    static void main(String[] args) {
        LOGGER.entering('Main', 'main')
        List<DataFrame> specificationsList = collectSpecifications(28)
        LOGGER.info('Updated all specifications.')

        DataFrame specifications = mergeSpecifications(specificationsList)
        Csv.save(specifications, Paths.get('..', 'CPU_specifications.csv'))
        this.specifications = specifications
        LOGGER.info('Merged all specifications.')
        LOGGER.exiting('Main', 'main')
    }

}
