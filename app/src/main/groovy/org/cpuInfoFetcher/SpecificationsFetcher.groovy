package org.cpuinfofetcher

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

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


    // Check last snap of Dataframe
    def check_snap(Path path, List newColumns) {
        if (Files.isRegularFile(path)) {
            return Csv.load(path)
        } else if (newColumns.size() > 0) {
            return DataFrame
                .byArrayRow(*newColumns)
                .appender()
        } else {
            return DataFrame.empty()
        }
    }

    // Method for accessing time of snapshot
    int check_last_update(def df, ChronoUnit unit) {
        int days_since_update = -1
        if (df instanceof DataFrame && df.height() > 0) {
            days_since_update = LocalDateTime.parse(df.get('time', 0), timeFormat)
                .until(this.localTime.now(), unit)
        }
        return days_since_update
    }

}

/**
 * Execute Main function
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class Main {

    static DataFrame specifications

    static List<DataFrame> collectSpecifications() {
        List<DataFrame> specificationsList = []

        // AMD
        AMDSpecificationsFetcher amdSF = new AMDSpecificationsFetcher(-1)

        // Intel
        IntelSpecificationsFetcher intelSF = new IntelSpecificationsFetcher(8, -1)
        specificationsList.add(intelSF.main())

        // Ampera

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
        List<DataFrame> specificationsList = collectSpecifications()
        DataFrame specifications = mergeSpecifications(specificationsList)
        Csv.save(specifications, Paths.get('..', 'CPU_specifications.csv'))
        this.specifications = specifications
    }

}
