plugins { kotlin("jvm") }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencies { implementation(kotlin("stdlib")) }
