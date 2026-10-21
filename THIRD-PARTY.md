# Fremde Bestandteile

Diese App enthält freie Software und freie Daten von anderen. Die Texte stehen hier,
damit sich jeder ansehen kann, was er benutzen darf.

## Sprachausgabe

- sherpa-onnx (k2-fsa), Lizenz Apache-2.0, https://github.com/k2-fsa/sherpa-onnx.
  Die Dateien `app/src/main/jniLibs/*/libsherpa-onnx-jni.so` und `app/src/main/java/com/k2fsa/sherpa/onnx/Tts.kt`
  stammen aus der Version 1.13.8.
- ONNX Runtime (Microsoft), Lizenz MIT, `libonnxruntime.so`, ist Teil des sherpa-onnx-Pakets.
- espeak-ng, Lizenz GPL-3.0. Die Aussprachedaten werden auf Wunsch nachgeladen und stecken
  auch in `libsherpa-onnx-jni.so`. Deshalb steht dieses Projekt unter der GPL-3.0 (Datei `LICENSE`).
- Stimmen (werden nur auf Wunsch geladen): Thorsten-Voice (CC0), Fahrettin von NabuCasa (CC0),
  Cori von Bryce Beattie (gemeinfrei, LibriVox). Alle als Piper-Modelle.

## Grafiken

- Spielgrafiken von Kenney (www.kenney.nl), Lizenz CC0.
