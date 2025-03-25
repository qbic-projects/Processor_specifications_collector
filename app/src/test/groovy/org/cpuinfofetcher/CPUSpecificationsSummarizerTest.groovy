/* groovylint-disable Indentation */
package org.cpuinfofetcher

import org.dflib.DataFrame
import org.dflib.JoinType
import org.dflib.Printers

import spock.lang.Specification

/**
 * Test summary of extracted information into one file
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class CPUSpecificationsSummarizerTest extends Specification {

    private final CPUSpecificationsSummarizer summarizer = new CPUSpecificationsSummarizer()

    private final DataFrame exampleDF = DataFrame.foldByRow('A', 'B').of('1', '2')
    private final Map<String, String[]> aliases = ['A': ['A'], 'C': ['X', 'B'], 'B': ['C']] as Map<String, String[]>

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

    // Pattern matching
    def 'check matching of columns'() {
        when:
            Map<String, String> matches = this.summarizer.match_column_aliases(this.exampleDF, this.aliases)

        then:
            matches == [
                'A': '1',
                'C': '2',
                'B': null
            ]
    }

    // Extraction
    def 'check information extraction'() {
        setup:
            DataFrame df = DataFrame.empty()

        when:
            DataFrame df_res = this.summarizer.extract_selection(this.exampleDF, this.aliases, true)

        then:
            assertEqualDF(df_res, df)
    }

    def 'check information extraction without discarding'() {
        setup:
            DataFrame df = DataFrame
                .foldByRow('A', 'C', 'B')
                .of('1', '2', null)

        when:
            DataFrame df_res = this.summarizer.extract_selection(this.exampleDF, this.aliases, false)

        then:
            assertEqualDF(df_res, df)
    }

}
