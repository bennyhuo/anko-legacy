package org.jetbrains.android.anko.artifact

import com.google.gson.Gson
import java.io.File

class Artifact(val name: String, val jars: List<File>) {
    override fun toString(): String {
        return "$name: { jars = \"${jars.joinToString()}\"}"
    }
}

class Tunes(val excludedClasses: Set<String>)