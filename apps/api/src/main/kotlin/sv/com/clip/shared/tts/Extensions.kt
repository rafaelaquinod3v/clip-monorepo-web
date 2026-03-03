package sv.com.clip.shared.tts

fun String.cleanForTTS(): String {
  return this
    // 1. Normalizar comillas elegantes/inclinadas
    .replace("[\u201C\u201D\u201E\u201F\u2033\u2036]".toRegex(), "\"")
    // 2. Normalizar apóstrofes
    .replace("[\u2018\u2019\u201A\u201B\u2032\u2035]".toRegex(), "'")
    // 3. Reemplazar guiones largos (em-dash) por comas o puntos para mejorar la entonación
    .replace("\u2014", ", ")
    // 4. Eliminar caracteres especiales no deseados (opcional)
    .replace("[#*|]".toRegex(), "")
    // 5. Normalizar espacios en blanco
    .replace("\\s+".toRegex(), " ")
    .trim()
}
