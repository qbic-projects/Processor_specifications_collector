package org.cpuinfofetcher

import org.cpuinfofetcher.utils.Helpers
import org.cpuinfofetcher.utils.UnitsAdapter
import org.dflib.Exp
import org.dflib.Series
import org.dflib.Printers

import static org.dflib.Exp.*
import java.time.LocalDateTime


import java.nio.file.Files
import java.util.logging.Logger

import java.nio.file.Paths

import org.dflib.DataFrame
import org.dflib.JoinType
import org.dflib.csv.Csv

/**
 * Execute Main function
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class Main {

    private static final Logger LOGGER = Logger.getLogger('Main')

    private static DataFrame specifications
    private static DataFrame selected_specifications

    private static int days_until_outdated

    // Mapping possible naming schemes for attributes
    static Map<String, List<String>> specification_aliases = [
        'product_id': ['name', 'product_id'],
        'name': ['name'],
        'time': ['time'],
        'source': ['source'],
        'intended_usage': ['intended_usage'],
        'tdp': [
            'SDP', 'Scenario Design Power',
            'Processor Base Power', 'USAGE POWER (W)', 'AMD Configurable TDP (cTDP)', 'Default TDP',
            'tdp', 'thermal design power', 'Thermal Design Power (TDP)',
        ],
        'cores': ['Total Cores', '# of CPU Cores', 'cores'],
        'threads': ['cores', 'Total Cores', '# of CPU Cores', 'Total Threads', '# of Threads', 'threads']
    ]
    // Mapping units to columns
    static Map<String, List<String>> units_mapping = ['tdp': ['W', 'Watt']]

    static List<DataFrame> collectSpecifications(int days_until_outdated) {
        LOGGER.entering('Main', 'collectSpecifications')
        List<DataFrame> specificationsList = []

        // Intel
        LOGGER.info('Fetching Intel specifications.')
        IntelSpecificationsFetcher intelSF = new IntelSpecificationsFetcher(days_until_outdated, 1)
        specificationsList.add(intelSF.main())

        // AMD
        LOGGER.info('Fetching AMD specifications.')
        AMDSpecificationsFetcher amdSF = new AMDSpecificationsFetcher(days_until_outdated)
        specificationsList.add(amdSF.main())

        // Ampere
        LOGGER.info('Fetching Ampere specifications.')
        AmpereSpecificationsFetcher AmpereSF = new AmpereSpecificationsFetcher(days_until_outdated)
        specificationsList.add(AmpereSF.main())

        LOGGER.exiting('Main', 'collectSpecifications')
        return specificationsList
    }

    static DataFrame mergeSpecifications(List<DataFrame> specificationsList) {
        DataFrame specifications = DataFrame.empty()

        for (DataFrame specification : specificationsList) {
            specifications = specifications.vConcat(JoinType.full, specification)
        }

        return specifications
    }

    static DataFrame removeDuplicates(DataFrame specifications) {
        return specifications.rows().selectUnique('name')
    }


    /**
     * Adds default TDP values to the specifications DataFrame.
     *
     * Groups by 'intended_usage', computes averages for cores, threads,
     * and TDP, adds a row for "unknown" usage, and updates the DataFrame with default entries.
     *
     * @param specifications the input DataFrame
     * @return the updated DataFrame with default TDP values
     */
    static DataFrame computeDefaultTdps(DataFrame specifications) {
        DataFrame aggregatedDf = specifications.group('intended_usage').agg(
                $col('intended_usage').first().as('intended_usage'),
                $col('cores').castAsInt().avg().as("avg_cores"),
                $col('threads').castAsInt().avg().as("avg_threads"),
                $col('tdp (W)').castAsDouble().avg().as("avg_tdp"))

        DataFrame local_server_rows = aggregatedDf.rows({ it.get('intended_usage') == 'local' || it.get('intended_usage') == 'server' })
                .select()

        Double unknown_avg_cores = (local_server_rows.sum { it.get('avg_cores') } as Double) / local_server_rows.height()
        Double unknown_avg_threads = (local_server_rows.sum { it.get('avg_threads') } as Double) / local_server_rows.height()
        Double unknown_avg_tdp = (local_server_rows.sum { it.get('avg_tdp') } as Double) / local_server_rows.height()

        aggregatedDf = aggregatedDf.addRow([
                "intended_usage": "unknown",
                "avg_cores": unknown_avg_cores,
                "avg_threads": unknown_avg_threads,
                "avg_tdp": unknown_avg_tdp

        ])

        for (int i = 0; i < aggregatedDf.height(); i++) {
            DataFrame row = aggregatedDf.rows(i).select()
            Double avgThreads = row.get("avg_threads", 0) as Double
            Double avgCores = row.get("avg_cores", 0) as Double
            Double avgtdp = row.get("avg_tdp", 0) as Double
            Double computedThreads = avgCores != 0 ? avgThreads / avgCores : 0
            String intended_usage = row.get("intended_usage", 0)
            specifications = specifications.addRow([
                    'product_id': "default $intended_usage",
                    'name':  "default $intended_usage",
                    "time": LocalDateTime.now().toString(),
                    'source':  "default $intended_usage",
                    "intended_usage":  "default $intended_usage",
                    'tdp (W)': Helpers.round(avgtdp),
                    "cores": 1,
                    "threads": Helpers.round(computedThreads)
            ])
        }

        //String table = Printers.tabular.toString(aggregatedDf);
        //println(table);

        return specifications
    }



    static void main(String[] args) {
        this.days_until_outdated = args.length > 0 ? Integer.parseInt(args[0]) : 28

        Files.createDirectories(Paths.get('..', 'specifications_out'))
        Files.createDirectories(Paths.get('..', 'nf-co2footprint'))

        // Collecting Info
        LOGGER.entering('Main', 'main')
        List<DataFrame> specificationsList = collectSpecifications(this.days_until_outdated)
        LOGGER.info('Updated all specifications.')

        // Merging Info into big file
        DataFrame specifications = mergeSpecifications(specificationsList)
        specifications = removeDuplicates(specifications)
        Csv.save(specifications, Paths.get('..', 'specifications_out', 'specifications.csv'))
        this.specifications = specifications
        LOGGER.info('Merged all specifications.')

        // Selecting relevant information
        CPUSpecificationsSummarizer summarizer = new CPUSpecificationsSummarizer()
        DataFrame selected_specifications = summarizer.extract_selection(
            specifications,
            this.specification_aliases,
            true
        )
        LOGGER.info('Extracted information.')

        UnitsAdapter ua = new UnitsAdapter()
        selected_specifications = ua.unitToColumnName(selected_specifications, this.units_mapping)
        LOGGER.info('Extracted units from data.')

        // adjusts format of tdp values to make them uniform
        selected_specifications = ua.extractFirstNumber(selected_specifications)

        // add default TDPs
        selected_specifications = computeDefaultTdps(selected_specifications)
        LOGGER.info('Added default TDP values.')

        Csv.save(selected_specifications, Paths.get('..', 'specifications_out', 'specifications_filtered.csv'))
        Csv.save(selected_specifications, Paths.get('..', 'nf-co2footprint', 'CPU_TDP.csv'))

        this.selected_specifications = selected_specifications
        LOGGER.info('Saved final results.')



        LOGGER.exiting('Main', 'main')
    }

}
