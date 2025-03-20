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

    def 'check extraction of units'() {
        setup:
            DataFrame expected = DataFrame.foldByRow('A (W)', 'B', 'C (C)').of('1', '2 Beta', '3')
        when:     
            DataFrame result = this.ua.unitToColumnName(this.exampleDF, this.units)
        
        then:
            assertEqualDF(result, expected)
    }

}