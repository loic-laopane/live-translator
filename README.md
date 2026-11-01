# Live Translator (Android)

## 📲 Télécharger

**[⬇️ Télécharger le dernier APK](https://github.com/loic-laopane/live-translator/releases/latest/download/live-translator.apk)**

L'APK est reconstruit et publié automatiquement après chaque merge sur `main`
(historique dans les [Releases](https://github.com/loic-laopane/live-translator/releases)).
Sur le téléphone, autorisez l'installation depuis des sources inconnues pour l'installer.

## Présentation

Application Android qui traduit **en direct** ce que vous écrivez vers la langue de votre choix.

- **Aucune langue source à choisir** : la langue du texte est détectée automatiquement pendant la frappe.
- Vous choisissez seulement la **langue de destination** (mémorisée entre deux lancements).
- Traduction **sur l'appareil** avec Google ML Kit : gratuit, sans clé API, et hors-ligne une fois
  le modèle de la paire de langues téléchargé (~30 Mo, une seule fois par langue).
- Plus de 50 langues supportées.

## Fonctionnement

1. À chaque frappe (avec un délai de 350 ms), le texte est analysé par **ML Kit Language ID**.
2. La langue détectée est affichée sous le champ de saisie.
3. Le texte est traduit avec **ML Kit Translation** vers la langue cible ; le modèle est téléchargé
   automatiquement s'il manque.
4. Si un texte est trop court pour être identifié (ex. un seul mot ambigu), la dernière langue
   détectée est réutilisée.

Code principal :
- `app/src/main/java/com/livetranslator/app/LiveTranslatorEngine.kt` – détection + traduction
- `app/src/main/java/com/livetranslator/app/TranslatorViewModel.kt` – logique « live » (debounce, états)
- `app/src/main/java/com/livetranslator/app/ui/TranslatorScreen.kt` – interface Jetpack Compose

## Compiler

Prérequis : Android Studio (ou JDK 17 + Android SDK 35).

```bash
./gradlew assembleDebug
# APK : app/build/outputs/apk/debug/app-debug.apk
```

Ou ouvrez le dossier dans Android Studio et lancez sur un appareil/émulateur (Android 8.0+).

## Intégration continue

Le workflow GitHub Actions **Build APK** :
- compile l'APK sur chaque pull request (vérification avant merge) ;
- après chaque merge sur `main`, publie l'APK dans une nouvelle Release GitHub marquée « latest »,
  ce qui met à jour le lien de téléchargement ci-dessus.

Tous les APK sont signés avec la même clé (`app/debug.keystore`), ce qui permet d'installer une
nouvelle version par-dessus l'ancienne sans désinstaller.
