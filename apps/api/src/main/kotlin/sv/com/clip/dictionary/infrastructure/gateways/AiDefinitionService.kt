package sv.com.clip.dictionary.infrastructure.gateways

import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.stereotype.Service
import sv.com.clip.dictionary.api.AiDataDTO

@Service
class AiDefinitionService(
  private val chatModel: ChatModel
) {
  // 1. Create a converter for our DTO
  private val converter = BeanOutputConverter(AiDataDTO::class.java)

  fun getAiDefinition(term: String): AiDataDTO? {
    try {
      // 2. Define the System Prompt to guide Gemma
      val message = """
        You are a linguistic expert.
        Analyze the English word '{term}' for a Spanish speaker.

        INSTRUCTIONS:
        1. Source Lemma: The root form of the word.
        2. Target Lemma: The main Spanish translation.
        {format}
    """.trimIndent()

      // 3. Create the template with the expected JSON format
      val promptTemplate = PromptTemplate(message)
      val prompt = promptTemplate.create(
        mapOf(
          "term" to term,
          "format" to converter.format // This tells Gemma exactly how the JSON should look
        )
      )

      // 4. Call the model
      val response = chatModel.call(prompt)
      val jsonResponse  = response.result.output.text

      // 5. Parse the JSON back to our DTO
      return converter.convert(jsonResponse!!)
    } catch (e: Exception) {
      // Log error and return null so the flow can handle the fallback
      println("Error calling AI: ${e.message}")
      return null
    }
  }

}
