package org.cpuinfofetcher

import org.dflib.DataFrame
import org.dflib.JoinType

/**
 * Summarize extracted information into one file
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class CPUSpecificationsSummarizer {

    /**
    * Returns the last alias for each Map key with the respective alias array
    * that has a none null value in the given DataFrame.
    * @author Josua Carl
    * @version 1.0
    * @since 1.0
    */
    Map<String, String> match_column_aliases(DataFrame df, Map<String, String[]> specification_aliases) {
        Map<String, String> matched_elements = [:]
        specification_aliases.each { specification_key, aliases ->
            for (String alias : aliases) {
                for (String col_name : df.getColumnsIndex().toArray()) {
                    if (alias.toLowerCase() == col_name.toLowerCase() && df.get(col_name, 0)) {
                        matched_elements.put(specification_key, df.get(col_name, 0))
                    }
                }
            }
            if (!matched_elements.containsKey(specification_key)) {
                matched_elements.put(specification_key, null)
            }
        }

        return matched_elements
    }

    DataFrame extract_selection(DataFrame df, Map<String, String[]> specification_aliases, boolean discard_unmatched) {
        DataFrame combined = DataFrame.empty()
        for (int i = 0; i < df.height(); i++) {
            DataFrame row = df.rows(i).select()
            Map<String, String> matched_elements = match_column_aliases(row, specification_aliases)
            if (discard_unmatched && matched_elements.containsValue(null)) {
                continue
            }
            DataFrame selected_info = DataFrame
                .byArrayRow(*matched_elements.keySet().toArray())
                .appender()
                .append(*matched_elements.values().toArray())
                .toDataFrame()
            combined = combined.vConcat(JoinType.full, selected_info)
        }

        return combined
    }

}
