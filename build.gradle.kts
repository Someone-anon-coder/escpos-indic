/*
 * File:      build.gradle.kts
 * Module:    escpos-indic
 * Layer:     build
 * Purpose:   Builds, tests, formats, measures and publishes the escpos-indic library.
 * Owns:      Java 25 toolchain, test category tasks, Spotless, Checkstyle, JaCoCo, PIT, maven-publish, the golden-update switch.
 * Uses:      gradle/libs.versions.toml, config/checkstyle.
 * Used by:   CI workflows and developers.
 * Not here:  Library code.
 * Reuse:     Library template, CLAUDE.md §12.
 * Plan:      L3
 */

plugins {
    `java-library`
    `maven-publish`
    checkstyle
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.pitest)
}

group = "io.github.someone-anon-coder"
// Release builds pass -PreleaseVersion=x.y.z (release workflow) or set VERSION (JitPack).
version = (findProperty("releaseVersion") as String?) ?: System.getenv("VERSION") ?: "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror", "-parameters", "-Xpkginfo:always"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all", "-quiet")
}

dependencies {
    implementation(libs.zxing.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.jqwik)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Tag names match the meta-annotations in src/test/java/.../testing (CLAUDE.md §8.2).
val categories = mapOf(
    "Unit" to "unit", "Property" to "property", "Golden" to "golden",
    "Arch" to "arch", "Security" to "security",
)

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests = false
    finalizedBy(tasks.jacocoTestReport)
}

// -PupdateGolden rewrites the approved files; the diff is reviewed in the PR (CLAUDE.md §8.2).
tasks.withType<Test>().configureEach {
    if (project.hasProperty("updateGolden")) systemProperty("updateGolden", "true")
    systemProperty("java.awt.headless", "true")
}

categories.forEach { (name, tag) ->
    tasks.register<Test>("test$name") {
        group = "verification"
        description = "Runs only tests tagged '$tag'."
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        failOnNoDiscoveredTests = false
        useJUnitPlatform { includeTags(tag) }
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat(libs.versions.google.java.format.get())
        removeUnusedImports()
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configDirectory = layout.projectDirectory.dir("config/checkstyle")
    maxWarnings = 0
}
// Header and Javadoc rules apply to production code only.
tasks.named("checkstyleTest") { enabled = false }

jacoco { toolVersion = "0.8.14" }
tasks.jacocoTestReport { reports { xml.required = true } }
tasks.jacocoTestCoverageVerification {
    violationRules { rule { limit { counter = "LINE"; minimum = "0.90".toBigDecimal() } } }
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }

pitest {
    junit5PluginVersion = libs.versions.pitest.junit5.get()
    targetClasses = listOf("io.github.someoneanoncoder.escposindic.*")
    mutationThreshold = 70
    outputFormats = listOf("HTML", "XML")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "escpos-indic"
                description = "ESC/POS thermal printing with rasterised Hindi/Marathi text, QR and barcodes."
                url = "https://github.com/Someone-anon-coder/escpos-indic"
                licenses { license { name = "MIT"; url = "https://opensource.org/license/mit" } }
                scm { url = "https://github.com/Someone-anon-coder/escpos-indic" }
            }
        }
    }
}
