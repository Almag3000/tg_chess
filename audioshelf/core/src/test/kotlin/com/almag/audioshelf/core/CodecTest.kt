package com.almag.audioshelf.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CodecTest {
    @Test fun roundTrip() {
        val lib = seedLibrary()
        assertEquals(lib, LibraryCodec.decode(LibraryCodec.encode(lib)))
    }

    @Test fun webStatusCodesAndUnknownFields() {
        val json = """{"books":[{"id":"x","title":"T","author":"A","series":"S","status":"prog","note":"","extra":1}],"future":true}"""
        val lib = LibraryCodec.decode(json)
        assertEquals(Status.LISTENING, lib.books.single().status)
        assertEquals(listOf("A"), lib.authors)
        assertEquals(listOf(Series("S", "A")), lib.series)
    }

    @Test fun repairDropsBrokenRows() {
        val json = """{"books":[{"id":"x","title":" ","author":"A"},{"id":"y","title":"T","author":"B","rating":9,"progress":-3},{"id":"y","title":"dup","author":"B"}]}"""
        val lib = LibraryCodec.decode(json)
        assertEquals(1, lib.books.size)
        assertEquals(5, lib.books[0].rating)
        assertEquals(0, lib.books[0].progress)
    }
}
