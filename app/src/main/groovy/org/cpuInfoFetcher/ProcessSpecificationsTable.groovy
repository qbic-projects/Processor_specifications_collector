package org.cpuinfofetcher
import org.cpuinfofetcher.utils.Helpers
import org.dflib.DataFrame
import java.time.LocalDateTime
import static org.dflib.Exp.$col

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

    return specifications
}