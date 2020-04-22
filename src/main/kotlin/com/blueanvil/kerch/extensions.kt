package com.blueanvil.kerch

import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.io.OutputStream
import java.io.PrintStream


/**
 * @author Cosmin Marginean
 */
fun SearchRequestBuilder.write(outputStream: OutputStream) {
    val response = execute().actionGet()
    val printStream = PrintStream(outputStream)
    printStream.print(response.toString())
    printStream.flush()
}


