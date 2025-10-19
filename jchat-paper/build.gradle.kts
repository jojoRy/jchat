plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "8.3.5"
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencies {
    implementation(project(":jchat-common"))
    compileOnly("io.papermc.paper", "paper-api", "1.21.1-R0.1-SNAPSHOT")
    compileOnly("kr.hqservice", "hqframework-bukkit-core", "2.0.1-SNAPSHOT")
    compileOnly("kr.hqservice", "hqframework-bukkit-command", "2.0.1-SNAPSHOT")
    compileOnly("kr.hqservice", "hqframework-bukkit-scheduler", "2.0.1-SNAPSHOT")
    compileOnly("kr.hqservice", "hqframework-global-netty", "2.0.1-SNAPSHOT")
    compileOnly("net.luckperms", "api", "5.4")
    compileOnly("me.clip", "placeholderapi", "2.11.5")
}

tasks.shadowJar {
    archiveBaseName.set("jchat-paper")
    archiveVersion.set("1.0.2")
    minimize()
}
