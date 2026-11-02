package com.elearning.openbook.epub

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentTitleTest {

    @Test
    fun ignoresAGenericIndexTitleAndUsesTheHeading() {
        val html = """
            <html><head><title>index</title></head>
            <body><h1>HỒI TƯỞNG</h1><p>…</p></body></html>
        """.trimIndent()

        assertEquals("HỒI TƯỞNG", EpubArchive.documentTitle(html))
    }

    @Test
    fun keepsARealTitle() {
        val html = "<html><head><title>Chương 3</title></head><body><h1>Khác</h1></body></html>"

        assertEquals("Chương 3", EpubArchive.documentTitle(html))
    }

    @Test
    fun fallsBackToNothingWhenTheDocumentSaysIndexAndHasNoHeading() {
        val html = "<html><head><title>Index</title></head><body><p>…</p></body></html>"

        assertEquals("", EpubArchive.documentTitle(html))
    }
}
