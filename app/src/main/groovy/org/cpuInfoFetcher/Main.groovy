package org.cpuinfofetcher

import org.cpuinfofetcher.utils.UnitsAdapter

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

    static void main(String[] args) {
        this.days_until_outdated = args.length > 0 ? Integer.parseInt(args[0]) : 28

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

        Csv.save(selected_specifications, Paths.get('..', 'specifications_out', 'specifications_filtered.csv'))
        this.selected_specifications = selected_specifications
        LOGGER.info('Saved final results.')


        LOGGER.exiting('Main', 'main')
    }

}
