package sv.com.clip

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import sv.com.clip.config.TestConfig

@SpringBootTest
@Import(TestConfig::class)
class ApiApplicationTests {

	@Test
	fun contextLoads() {
	}

}
