package org.cpuinfofetcher

import org.dflib.DataFrame
import org.dflib.JoinType
import org.dflib.Printers
import org.dflib.csv.Csv

/**
 * Summarize extracted information into one file
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class CPUSpecificationsSummarizer {

    // Mapping possible naming schenes for attributes
    Map<String, String[]> specification_aliases = [
        'tgb': ['Processor Base Power', 'tdp', 'thermal design power', 'Scenario Design Power', 'SDP'],
        'cores': ['cores'],
        'threads': ['threads']
    ]

    DataFrame match_column_aliases(DataFrame df, Map<String, String[]> specification_aliases) {
        // Find columns with desired info
        Map<String, String> matched_cols = [:]
        specification_aliases.each { specification_key, aliases ->
            matched_cols.put(
                specification_key,
                df.getColumnsIndex().toArray().find { col_name ->
                    aliases.any { alias -> col_name.toLowerCase().contains(alias.toLowerCase()) }
                }
            )
        }

        return matched_cols
    }
}
