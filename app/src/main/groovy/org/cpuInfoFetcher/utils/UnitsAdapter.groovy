package org.cpuinfofetcher.utils

import org.dflib.DataFrame

/**
 * Adapts columns with units to be more uniform
 */
class UnitsAdapter {

    DataFrame unitToColumnName(DataFrame df, Map<String, String[]> unit_mapping) {
        List<String> old_col_names = df.getColumnsIndex().toArray()
        // Define new column names with units
        List<String> new_col_names = []
        for (String col_name : old_col_names) {
            List<String> units = unit_mapping.get(col_name)
            if (units == null || col_name.endsWith("(${units.get(0)})")) {
                new_col_names.add(col_name)
            } else {
                new_col_names.add("${col_name} (${units.get(0)})")
            }
        }

        // Extract units from values
        def new_df = DataFrame.byArrayRow(*new_col_names).appender()
        for (int i = 0; i < df.height(); i++) {
            List row = []
            for (int j = 0; j < df.width(); j++) {
                List<String> units = unit_mapping.get(old_col_names.get(j))
                String value = df.get(j, i)
                if (units != null) {
                    for (String unit : units) {
                        value = value.replaceAll("${units.get(0)}", '').replaceAll(' ', '')
                    }
                }
                row.add(value)
            }

            new_df.append(*row)
        }
        new_df = new_df.toDataFrame()

        return new_df
    }

}
