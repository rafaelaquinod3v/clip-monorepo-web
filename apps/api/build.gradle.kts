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
}

group = "sv.com.clip"
version = "0.0.1-SNAPSHOT"
description = "Clip Learn English"

extensions.configure<JavaPluginExtension> {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

dependencies {
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
