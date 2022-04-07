rootProject.name = "anko-generator"
include(":generator")

pluginManagement {
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        gradlePluginPortal()
    }
}