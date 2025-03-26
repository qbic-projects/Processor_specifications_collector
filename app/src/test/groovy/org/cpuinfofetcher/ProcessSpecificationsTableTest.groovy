package org.cpuinfofetcher

import org.cpuinfofetcher.utils.Helpers
import org.dflib.DataFrame
import spock.lang.Specification

class ProcessSpecificationsTableTest extends Specification {

    def 'check default tdp computation'() {
        setup:
        DataFrame input = DataFrame
                .foldByColumn("intended_usage", "cores", "threads", "tdp (W)")
                .of("local", "server", "embedded", "local", "server", "embedded",
                        2, 8, 4, 4, 16, 4,
                        2, 16, 8, 4, 32, 8,
                        15.0, 15.0, 1.5, 15.0, 10.0, 1.5)
        DataFrame expected = DataFrame
                .foldByColumn("intended_usage", "cores", "threads", "tdp (W)")
                .of("local", "server", "embedded", "local", "server", "embedded", "default local", "default server", "default embedded", "default unknown",
                        2, 8, 4, 4, 16, 4, 1, 1, 1, 1,
                        2, 16, 8, 4, 32, 8, 1, 2.0, 2.0, 1.8,
                        15.0, 15.0, 1.5, 15.0, 10.0, 1.5, 15.0, 12.5, 1.5, 13.75)

        when:
        DataFrame output = ProcessSpecificationsTable.computeDefaultTdps(input)

        then:
        Helpers.assertEqualDF(expected, output)

    }
}
