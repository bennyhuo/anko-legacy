plugins {
    kotlin("jvm") version "1.6.20" apply false
}

group = "com.bennyhuo.kotlin"
version = "1.0-SNAPSHOT"

subprojects {
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
    }
}