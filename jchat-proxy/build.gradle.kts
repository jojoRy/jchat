plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "8.3.5"
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencies {
    implementation(project(":jchat-common"))
    compileOnly("com.velocitypowered", "velocity-api", "3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered", "velocity-api", "3.4.0-SNAPSHOT")
    compileOnly("kr.hqservice", "hqframework-proxy-velocity-core", "2.0.1-SNAPSHOT")
    compileOnly("kr.hqservice", "hqframework-velocity-netty", "2.0.1-SNAPSHOT")
    compileOnly("io.lumine", "MythicLib-dist", "1.7.1-SNAPSHOT")
    compileOnly("net.Indyuce", "MMOCore-API", "1.13.1-SNAPSHOT")
    implementation("org.yaml:snakeyaml:2.2")
    implementation(kotlin("stdlib"))
}

tasks.shadowJar {
    archiveBaseName.set("jchat-proxy")
    archiveVersion.set("1.0.2")
    minimize()
}
