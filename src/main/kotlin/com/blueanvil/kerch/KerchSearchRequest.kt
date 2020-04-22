package com.blueanvil.kerch

import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.io.stream.OutputStreamStreamOutput
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.search.SearchHit
import java.io.OutputStream
import java.io.PrintStream

/**
 * @author Cosmin Marginean
 */
class KerchSearchRequest(private val kerch: Kerch,
                         private val request: SearchRequest) {

    fun write(outputStream: OutputStream) {
        val printStream = PrintStream(outputStream)
        printStream.print(kerch.esClient.search(request, RequestOptions.DEFAULT).toString())
        printStream.close()
    }

    fun paging(from: Int = 0, size: Int = 10): KerchSearchRequest {
        request.source(request.source().from(from).size(size))
        return this
    }

    fun pageSize(size: Int = 10): KerchSearchRequest {
        request.source(request.source().size(size))
        return this
    }

    fun hits(): Sequence<SearchHit> {
        return kerch.esClient.search(request, RequestOptions.DEFAULT).hits.asSequence()
    }

    fun ids(): Sequence<String> {
        return scroll().map { it.id }
    }

    fun scroll(pageSize: Int = 100, keepAlive: TimeValue = TimeValue.timeValueMinutes(10)): Sequence<SearchHit> {
        request.scroll(keepAlive)
        pageSize(pageSize)

        var page = kerch.esClient.search(request, RequestOptions.DEFAULT)
        val totalHits = page.hits.totalHits.value
        var crtIndex = 0
        var scrollId = page.scrollId

        return generateSequence {
            var hit: SearchHit? = null
            if (totalHits > 0 && crtIndex < totalHits) {
                var crtPageIndex = crtIndex % request.source().size()
                hit = page.hits.hits[crtPageIndex]
                crtIndex++
                if (crtIndex % request.source().size() == 0) {
                    page = kerch.esClient.scroll(SearchScrollRequest(scrollId).scroll(keepAlive), RequestOptions.DEFAULT)
                    scrollId = page.scrollId
                }
            }

            hit
        }
    }
}