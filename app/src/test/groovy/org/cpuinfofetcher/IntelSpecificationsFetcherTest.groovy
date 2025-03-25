/* groovylint-disable Indentation, MethodName */
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
 * Test fetching of Intel specifications of processors
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class IntelSpecificationsFetcherTest extends Specification  {

    private final Path tempPath = Files.createTempDirectory('tmpdir')

    private final IntelSpecificationsFetcher sf = new IntelSpecificationsFetcher(
        0, 1, tempPath
    )

    private final HttpClient httpClient = HttpClient.newHttpClient()

    // Head level
    def 'check existence of website'() {
        setup:
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create('https://www.intel.com/content/www/us/en/ark.html'))
                .build()

        when:
            HttpResponse<Void> response = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            int responseCode = response.statusCode()

        then:
            responseCode != 404
    }

    def 'check family url fetching'() {
        setup:
            String url = 'https://www.intel.com/content/www/us/en/ark.html'
            Map<String, String> intended_usage_map = [
            'Intel® Core™ Ultra'            : 'local',
            'Intel® Core™'                  : 'local',
            'some other processor family'   : 'something',
 
            ]

        when:
            this.sf.fetch_processor_family_urls(url, this.tempPath, intended_usage_map)
            DataFrame specifications = Csv.load(this.tempPath.resolve('Intel_family_info.csv'))

        then:
            specifications.getColumnsIndex().toList() == [
                'product_id', 'name', 'time', 'source', 'intended_usage', 'url'
            ]
    }

    // Family level
    def 'check existence of family website'() {
        setup:
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create('https://www.intel.com/content/www/us/en/ark/products/series/232165/intel-processor-u-series.html'))
                .build()

        when:
            HttpResponse<Void> response = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            int responseCode = response.statusCode()

        then:
            responseCode != 404
    }

    def 'check processor url fetching'() {
        setup:
            DataFrame df = DataFrame
            .foldByRow('url', 'name', 'intended_usage')
            .of(
                'https://www.intel.com/content/www/us/en/ark/products/series/232165/intel-processor-u-series.html',
                'Intel® Processor U-series',
                'local'
            )

        when:
            this.sf.fetch_processor_urls(df, this.tempPath)
            DataFrame specifications = Csv.load(this.tempPath.resolve('Intel_Processor_Useries.csv'))

        then:
            specifications.getColumnsIndex().toList() == [
                'product_id', 'name', 'time', 'source', 'intended_usage', 'url'
            ]
    }

    // Processor level
    def 'check existence of model website'() {
        setup:
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create('https://www.intel.com/content/www/us/en/products/sku/35635/intel-atom-processor-230-512k-cache-1-60-ghz-533-mhz-fsb/specifications.html'))
                .build()

        when:
            HttpResponse<Void> response = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            int responseCode = response.statusCode()

        then:
            responseCode != 404
    }

    def 'check processor specifications fetching'() {
        setup:
            DataFrame df = DataFrame
                .foldByRow('product_id', 'url', 'name', 'intended_usage')
                .of(
                    '0',
                    'https://www.intel.com/content/www/us/en/products/sku/35635/intel-atom-processor-230-512k-cache-1-60-ghz-533-mhz-fsb/specifications.html',
                    'some_name',
                    'local'
                )

        when:
            this.sf.fetch_processor_specification(df, this.tempPath)
            DataFrame specifications = Csv.load(this.tempPath.resolve('some_name.csv'))

        then:
            specifications.getColumnsIndex().toList() == [
                'product_id', 'name', 'time', 'source', 'intended_usage', 'Product Collection', 'Code Name', 'Vertical Segment',
                'Processor Number', 'Lithography', 'Total Cores', 'Processor Base Frequency', 'Cache', 'Bus Speed',
                'FSB Parity', 'TDP', 'VID Voltage Range', 'Marketing Status', 'Launch Date', 'Servicing Status',
                'Embedded Options Available', 'Sockets Supported', 'TCASE', 'Package Size', 'Processing Die Size',
                '# of Processing Die Transistors', 'Intel® Turbo Boost Technology ‡',
                'Intel® Hyper-Threading Technology ‡', 'Intel® Virtualization Technology (VT-x) ‡',
                'Intel® Virtualization Technology for Directed I/O (VT-d) ‡', 'Intel® 64 ‡', 'Instruction Set',
                'Instruction Set Extensions', 'Idle States', 'Enhanced Intel SpeedStep® Technology',
                'Intel® Demand Based Switching', 'Thermal Monitoring Technologies',
                'Intel® Trusted Execution Technology ‡', 'Execute Disable Bit ‡'
            ]
    }

}
