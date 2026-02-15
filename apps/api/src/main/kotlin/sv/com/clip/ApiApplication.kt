package sv.com.clip

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class ApiApplication

fun main(args: Array<String>) {
  println("Hello World")
	runApplication<ApiApplication>(*args)
}
