package sv.com.clip.speech.internal

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface EspeakNG : Library {
  companion object {
    // Carga la librería dinámica (libespeak-ng.so o libespeak-ng.dll)
    val INSTANCE: EspeakNG = Native.load("espeak-ng", EspeakNG::class.java) as EspeakNG
  }

  // Inicializa el motor (devuelve el sample rate o error)
  fun espeak_Initialize(output: Int, buflength: Int, path: String?, options: Int): Int

  // Configura la voz (ej: "en-us")
  fun espeak_SetVoiceByName(name: String): Int

  // Convierte texto a fonemas (IPA)
  //fun espeak_TextToPhonemes(textptr: Pointer, encoding: Int, phonememode: Int): Pointer?
  fun espeak_TextToPhonemes(text: String, encoding: Int, phonememode: Int): Pointer?
}
