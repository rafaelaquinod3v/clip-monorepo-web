pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }
}

rootProject.name = "clip-monorepo"

include("api")
project(":api").projectDir = file("apps/api")
