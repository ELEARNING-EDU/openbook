package com.elearning.openbook.epub

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A very small DOM helper layer.
 *
 * Instead of relying on `getElementsByTagNameNS` (which behaves differently
 * depending on how well an EPUB declares its namespaces), everything walks the
 * document and matches on the *local* name, so `dc:title`, `ns:title` and a bare
 * `title` are all found.
 */
internal object Xml {

    fun parse(bytes: ByteArray): Document? {
        val stripped = stripProlog(bytes)
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.isExpandEntityReferences = false
        factory.isValidating = false
        trySetFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false)
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false)
        return try {
            val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(stripped))
            if (document.documentElement == null) null else document
        } catch (t: Throwable) {
            // Some real-world EPUBs carry HTML entities that are not valid XML.
            // Callers fall back to regex extraction when this returns null.
            null
        }
    }

    fun parse(string: String): Document? = parse(string.toByteArray(Charsets.UTF_8))

    private fun trySetFeature(factory: DocumentBuilderFactory, feature: String, value: Boolean) {
        runCatching { factory.setFeature(feature, value) }
    }

    /**
     * Removes the XML declaration and any DOCTYPE, whose internal subsets often
     * contain entities Android's parser refuses to resolve.
     */
    private fun stripProlog(bytes: ByteArray): ByteArray {
        var cleaned = String(bytes, Charsets.UTF_8)
        if (cleaned.startsWith("\uFEFF")) cleaned = cleaned.substring(1)
        cleaned = cleaned.replace(Regex("(?s)<\\?xml.*?\\?>"), "")
        cleaned = cleaned.replace(Regex("(?is)<!DOCTYPE[^\\[>]*(\\[[^\\]]*])?[^>]*>"), "")
        return cleaned.toByteArray(Charsets.UTF_8)
    }

    fun descendants(root: Node, name: String): List<Element> {
        val out = ArrayList<Element>()
        collect(root, name, out)
        return out
    }

    private fun collect(node: Node, name: String, out: MutableList<Element>) {
        val children = node.childNodes ?: return
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element) {
                if (child.hasLocalName(name)) out.add(child)
                collect(child, name, out)
            }
        }
    }

    fun children(root: Node?, name: String): List<Element> {
        val out = ArrayList<Element>()
        val children = root?.childNodes ?: return out
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element && child.hasLocalName(name)) out.add(child)
        }
        return out
    }

    fun child(root: Node?, name: String): Element? = children(root, name).firstOrNull()

    fun attr(element: Element?, name: String): String? {
        if (element == null) return null
        if (element.hasAttribute(name)) return element.getAttribute(name)
        // Namespaced attributes (xml:lang, epub:type, ...) come back prefixed.
        val attributes = element.attributes ?: return null
        for (i in 0 until attributes.length) {
            val item = attributes.item(i)
            val nodeName = item.nodeName ?: continue
            if (nodeName.equals(name, ignoreCase = true)) return item.nodeValue
            if (nodeName.substringAfterLast(':').equals(name, ignoreCase = true)) return item.nodeValue
        }
        return null
    }

    fun text(element: Node?): String = element?.textContent?.trim().orEmpty()

    /** Text of the element itself, ignoring any child markup. */
    fun onlyText(element: Node?): String {
        val node = element ?: return ""
        var out = ""
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.TEXT_NODE || child.nodeType == Node.CDATA_SECTION_NODE) {
                out += child.nodeValue ?: ""
            }
        }
        return out.trim()
    }
}

internal fun Element.hasLocalName(expected: String): Boolean {
    val ln = localName ?: nodeName?.substringAfterLast(':')
    return ln.equals(expected, ignoreCase = true)
}

internal fun Element.attr(name: String): String? = Xml.attr(this, name)
