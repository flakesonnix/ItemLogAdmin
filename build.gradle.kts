plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    idea
    id("com.diffplug.spotless") version "7.0.2"
}
group = "com.itemlogadmin"
version = "1.0.0-SNAPSHOT"
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
val paperVersion = "1.21.10-R0.1-SNAPSHOT"
dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("com.mysql:mysql-connector-j:9.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
kotlin { jvmToolchain(21) }
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }
tasks.processResources { filteringCharset = "UTF-8" }
tasks.jar { archiveBaseName.set("itemlogadmin") }
tasks.shadowJar {
    archiveBaseName.set("itemlogadmin")
    archiveClassifier.set("")
    mergeServiceFiles()
}
tasks.build { dependsOn(tasks.shadowJar) }
tasks.test { useJUnitPlatform() }
idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.5.0").editorConfigOverride(mapOf("indent_size" to "4", "continuation_indent_size" to "4", "max_line_length" to "off", "ktlint_standard_max-line-length" to "disabled", "ktlint_standard_no-wildcard-imports" to "disabled"))
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.kts", "gradle/*.kts")
        ktlint("1.5.0").editorConfigOverride(mapOf("ktlint_standard_max-line-length" to "disabled"))
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("misc") {
        target("*.md", "*.yml", "*.yaml", "*.json", ".editorconfig")
        trimTrailingWhitespace()
        endWithNewline()
        leadingTabsToSpaces(2)
    }
}
