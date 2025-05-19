package org.cpuinfofetcher
import org.cpuinfofetcher.utils.Helpers
import org.dflib.DataFrame
import org.dflib.Printers
import java.time.LocalDateTime
import static org.dflib.Exp.$col
import java.time.Year

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


/**
 * Removes duplicate rows from the DataFrame based on the 'name' column.
 *
 * @param specifications the DataFrame to process.
 * @return a DataFrame with unique rows by 'name'.
 */

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
    int currentYear = Year.now().getValue()

    DataFrame specifications_filtered = specifications.rows({ row ->
        def year = row.get("Launch Year/Last Time Buy") as Integer
        return year != null && year >= currentYear - 10
    }).select()

    DataFrame aggregatedDf = specifications_filtered.group('intended_usage').agg(
            $col('intended_usage').first().as('intended_usage'),
            $col('cores').castAsInt().avg().as("avg_cores"),
            $col('threads').castAsInt().avg().as("avg_threads"),
            $col('tdp (W)').castAsDouble().avg().as("avg_tdp"))

    DataFrame local_server_rows = aggregatedDf.rows({ it.get('intended_usage') == 'local' || it.get('intended_usage') == 'compute cluster' })
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

    return specifications
}



static DataFrame extractUniformYearColumn(DataFrame df) {
    // Helper method to extract year from various formats
    // Match and parse specific patterns
    def extractYear = { value ->
        if (value == null || value.toString().trim().isEmpty()) {
            return null // Handle null or empty input
        }

        value = value.toString().trim() // Ensure the value is a trimmed string

        if (value =~ /^\d{4}$/) { // Matches "2023" (4-digit year)
            return value
        } else if (value =~ /^\d{1,2}\/\d{1,2}\/\d{2,4}$/) { // Matches "3/22/22" or "03/15/2021"
            def parts = value.split("/")
            return parts[-1].length() == 2 ? "20${parts[-1]}" : parts[-1] // Handle yy or yyyy
        } else if (value =~ /^Q[1-4]'\d{2}$/) { // Matches "Q2'22" (quarter-year format with short year)
            return "20" + value[-2..-1]
        } else if (value =~ /^Q[1-4]\d{4}$/) { // Matches "Q12021" (quarter-year full format)
            return value[-4..-1]
        } else if (value =~ /^Q[1-4]\s?\d{2,4}$/) { // Matches "Q217" or "Q2 2026"
            def extractedYear = value.replaceAll(/Q[1-4]\s?/, "") // Extract the number after "Q"
            return extractedYear.length() == 2 ? "20" + extractedYear : extractedYear
        } else if (value =~ /^[1-4]Q\s\d{4}$/) { // Matches "3Q 2016"
            return value[-4..-1]
        } else if (value =~ /^[A-Za-z]+\s\d{4}$/) { // Matches "June 2017" (Month-Year format)
            return value.replaceAll(/[^\d]/, "") // Remove non-digit characters, keep year
        } else if (value =~ /^\d{1,2}\/\d{4}$/) { // Matches "06/2017" (MM/YYYY format)
            return value.split("/")[1] // Extract year from MM/YYYY format
        } else if (value =~ /^[1-4]Q\d{2}$/) { // Matches "2Q18" (Quarter-Year with short year)
            return "20" + value[-2..-1]
        } else if (value =~ /^\d{2}'\d{2}$/) { // Matches "04'16" (Month-Year short year MM'YY)
            return "20" + value[-2..-1] // Extract "16" and convert to "2016"
        } else if (value =~ /\b\d{4}\b/) { // Matches all 4-digit years in a string
            def matcher = (value =~ /\b\d{4}\b/) // Find all 4-digit years
            def allYears = matcher.collect { it.toInteger() } // Collect all matched years as integers
            return allYears.min() // Return the earliest year (minimum)
        } else {
            return null // Return null if no patterns match
        }
    }

    def new_df = DataFrame.empty("Launch Year/Last Time Buy")

    // Create a new column that contains the uniform year format
    df.rows().select().each { row ->
        def launchDate = row.get("Launch Date") // Get value for "Launch Date"
        def lastTimeBuy = row.get("Last Time Buy") // Get value for "Last Time Buy"

        // Check for "Launch Date" or fall back to "Last Time Buy"
        Integer year = extractYear(launchDate ?: lastTimeBuy) as Integer
        new_df = new_df.addRow("Launch Year/Last Time Buy": year)
    }
    //new_df = df.hConcat(new_df).colsExcept("Launch Date", "Last Time Buy").select()
    new_df = df.hConcat(new_df)

    // Iterate through the rows and print results
    for (int i = 0; i < new_df.height(); i++) {
        def name = new_df.getColumn("name").get(i) // # Processor name
        def launchDate = new_df.getColumn("Launch Date").get(i) // Original "Launch Date"
        def lastTimeBuy = new_df.getColumn("Last Time Buy").get(i) // Original "Last Time Buy"
        def uniformYear = new_df.getColumn("Launch Year/Last Time Buy").get(i) // New "Uniform Launch Year"


        if ((!(launchDate == null || launchDate.toString().trim().isEmpty())
                || !(lastTimeBuy == null || lastTimeBuy.toString().trim().isEmpty()))
                && (uniformYear == null || uniformYear.toString().trim().isEmpty())) {
            println "Warning: ${name} -> No valid uniform year found! (Launch Date: $launchDate, Last Time Buy: $lastTimeBuy, Uniform Year: $uniformYear)"
        }

        //println "Processor: $name, Uniform Year: $uniformYear (Launch Date: $launchDate, Last Time Buy: $lastTimeBuy)"
        //if (uniformYear != null) {
        //    println "Row ${i + 1}: Uniform Year is $uniformYear (Launch Date: $launchDate, Last Time Buy: $lastTimeBuy)"
        //} else {
        //    println "Row ${i + 1}: Warning - No valid value found in 'Launch Date' or 'Last Time Buy'! (Launch Date: $launchDate, Last Time Buy: $lastTimeBuy)"
        //}
    }

    //String table = Printers.tabular.toString(new_df.head(10));
    //System.out.println(table);
    return new_df
}



