

import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
  // Sincroniza todas las versiones de Kotlin a la 2.2.21
  kotlin("jvm") version "2.2.21"
  kotlin("plugin.spring") version "2.2.21"
  kotlin("plugin.jpa") version "2.2.21" // Actualizado de 1.9.22

  id("org.springframework.boot") version "4.0.1"
  id("io.spring.dependency-management") version "1.1.7"
  id("org.flywaydb.flyway") version "11.20.1"
  //id("org.graalvm.python") version "25.0.2"
}
// These are specific to the GraalVM Python Plugin
/*graalPy {
  packages.set(listOf("misaki[en]"))
}*/
/*configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == null) {
      // Usar una dependencia dummy con group válido
      useTarget("org.graalvm.python:dummy:1.0")
    }
  }
}*/
group = "sv.com.clip"
version = "0.0.1-SNAPSHOT"
description = "Clip Learn English"

extensions.configure<JavaPluginExtension> {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

dependencies {

  // Esto incluirá tanto el puente Java como la librería nativa
  implementation(fileTree("libs") { include("*.jar") })
/*  implementation(files(
    "libs/sherpa-onnx-native-lib-linux-x64-v1.12.10.jar",
    "libs/sherpa-onnx-v1.12.10.jar"
  ))*/

  // WebSockets y Mensajería STOMP
  implementation("org.springframework.boot:spring-boot-starter-websocket")
  implementation("org.springframework:spring-messaging")
  // --- Spring & Kotlin Core ---
  // Fuerza la presencia de la infraestructura del cliente HTTP
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin") // Corregido: antes tenías tools.jackson


  // --- IA / DJL (Configuración para evitar "Función Incorrecta" en WSL) ---
  implementation(platform("ai.djl:bom:0.31.1"))
  implementation("ai.djl:api")
  implementation("ai.djl.pytorch:pytorch-engine")
  implementation("ai.djl.huggingface:tokenizers")
  implementation("org.springframework.ai:spring-ai-starter-model-ollama")

  // Kokoro
  implementation("com.microsoft.onnxruntime:onnxruntime:1.17.1")
  implementation("net.java.dev.jna:jna:5.13.0")

  //

  // Motor políglota básico
  // Source: https://mvnrepository.com/artifact/org.graalvm.python/python-language
/*  implementation("org.graalvm.polyglot:polyglot:25.0.2")
  implementation("org.graalvm.python:python-language:25.0.2")*/
  //implementation("org.graalvm.polyglot:polyglot:24.1.1")
  //implementation("org.graalvm.polyglot:python-community:24.1.1")
  //implementation("org.graalvm.python:python-embedding:24.1.1")
  //implementation("org.graalvm.python:python-language:24.1.1")
  //implementation("org.graalvm.python:python-resources:24.1.1")

  // text chop
  implementation("org.apache.opennlp:opennlp-tools:2.3.0")

  // epub
  //implementation("nl.siegmann.epublib:epublib-core:3.1")
  implementation("nl.siegmann.epublib:epublib-core:3.1") {
    // Opcional: Excluir slf4j si entra en conflicto con Spring Boot
    exclude(group = "org.slf4j")
  }
  implementation("org.jsoup:jsoup:1.15.4")
  implementation("org.apache.commons:commons-compress:1.26.1")
 // implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")



  // FORZAR NATURALEZA LINUX PARA WSL
  // Esto evita que busque .dll de Windows y use .so de Linux
  runtimeOnly("ai.djl.pytorch:pytorch-native-cpu::linux-x86_64")

  // --- Base de Datos & Flyway ---
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  // --- Vector DB (pgvector) ---
  implementation("com.pgvector:pgvector:0.1.6")
  implementation("org.hibernate.orm:hibernate-vector:7.2.0.Final")

  // --- Spring Modulith ---
  // Importa el BOM aquí mismo para asegurar que el classpath de :api lo vea
  implementation(platform("org.springframework.modulith:spring-modulith-bom:2.0.1"))
  implementation("org.springframework.modulith:spring-modulith-starter-core")
  implementation("org.springframework.modulith:spring-modulith-events-api")
  runtimeOnly("org.springframework.modulith:spring-modulith-starter-jpa")

  // --- Spring Security
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("io.jsonwebtoken:jjwt-api:0.12.5")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

  // --- JMolecules (DDD) ---
  implementation(platform("org.jmolecules:jmolecules-bom:2025.0.2"))
  implementation("org.jmolecules:jmolecules-ddd")
  implementation("org.jmolecules:kmolecules-ddd")
  implementation("org.jmolecules.integrations:jmolecules-spring")
  implementation("org.jmolecules.integrations:jmolecules-jpa")

  // --- Utilidades ---
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("com.github.therealbush:translator:1.0.2")
  implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")
  implementation("com.opencsv:opencsv:5.9")
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")
  // Core de Lucene (contiene CharTermAttribute)
  implementation("org.apache.lucene:lucene-core:9.12.1")
  // Módulo de análisis (contiene EnglishAnalyzer)
  implementation("org.apache.lucene:lucene-analysis-common:9.12.1")
  // detect mimeType
  implementation("org.apache.tika:tika-core:2.9.1")
  // Ffmpeg
  implementation("ws.schild:jave-core:3.5.0")
  implementation("ws.schild:jave-nativebin-linux64:3.5.0")

  // --- Testing ---
  testImplementation("org.springframework.modulith:spring-modulith-starter-test")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}
dependencyManagement {
  imports {
    // Usa la versión más reciente disponible en 2026
    mavenBom("org.springframework.ai:spring-ai-bom:2.0.0-M1")
  }
}

tasks.test {
  useJUnitPlatform()
}

sourceSets {
  main {
    java {
      srcDirs("src/main/java")
    }
  }
}
