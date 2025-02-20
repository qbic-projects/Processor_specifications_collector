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

    /**
    * Returns the last alias for each Map key with the respective alias array
    * that has a none null value in the given DataFrame.
    * @author Josua Carl
    * @version 1.0
    * @since 1.0
    */
    Map<String, String> match_column_aliases(DataFrame df, Map<String, String[]> specification_aliases) {
        Map<String, String> matched_cols = [:]
        specification_aliases.each { specification_key, aliases ->
            for (String alias : aliases) {
                for (String col_name : df.getColumnsIndex().toArray()) {
                    if (alias.toLowerCase() == col_name.toLowerCase() && df.get(col_name, 0) != null) {
                        matched_cols.put(specification_key, col_name)
                    }
                }
            }
            if (!matched_cols.containsKey(specification_key)) {
                matched_cols.put(specification_key, 'not found')
            }
        }

        return matched_cols
    }

    DataFrame extract_selection(DataFrame df, Map<String, String[]> specification_aliases){
        DataFrame combined = DataFrame.empty()
        for (int i = 0; i < df.height(); i++) {
            DataFrame row = df.rows(i).select()
            Map<String, String> matched_cols = match_column_aliases(row, specification_aliases)
            row = row.hConcat(
                DataFrame.foldByRow('not found').of(null)
            )
            combined = combined.vConcat(
                row
                    .cols(*matched_cols.values().toArray())
                    .selectAs(*matched_cols.keySet().toArray())
            )
        }

        return combined
    }

}
