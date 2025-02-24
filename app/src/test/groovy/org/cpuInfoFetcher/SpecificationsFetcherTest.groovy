/* groovylint-disable Indentation, MethodName */
package org.cpuinfofetcher

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

import spock.lang.Specification

/**
 * Test Generalization class of Fetchers
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
public class SpecificationsFetcherTest extends Specification {

    private final SpecificationsFetcher sf = new SpecificationsFetcher()
    private final Path tempPath = Files.createTempFile('file', '.tmp')
    private final DataFrame exampleDF = DataFrame.foldByRow('time', 'place').of('2024-01-01 16:45:00', 'Milkyway')

    boolean assertEqualDF(DataFrame df1, DataFrame df2) {
        assert df1.size() == df2.size()
        assert df1.getColumnsIndex().toArray() == df2.getColumnsIndex().toArray()
        for (int i = 0; i < df1.width(); i++) {
            for (int j = 0; j < df1.height(); j++) {
                assert df1.get(i, j) == df2.get(i, j)
            }
        }
        return true
    }

    def 'check loading of snap'() {
        setup:
            Csv.save(this.exampleDF, this.tempPath)

        when:
            DataFrame snap = this.sf.check_snap(this.tempPath, null)

        then:
            this.assertEqualDF(this.exampleDF, snap)
    }

    def 'check last update extraction failing'() {
        when:
            int days_since_update = this.sf.check_last_update(null, ChronoUnit.DAYS)

        then:
            days_since_update == -1
    }

    def 'check last update extraction succeeding'() {
        when:
            int days_since_update = this.sf.check_last_update(this.exampleDF, ChronoUnit.DAYS)

        then:
            days_since_update > 365
    }

    def 'check metadata adding succeeding'() {
        setup:
            DataFrame df = this.exampleDF

        when:
            String now = this.sf.timeFormat.format(this.sf.localTime.now())
            df = this.sf.add_metadata(df, 'some_source')

        then:
            assertEqualDF(
                df,
                DataFrame
                    .foldByRow('time', 'source', 'place')
                    .of(now, 'some_source', 'Milkyway')
            )
    }

}
