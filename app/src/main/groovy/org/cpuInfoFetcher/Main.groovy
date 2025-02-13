package org.cpuInfoFetcher

import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import org.jsoup.Connection
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.jsoup.HttpStatusException

/**
 * Execute Main function
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class Main {

    static void main(String[] args) {
        def isf = new Intel_Specifications_Fetcher(236773, 236774)
        isf.main()
    }

}