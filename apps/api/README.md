curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk list java | grep graal
sdk install java 21.0.2-graalce
java -version


todo: long term rust kokoro tts & espeak-ng 


https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX

-- kokoro
//sudo apt-get install libespeak-ng-dev
sudo apt update && sudo apt install espeak-ng espeak-ng-data libespeak-ng-dev -y
sudo ln -s /usr/lib/x86_64-linux-gnu/espeak-ng-data /usr/share/espeak-ng-data
# Crear la carpeta donde eSpeak busca por defecto
sudo mkdir -p /usr/share/espeak-ng-data

# Copiar TODO el contenido desde la ruta de librerías a la ruta de datos
# (Usamos -r para copiar subcarpetas como 'lang', 'voices', etc.)
sudo cp -r /usr/lib/x86_64-linux-gnu/espeak-ng-data/* /usr/share/espeak-ng-data/

# Dar permisos de lectura (por si acaso Spring no puede leerlos)
sudo chmod -R 755 /usr/share/espeak-ng-data


-- For the direct lemma search
CREATE INDEX idx_lexical_lemma ON lexical_entries (lemma);

-- For the forms search (VERY IMPORTANT)
CREATE INDEX idx_forms_written_rep ON lexical_entry_forms (written_representation);

-- This allows the DB to jump from ILI -> Lexicon -> Entry in one step
CREATE INDEX idx_senses_ili_lexicon ON lexical_senses (concept_ili, lexical_entry_id);

-- This makes the "le.lemma OR forms" check fast
CREATE INDEX idx_le_lemma_lexicon ON lexical_entries (lemma, lexicon_id);
CREATE INDEX idx_lf_written_rep ON lexical_entry_forms (written_representation);


. 
3. Estrategia de "Caché de Vocabulario" (Control de Costos)

Model Hibrido - Api fuente DB Cerebro
Paso 1: El usuario busca una palabra.
Paso 2: Tu sistema busca en tu propia base de datos (donde ya tienes almacenadas las palabras que otros usuarios han consultado antes).
Paso 3: Si la palabra no existe, consultas la API, guardas el resultado en tu base de datos y se lo entregas al usuario.
Resultado: Solo pagas por la consulta de una palabra una vez en toda la vida de tu app. Con el tiempo, tu dependencia de la API tiende a cero.

2. Gestión del Progreso del Usuario (Dominio)
   Para llevar el control de qué palabras conoce cada usuario, necesitas obligatoriamente tu propia base de datos. Las APIs no están diseñadas para guardar el historial de aprendizaje. Debes estructurar tu base de datos así:
   Tabla de Palabras: Definición, nivel de dificultad (A1-C2), ejemplos.
   Tabla de Progreso: ID del usuario, ID de la palabra, nivel de dominio (0-100%), fecha de último repaso.
   Control: Esto te permite implementar algoritmos de Repetición Espaciada (SRS) como los de Anki, que son el estándar de oro en 2026 para el aprendizaje de idiomas.
   Neo4j para que ???????
Modelos de lenguaje
locales Llama3 o Mistral
sudo systemctl start ollama
3. Evitar la Dependencia Total (Vendor Lock-in)
   Api: Google Oxford DeepL abstraccion
4. Cuándo sí usar la API (Funciones Premium)
   Puedes dejar la API activa solo para funciones de alto valor que justifiquen el costo:
   Pronunciación por IA (Text-to-Speech): Donde la calidad de APIs como las de ElevenLabs es muy superior a lo que podrías procesar localmente.
   Corrección de gramática en tiempo real: Para analizar párrafos enteros que el usuario escribe.
   ElevenLabs vs chatterbox

En resumen: Usa una API para poblar tu propia base de datos inicialmente, 
pero construye tu propia arquitectura para gestionar el progreso de los usuarios 
y el almacenamiento de términos. Así tienes el control total del activo más valioso de tu app: la data del aprendizaje de tus usuarios.

src/main/kotlin/sv/com/clip/vocabulary/          <-- Raíz del Módulo (API Pública)
│
├── [Público] VocabularyCreator.kt               <-- Interfaz (Caso de Uso / Command)
├── [Público] VocabularyQueries.kt               <-- Interfaz (Query / Lectura)
│
├── domain/                                      <-- El Corazón (Puro, sin Spring)
│   ├── model/
│   │   ├── Lexicon.kt                           <-- @AggregateRoot
│   │   ├── LexiconId.kt                         <-- Value Object
│   │   └── Language.kt                          <-- Enum/VO
│   ├── repository/
│   │   └── internal LexiconRepository.kt        <-- Interfaz (Puerto de persistencia)
│   └── services/
│       └── internal LexiconRegistry.kt          <-- Servicio de Dominio (Invariantes)
│
├── application/                                 <-- Orquestación (Casos de Uso)
│   ├── internal CreateLexiconUseCase.kt         <-- Implementa VocabularyCreator
│   └── internal LexiconQueryService.kt          <-- Implementa VocabularyQueries
│
├── infrastructure/                              <-- Detalle técnico (Spring, JPA)
│   ├── persistence/
│   │   ├── LexiconEntity.kt                     <-- JPA Entity (implementa Persistable)
│   │   ├── SpringDataLexiconRepository.kt       <-- Spring Data Interface
│   │   └── LexiconRepositoryAdapter.kt          <-- Implementa LexiconRepository
│   └── web/
│       └── LexiconController.kt                 <-- @RestController
│
└── config/                                      <-- Cableado
└── internal VocabularyModuleConfig.kt       <-- @Configuration (Define @Beans)



src/main/kotlin/sv/com/clip
│
├── library/               <-- Gestión de Fuentes (Videos, Libros, Noticias)
│   ├── api/               <-- Endpoints para subir videos, scrapear noticias
│   ├── application/       <-- Use Cases: "ProcessVideoContent", "ImportNews"
│   ├── domain/            <-- Entities: ContentSource, MediaResource (Value Objects para URL, Duración)
│   └── infrastructure/    <-- Adapters: YouTubeClient, S3Storage, NewsScraper
│
├── course/                <-- Gestión de la estructura educativa
│   ├── api/               <-- Endpoints para que el alumno vea su curso
│   ├── application/       <-- Use Cases: "AddContentToLesson"
│   ├── domain/            <-- Entities: Course, Lesson (solo referencias al ID del contenido)
│   └── infrastructure/    <-- Persistencia de la estructura del curso
│
├── student/               <-- Gestión de Estudiantes
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
│
├── progression/           <-- Gamificación/Motivación (Puntos, Logros, Niveles, Rachas)
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
│
├── vocabulary/            <-- (Palabras, Análisis de texto)
│
└── shared/                <-- Kernel Diferido (Código común a módulos)
