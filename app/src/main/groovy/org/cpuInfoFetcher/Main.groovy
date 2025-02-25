package org.cpuinfofetcher

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

    private static final int days_until_outdated = 28

    // Mapping possible naming schemes for attributes
    static Map<String, String[]> specification_aliases = [
        'product_id': ['name', 'product_id'],
        'name': ['name'],
        'time': ['time'],
        'source': ['source'],
        'tdp': [
            'SDP', 'Scenario Design Power',
            'Processor Base Power', 'USAGE POWER (W)', 'Default TDP',
            'tdp', 'thermal design power',
        ],
        'cores': ['Total Cores', '# of CPU Cores', 'cores'],
        'threads': ['cores', 'Total Cores', '# of CPU Cores', 'Total Threads', '# of Threads', 'threads']
    ]
    // Mapping units to columns
    static Map<String, String[]> units_mapping = ['tdp': ['W', 'Watt']]

    static List<DataFrame> collectSpecifications(int days_until_outdated) {
        LOGGER.entering('Main', 'collectSpecifications')
        List<DataFrame> specificationsList = []

        // Intel
        LOGGER.info('Fetching Intel specifications.')
        IntelSpecificationsFetcher intelSF = new IntelSpecificationsFetcher(1, days_until_outdated)
        specificationsList.add(intelSF.main())

        // AMD
        LOGGER.info('Fetching AMD specifications.')
        AMDSpecificationsFetcher amdSF = new AMDSpecificationsFetcher(days_until_outdated)
        specificationsList.add(amdSF.main())

        // Ampera
        LOGGER.info('Fetching Ampera specifications.')
        AmperaSpecificationsFetcher amperaSF = new AmperaSpecificationsFetcher(days_until_outdated)
        specificationsList.add(amperaSF.main())

        LOGGER.exiting('Main', 'collectSpecifications')
        return specificationsList
    }

    static DataFrame mergeSpecifications(List<DataFrame> specificationsList) {
        DataFrame specifications = DataFrame.empty()

        for (specification : specificationsList) {
            specifications = specifications.vConcat(JoinType.full, specification)
        }

        return specifications
    }

    static void main(String[] args) {
        // Collecting Info
        LOGGER.entering('Main', 'main')
        List<DataFrame> specificationsList = collectSpecifications(this.days_until_outdated)
        LOGGER.info('Updated all specifications.')

        // Merging Info into big file
        DataFrame specifications = mergeSpecifications(specificationsList)
        Csv.save(specifications, Paths.get('..', 'CPU_specifications.csv'))
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

        Csv.save(selected_specifications, Paths.get('..', 'CPU_selected_specifications.csv'))
        this.selected_specifications = selected_specifications
        LOGGER.info('Saved final results.')


        LOGGER.exiting('Main', 'main')
    }

}
