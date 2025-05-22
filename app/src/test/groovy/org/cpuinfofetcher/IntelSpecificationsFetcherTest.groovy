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
                .uri(URI.create('https://www.intel.com/content/www/us/en/products/sku/230902/intel-core-i31215ul-processor-10m-cache-up-to-4-40-ghz/specifications.html'))
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
                    'https://www.intel.com/content/www/us/en/products/sku/230902/intel-core-i31215ul-processor-10m-cache-up-to-4-40-ghz/specifications.html',
                    'some_name',
                    'local'
                )

        when:
            this.sf.fetch_processor_specification(df, this.tempPath)
            DataFrame specifications = Csv.load(this.tempPath.resolve('some_name.csv'))

        then:
            specifications.getColumnsIndex().toList() == [
                'product_id', 'name', 'time', 'source', 'intended_usage', 'Product Collection', 'Code Name', 'Vertical Segment',
                'Processor Number', 'Lithography', 'Total Cores', '# of Performance-cores', '# of Efficient-cores', 'Total Threads',
                'Max Turbo Frequency', 'Performance-core Max Turbo Frequency', 'Efficient-core Max Turbo Frequency',
                'Processor Base Frequency', 'Cache', 'Processor Base Power', 'TDP', 'Minimum Assured Power', 'Maximum Assured Power',
                'Marketing Status', 'Launch Date', 'Embedded Options Available', 'Use Conditions', 'Max Memory Size (dependent on memory type)',
                'Memory Types', 'Max # of Memory Channels', 'ECC Memory Supported ‡', 'GPU Name‡', 'Graphics Max Dynamic Frequency',
                'Graphics Output', 'Execution Units', 'Max Resolution (HDMI)‡', 'Max Resolution (DP)‡', 'Max Resolution (eDP - Integrated Flat Panel)‡',
                'DirectX* Support', 'OpenGL* Support', 'OpenCL* Support', 'Multi-Format Codec Engines', 'Intel® Quick Sync Video',
                '# of Displays Supported ‡', 'Device ID', 'Intel® Thunderbolt™ 4', 'Microprocessor PCIe Revision', 'Chipset / PCH PCIe Revision',
                'Max # of PCI Express Lanes', 'Sockets Supported', 'Max CPU Configuration', 'TJUNCTION', 'Package Size',
                'Max Operating Temperature', 'Intel® Volume Management Device (VMD)', 'Intel® Gaussian & Neural Accelerator',
                'Intel® Thread Director', 'Intel® Image Processing Unit', 'Intel® Smart Sound Technology', 'Intel® Wake on Voice',
                'Intel® High Definition Audio', 'MIPI SoundWire*', 'Intel® Deep Learning Boost (Intel® DL Boost) on CPU',
                'Intel® Adaptix™ Technology', 'Intel® Speed Shift Technology', 'Intel® Turbo Boost Technology ‡',
                'Intel® Hyper-Threading Technology ‡', 'Instruction Set', 'Instruction Set Extensions', 'Thermal Monitoring Technologies',
                'Intel® Flex Memory Access', 'Intel® Hardware Shield Eligibility ‡', 'Intel® Threat Detection Technology (TDT)',
                'Intel® Active Management Technology (AMT) ‡', 'Intel® Standard Manageability (ISM) ‡', 'Intel® Remote Platform Erase (RPE) ‡',
                'Intel® One-Click Recovery ‡', 'Intel® QuickAssist Software Acceleration', 'Intel® Control-Flow Enforcement Technology',
                'Intel® Total Memory Encryption - Multi Key', 'Intel® Total Memory Encryption', 'Intel® AES New Instructions',
                'Secure Key', 'Intel® OS Guard', 'Intel® Trusted Execution Technology ‡', 'Execute Disable Bit ‡',
                'Intel® Boot Guard', 'Mode-based Execute Control (MBEC)', 'Intel® Stable IT Platform Program (SIPP)',
                'Intel® Virtualization Technology with Redirect Protection (VT-rp) ‡', 'Intel® Virtualization Technology (VT-x) ‡',
                'Intel® Virtualization Technology for Directed I/O (VT-d) ‡', 'Intel® VT-x with Extended Page Tables (EPT) ‡'
                ]
    }

}
