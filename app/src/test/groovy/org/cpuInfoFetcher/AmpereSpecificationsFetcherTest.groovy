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
 * Test fetching of Ampere specifications
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class AmpereSpecificationsFetcherTest extends Specification {

    private final Path tempPath = Files.createTempDirectory('tmpdir')

    private final AmpereSpecificationsFetcher sf = new AmpereSpecificationsFetcher(
        0, tempPath
    )

    private final HttpClient httpClient = HttpClient.newHttpClient()

    def 'check existence of website'() {
        setup:
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create('https://amperecomputing.com/briefs/ampere-altra-family-product-brief'))
                .build()
        
        when:     
            HttpResponse<Void> response = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            int responseCode = response.statusCode()
        
        then:
            responseCode != 404
    }

    def 'check processor specifications fetching'() {
        setup:
            String url = 'https://amperecomputing.com/briefs/ampere-altra-family-product-brief'

        when:
            this.sf.fetch_processor_specifications(url, this.tempPath)
            DataFrame specifications = Csv.load(this.tempPath.resolve('Ampere_cpu_specifications.csv'))

        then:
            specifications.getColumnsIndex().toArray() == [
                'time', 'source' , 'intended_usage', 'name', 'CORES', 'SUSTAINED FREQUENCY (GHz)', 'USAGE POWER (W)', 'product_id'
            ]
    }
}
