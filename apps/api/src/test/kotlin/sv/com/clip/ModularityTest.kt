package sv.com.clip

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModularityTest {

  //val modules = ApplicationModules.of(ApiApplication::class.java)
  val modules = ApplicationModules.of("sv.com.clip")
  @Test
  fun verificarEstructuraModular() {
    modules.forEach { println("Detected module: ${it.name} -> ${it.basePackage}") }
    modules.verify()
  }

  @Test
  fun imprimirModulos() {
    modules.forEach(::println) // Esto te dirá qué módulos detectó realmente
  }

}
