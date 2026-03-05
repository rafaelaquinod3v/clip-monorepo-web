plugins {
  id("dev.nx.gradle.project-graph") version("0.1.10")
  kotlin("jvm") version "2.1.21" apply false
  kotlin("plugin.spring") version "2.1.21" apply false
  kotlin("plugin.jpa") version "2.1.21" apply false
  id("org.springframework.boot") version "4.0.1" apply false
  id("io.spring.dependency-management") version "1.1.7" apply false
  id("org.flywaydb.flyway") version "11.20.1" apply false
}

allprojects {
  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://github.com/psiegman/mvn-repo/raw/master/releases") }
  }
}
