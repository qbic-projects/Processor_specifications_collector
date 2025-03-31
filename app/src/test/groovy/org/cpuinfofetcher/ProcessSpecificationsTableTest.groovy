package org.cpuinfofetcher
import org.cpuinfofetcher.utils.Helpers
import org.dflib.DataFrame
import spock.lang.Specification

class ProcessSpecificationsTableTest extends Specification {

    private final UnitsAdapter ua = new UnitsAdapter()

    private final DataFrame exampleDF = DataFrame.foldByRow('A', 'B', 'C').of('1 W', '2 Beta', '3')
    private final Map<String, List<String>> units = ['A': ['W', 'Watt'], 'C': ['C', 'Coloumb'], 'X': ['I', 'Imaginary']]


    def 'check extraction of units'() {
        setup:
        DataFrame expected = DataFrame.foldByRow('A (W)', 'B', 'C (C)').of('1', '2 Beta', '3')
        when:
        DataFrame result = this.ua.unitToColumnName(this.exampleDF, this.units)

        then:
        Helpers.assertEqualDF(result, expected)
    }

    def 'check tdp value extraction'() {
        setup:
        DataFrame input = DataFrame
                .foldByColumn("test_col", "tdp (W)")
                .of("test_val_1", "test_val_2", "test_val_3", "test_val_4", "15-30", "15-30", "1.5/10", "2.3--5")
        DataFrame expected = DataFrame
                .foldByColumn("test_col", "tdp (W)")
                .of("test_val_1", "test_val_2", "test_val_3", "test_val_4", 15.0, 15.0, 1.5, 2.3)

        when:
        DataFrame output = ProcessSpecificationsTable.extractFirstNumber(input)

        then:
        Helpers.assertEqualDF(expected, output)

    }

    def 'check default tdp computation'() {
        setup:
        DataFrame input = DataFrame
                .foldByColumn("intended_usage", "cores", "threads", "tdp (W)", "Launch Year/Last Time Buy")
                .of("local", "server", "embedded", "local", "server", "embedded", "embedded",
                        2, 8, 4, 4, 16, 4, 4,
                        2, 16, 8, 4, 32, 8, 8,
                        15.0, 15.0, 1.5, 15.0, 10.0, 1.5, 2.5,
                        2022, 2023, 2027, 2025, 2030, 2021, -1)
        DataFrame expected = DataFrame
                .foldByColumn("intended_usage", "cores", "threads", "tdp (W)", "Launch Year/Last Time Buy")
                .of("local", "server", "embedded", "local", "server", "embedded", "embedded", "default local", "default server", "default embedded", "default unknown",
                        2, 8, 4, 4, 16, 4, 4, 1, 1, 1, 1,
                        2, 16, 8, 4, 32, 8, 8, 1, 2.0, 2.0, 1.8,
                        15.0, 15.0, 1.5, 15.0, 10.0, 1.5, 2.5, 15.0, 12.5, 1.5, 13.75,
                        2022, 2023, 2027, 2025, 2030, 2021, -1, null, null, null, null
                        )

        when:
        DataFrame output = ProcessSpecificationsTable.computeDefaultTdps(input)

        then:
        Helpers.assertEqualDF(expected, output)

    }
}
