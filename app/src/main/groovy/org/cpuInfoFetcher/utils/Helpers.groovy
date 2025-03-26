package org.cpuinfofetcher.utils

import org.dflib.DataFrame

static boolean assertEqualDF(DataFrame df1, DataFrame df2) {
    assert df1.size() == df2.size()
    assert df1.getColumnsIndex().toArray() == df2.getColumnsIndex().toArray()
    for (int i = 0; i < df1.width(); i++) {
        for (int j = 0; j < df1.height(); j++) {
            assert df1.get(i, j) == df2.get(i, j)
        }
    }
    return true
}

/**
 * Rounds a Double value to two decimal places.
 *
 * @param value the Double to round
 * @return the value rounded to two decimal places
 */
static Double round(Double value) {
    Double rounded_value = Math.round(value * 100.0) / 100.0
    return rounded_value
}

