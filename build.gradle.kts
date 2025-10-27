plugins {
    kotlin("jvm") version "2.1.21" apply false
}

allprojects {
    group = "kr.jjory"
    version = "1.0.2"

    repositories {
        mavenCentral()
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.velocitypowered.com/snapshots/")
        maven("https://maven.hqservice.kr/repository/maven-public")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
        maven("https://insert-koin.io/")
    }
}
