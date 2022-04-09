plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish") version "0.19.0"
}

group = property("GROUP").toString()
version = property("VERSION_NAME").toString()

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib")

    api("com.github.javaparser:javaparser-core:3.0.1")
    api("com.google.code.gson:gson:2.8.0")
    api("org.slf4j:slf4j-simple:1.7.22")

    api("org.ow2.asm:asm:9.3")
    api("org.ow2.asm:asm-tree:9.3")

    api("org.jetbrains.kotlinx:kotlinx.dom:0.0.10")
}