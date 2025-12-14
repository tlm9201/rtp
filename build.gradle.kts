plugins {
    kotlin("jvm") version "2.1.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `java-library`
    `maven-publish`
}

group = "com.timomcgrath"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.glaremasters.me/repository/towny/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("io.github.townyadvanced.commentedconfiguration:CommentedConfiguration:1.0.2")
    compileOnly("com.palmergames.bukkit.towny:towny:0.101.1.0")
    implementation("fr.formiko.mc.biomeutils:biomeutils:1.1.8")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    archiveFileName.set("${project.name}-${project.version}.jar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}


publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        artifactId = "rtp"
    }
}
