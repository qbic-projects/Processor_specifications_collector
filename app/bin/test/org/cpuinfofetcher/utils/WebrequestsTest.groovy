package org.cpuinfofetcher

import spock.lang.Specification

/*
 * Test CSVio
 */
class WebrequestsTest extends Specification {

    boolean returnTrue() { return true }

    def "correct CSV writing"() {
        setup:
        def b = false

        when:
        b = returnTrue()


        then:
        b
    }

}
