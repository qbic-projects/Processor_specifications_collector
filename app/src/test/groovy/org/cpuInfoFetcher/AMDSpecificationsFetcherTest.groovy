/* groovylint-disable Indentation, MethodName, TrailingWhitespace */
package org.cpuinfofetcher

import java.nio.file.Path
import java.nio.file.Files

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import org.dflib.DataFrame
import org.dflib.csv.Csv

import spock.lang.Specification

/**
 * Test fetching of AMD specifications
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class AMDSpecificationsFetcherTest extends Specification {

    private final Path tempPath = Files.createTempDirectory('tmpdir')

    private final AMDSpecificationsFetcher sf = new AMDSpecificationsFetcher(
        0, tempPath
    )

    private final HttpClient httpClient = HttpClient.newHttpClient()

    def 'check existence of website'() {
        setup:
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create('https://www.amd.com/en/products/specifications/processors.html'))
                .build()
        
        when:     
            HttpResponse<Void> response = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            int responseCode = response.statusCode()
        
        then:
            responseCode != 404
    }

    def 'check processor specifications fetching'() {
        setup:
            String url = 'https://www.amd.com/en/products/specifications/processors.html'

        when:
            this.sf.fetch_processor_specifications(url, this.tempPath)
            DataFrame specifications = Csv.load(this.tempPath.resolve('AMD_processor_specifications.csv'))

        then:
            specifications.getColumnsIndex().toArray() == [
                'time', 'source', 'name', 'Family', 'Series', 'Form Factor', '# of CPU Cores', '# of Threads',
                'Max. Boost Clock', 'Base Clock', 'L2 Cache', 'L3 Cache', 'Default TDP', 'L1 Cache',
                'AMD Configurable TDP (cTDP)', 'Processor Technology for CPU Cores', 'Unlocked for Overclocking',
                'CPU Socket', 'Thermal Solution (PIB)', 'Recommended Cooler', 'Thermal Solution (MPK)',
                'Max. Operating Temperature (Tjmax)', 'Launch Date', '*OS Support', 'PCI Express® Version',
                'System Memory Type', 'Memory Channels', 'System Memory Specification', 'Graphics Model',
                'Graphics Core Count', 'Graphics Frequency', 'AMD Ryzen™ AI', 'Product ID Boxed', 'Product ID Tray',
                'Product ID MPK', 'Supported Technologies'
            ]
    }

}
