plugins {
    kotlin("jvm") version "2.0.21"
    idea
    id("com.diffplug.spotless") version "7.0.2"
    // Shadow removed — manual fatJar to avoid ASM 65 on Java 21
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
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.papermc.paper:paper-api:$paperVersion")
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

// Manual fatJar — bundles runtimeClasspath without shadow ASM
val shadowJar by tasks.registering(Jar::class) {
    archiveBaseName.set("itemlogadmin")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}

tasks.build { dependsOn(shadowJar) }
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
