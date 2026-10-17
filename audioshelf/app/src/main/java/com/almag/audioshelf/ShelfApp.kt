package com.almag.audioshelf

import android.app.Application
import com.almag.audioshelf.data.LibraryRepository

class ShelfApp : Application() {
    val repository: LibraryRepository by lazy { LibraryRepository(filesDir) }
}
