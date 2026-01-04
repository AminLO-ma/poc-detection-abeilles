# 📱 Bee Detection - Android App (Edge AI)

Ce module contient l'application **Android native** du projet Bee Detection.

C'est ici que se concrétise l'approche **Edge AI** : l'application embarque le modèle YOLOv8 entraîné et quantifié pour effectuer des détections d'abeilles en temps réel directement sur le téléphone, **sans aucune connexion internet**.

---

## 🏗 Architecture & Workflow

Le projet global suit un pipeline en 3 étapes. Nous sommes ici à l'**Étape 3**.

1.  ✅ **Entraînement (ML)** : Fine-tuning de YOLOv8 sur le dataset.
2.  ✅ **Conversion** : Export en `.tflite` avec quantification INT8 (Full Integer).
3.  🚀 **Intégration Android (Ce module)** :
    * Acquisition du flux vidéo via **CameraX**.
    * Pré-traitement des images (Resize, Normalization).
    * Inférence locale via **TensorFlow Lite**.
    * Affichage des Bounding Boxes via **Jetpack Compose**.

---

## 🛠 Technologies (Partie Mobile)

* **Langage** : Kotlin
* **UI Framework** : [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **Caméra** : [CameraX](https://developer.android.com/training/camerax) (ImageAnalysis use case)
* **ML Runtime** : TensorFlow Lite Support Library
* **Architecture** : MVVM (Model-View-ViewModel) + Clean Architecture simplifiée.

---

## 📂 Structure du Projet

L'organisation du code source (`app/src/main/`) est la suivante :

```text
android-app/
├── assets/
│   └── yolov8n_bees_v1_full_integer_quant.tflite  # 🧠 Le modèle quantifié
├── java/com/example/beedetectionapp/
│   ├── data/
│   │   ├── analyzer/
│   │   │   └── BeeImageAnalyzer.kt    # 📷 Fait le pont entre CameraX et TFLite
│   │   └── model/
│   │       └── TFLiteObjectDetector.kt # 🤖 Gère l'interpréteur et le post-processing
│   ├── domain/                        # Modèles de données (ex: DetectionResult)
│   ├── ui/
│   │   ├── components/
│   │   │   └── BeeCard.kt             # Composants UI réutilisables
│   │   ├── screens/
│   │   │   └── HomeScreen.kt          # 📱 Écran principal (Overlay + Caméra)
│   │   └── theme/                     # Thème et Couleurs
│   ├── HomeViewModel.kt               # Gestion de l'état UI
│   └── MainActivity.kt                # Point d'entrée de l'application
└── res/                               # Ressources Android (icônes, textes...)

```

---

## 🚀 Installation & Setup

### 1. Prérequis

* **Android Studio** (Koala ou plus récent recommandé).
* Un appareil Android physique (Recommandé pour tester la caméra et les perfs ML) OU un émulateur.
* **Mode Développeur** activé sur le téléphone.

### 2. Cloner et Ouvrir

Ouvrez le dossier `android` (ou la racine du repo si c'est un monorepo) directement dans Android Studio.

Laissez Gradle synchroniser les dépendances (`Sync Project with Gradle Files`).

### 3. Le Modèle TFLite

Le projet nécessite le fichier modèle dans le dossier `assets`.

* Si vous avez suivi la partie ML : copiez votre `best_int8.tflite` vers `app/src/main/assets/`.
* *Note : Le fichier est déjà inclus dans cette branche sous le nom `yolov8n_bees_v1_full_integer_quant.tflite`.*

### 4. Lancer l'application

1. Connectez votre téléphone en USB.
2. Sélectionnez votre appareil dans la barre d'outils Android Studio.
3. Cliquez sur le bouton **Run (▶)**.
4. Acceptez la permission Caméra au lancement.

---

## ⚙️ Fonctionnement Technique

### Le Pipeline d'Inférence

L'application ne traite pas chaque frame pour économiser la batterie.

1. **Input** : CameraX fournit une image au format YUV_420_888 (ou RGBA selon config).
2. **Preprocessing** :
* L'image est redimensionnée en **640x640**.
* Conversion en `UINT8` [0-255] (Le modèle gère la quantification interne).


3. **Inférence** : TFLite exécute le graphe du modèle.
4. **Post-processing** :
* Décodage des sorties (coordonnées [x, y, w, h] et score).
* Application du **NMS (Non-Maximum Suppression)** pour éviter les boîtes en double.


5. **UI Update** : Les résultats sont envoyés au `State` de Compose qui dessine les rectangles par-dessus la preview caméra.

---

## 🔮 Prochaines Étapes

1. Optimiser l'inférence en activant le délégué **GPU** ou **NNAPI** (Neural Networks API).
2. Gérer le mode paysage (Landscape).
3. Ajouter un seuil de confiance réglable dans l'UI.
4. Générer un APK signé pour la distribution.
