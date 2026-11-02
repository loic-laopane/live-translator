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
- **Correction orthographique et grammaticale** : les fautes sont soulignées dans le texte, une
  **version corrigée** est proposée (applicable en un clic, en entier ou faute par faute).
- **Langue maternelle** demandée au premier lancement (modifiable via l'icône en haut à droite) :
  elle apparaît en tête des listes de langues et sert à expliquer les règles et traduire les cours.
- **Règles expliquées dans votre langue** : chaque faute est accompagnée de la règle de grammaire
  ou du piège concerné (ex. « Confusion a/à »), avec un lien « En savoir plus » quand il existe.
  L'explication, le nom de la règle et sa catégorie sont traduits dans votre langue maternelle.
- **Apprendre : cours de langues** (anglais, allemand, espagnol, chinois, français) avec exercices (voir ci-dessous).

## Apprendre : cours de langues

L'onglet **Apprendre** propose des leçons courtes (niveaux du CECRL, A1 à B2) : une règle, des
**exemples à écouter** (synthèse vocale du téléphone), puis des **exercices** à choix multiple ou à
écrire, corrigés et expliqués. Le meilleur score de chaque leçon est mémorisé.

| Langue | A1 | A2 | B1 |
|---|---|---|---|
| Anglais | Le verbe « to be » | Present simple / continuous | Present perfect · Conditionnelles |
| Allemand | Genre des noms (der/die/das) | Les cas (déclinaisons) · Prépositions et cas | Place du verbe |
| Espagnol | Ser / estar · Gustar | Pretérito indefinido | |
| Chinois | Les tons · Se présenter | Classificateurs | |
| Français | | Homophones grammaticaux | Accord du participe passé |

Les explications sont rédigées en français et **traduites dans votre langue maternelle** sur
l'appareil. Les exemples et les phrases d'exercice, eux, restent dans la langue étudiée. Le cours
correspondant à votre langue maternelle est masqué.

**Ajouter une leçon** : éditer le fichier de la langue dans `app/src/main/assets/learn/`.
Chaque section a un `title`, un `body` (explication) et des `examples` (`text`, `pronunciation`
facultatif, `translation`). Les `exercises` sont de type `choice` (`options`, `answer` = index de la
bonne réponse, `translateOptions` si les choix sont des traductions) ou `input` (`answers` acceptées).
Le test `LearnContentTest` vérifie automatiquement chaque fichier.

## Correction grammaticale (LanguageTool)

La correction utilise l'API publique gratuite de [LanguageTool](https://languagetool.org)
(sans clé, ~30 langues, détection automatique de la langue). Points à savoir :
- le texte saisi est **envoyé aux serveurs de LanguageTool** pour la vérification ; un interrupteur
  dans l'app permet de désactiver la correction (la traduction reste 100 % sur l'appareil) ;
- l'API gratuite est limitée à ~20 vérifications par minute : la vérification attend 0,9 s
  après la dernière frappe ;
- le niveau « picky » est activé pour détecter davantage de fautes, mais l'API gratuite ne repère
  pas toutes les erreurs de déclinaison en allemand (rection des prépositions et des verbes).

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
- `app/src/main/java/com/livetranslator/app/grammar/` – client LanguageTool, parsing, application des corrections
- `app/src/main/java/com/livetranslator/app/ui/GrammarSection.kt` – version corrigée et cartes « règle »
- `app/src/main/java/com/livetranslator/app/learn/` + `ui/LearnScreen.kt` – cours de langues, exercices, prononciation
- `app/src/main/assets/learn/*.json` – contenu des leçons et des exercices

## Compiler

Prérequis : Android Studio (ou JDK 17 + Android SDK 35).

```bash
./gradlew assembleDebug
# APK : app/build/outputs/apk/debug/app-debug.apk
```

Ou ouvrez le dossier dans Android Studio et lancez sur un appareil/émulateur (Android 8.0+).

## Intégration continue

Le workflow GitHub Actions **Build APK** :
- lance les tests unitaires (`./gradlew testDebugUnitTest`) ;
- compile l'APK sur chaque pull request (vérification avant merge) ;
- après chaque merge sur `main`, publie l'APK dans une nouvelle Release GitHub marquée « latest »,
  ce qui met à jour le lien de téléchargement ci-dessus.

Tous les APK sont signés avec la même clé (`app/debug.keystore`), ce qui permet d'installer une
nouvelle version par-dessus l'ancienne sans désinstaller.
