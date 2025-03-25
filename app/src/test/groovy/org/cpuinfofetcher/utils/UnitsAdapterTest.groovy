/* groovylint-disable Indentation, MethodName, TrailingWhitespace */
package org.cpuinfofetcher.utils

import org.dflib.DataFrame

import spock.lang.Specification

/**
 * Test the adaption of units to a common format
 */
class UnitsAdapterTest extends Specification {

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
            DataFrame output = this.ua.extractFirstNumber(input)

        then:
        Helpers.assertEqualDF(expected, output)

    }

}