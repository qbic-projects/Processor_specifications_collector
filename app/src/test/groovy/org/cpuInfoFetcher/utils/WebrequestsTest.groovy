package org.cpuinfofetcher

import spock.lang.Specification

/*
 * Test CSVio
 */
class CSVioTest extends Specification {

    protected List<String[]> mockData = [
        ['col1', 'col2', 'col3'],
        ['1', '2', '3']
    ]
    protected Path mockPath = '../resources'

    def "correct CSV writing"() {
        setup:
        def csvIO = new CSVio()

        when:
        csvIO.write(this.mockData, this.mockPath.resolve('mock.csv'))

        then:
        mockPath.resolve('mock.csv').toFile().isFile()
    }

}
