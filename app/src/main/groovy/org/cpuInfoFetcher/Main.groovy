package org.cpuinfofetcher

import java.nio.file.Files
import java.util.logging.Logger
import java.nio.file.Paths
import org.dflib.DataFrame
import org.dflib.Printers
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

    static Map<String, List<String>> specification_aliases_retain_null_entries = ['name': ['name'], "Launch Year/Last Time Buy": ["Launch Year/Last Time Buy"]]

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

        // Remove duplicate rows
        specifications = ProcessSpecificationsTable.removeDuplicates(specifications)

        // Extract a uniform year for all rows
        specifications = ProcessSpecificationsTable.extractUniformYearColumn(specifications)

        Csv.save(specifications, Paths.get('..', 'specifications_out', 'specifications.csv'))
        this.specifications = specifications
        LOGGER.info('Merged all specifications.')

        // Selecting relevant information
        CPUSpecificationsSummarizer summarizer = new CPUSpecificationsSummarizer()

        selected_specifications = summarizer.extract_selection(
                specifications,
            this.specification_aliases,
            true
        )

        // Add "Launch Year/Last Time Buy" column
        DataFrame columns_to_add = summarizer.extract_selection(
                specifications,
                this.specification_aliases_retain_null_entries,
                false
        )

        // Perform Left Join (keeping all rows of selected_specifications)
        def selected_specifications = selected_specifications.join(columns_to_add)
                .on("name")
                .colsExcept(c -> c.endsWith("_"))
                .select()

        LOGGER.info('Extracted information.')

        UnitsAdapter ua = new UnitsAdapter()
        selected_specifications = ua.unitToColumnName(selected_specifications, this.units_mapping)
        LOGGER.info('Extracted units from data.')

        // adjusts format of tdp values to make them uniform
        selected_specifications = ProcessSpecificationsTable.extractFirstNumber(selected_specifications)

        // add default TDPs
        selected_specifications = ProcessSpecificationsTable.computeDefaultTdps(selected_specifications)
        LOGGER.info('Added default TDP values.')

        Csv.save(selected_specifications, Paths.get('..', 'specifications_out', 'specifications_filtered.csv'))
        Csv.save(selected_specifications, Paths.get('..', 'nf-co2footprint', 'CPU_TDP.csv'))

        this.selected_specifications = selected_specifications
        LOGGER.info('Saved final results.')



        LOGGER.exiting('Main', 'main')
    }

}
