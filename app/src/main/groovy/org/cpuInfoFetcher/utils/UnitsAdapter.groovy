package org.cpuinfofetcher.utils

import org.dflib.DataFrame

/**
 * Adapts columns with units to be more uniform
 */
class UnitsAdapter {

    static DataFrame unitToColumnName(DataFrame df, Map<String, String[]> unit_mapping) {
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

    /**
     * Extracts the first numeric value from the 'tdp (W)' column and updates the DataFrame.
     *
     * Examples of extraction:
     * - "15-30"   --> 15
     * - "1.5/20"  --> 1.5
     * - "3.1--6"  --> 3.1
     *
     * @param df the input DataFrame
     * @return a DataFrame with the updated 'tdp (W)' column containing only the first numeric value
     */
    static DataFrame extractFirstNumber(DataFrame df) {
        DataFrame old_df = df.cols().selectAs(Map.of("tdp (W)", "tdp old"))
        def new_df = DataFrame.empty("tdp (W)")

        for (int i = 0; i < old_df.height(); i++) {
            String tdp_value = old_df.rows(i).select().get("tdp old", 0)
            // Use the matcher to extract the first number
            def matcher = tdp_value =~ /^[0-9]*\.?[0-9]+/ // Regex to match the first number (integer or decimal)
            def new_tdp_value = matcher.find() ? Double.parseDouble(matcher.group(0)) : null
            new_df = new_df.addRow("tdp (W)": new_tdp_value)

        }
        new_df = old_df.hConcat(new_df).colsExcept('tdp old').select()

        return new_df
    }

}
