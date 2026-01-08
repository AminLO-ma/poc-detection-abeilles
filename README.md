# 🐝 POC : Détection d'Abeilles

Ce projet vise à créer un système de détection d'abeilles en temps réel sur smartphone Android.

## 🌿 Organisation du Dépôt (Branches)
Pour faciliter l'évaluation, le projet est structuré en plusieurs branches dédiées :
1. **main** : Contient la documentation globale et le cahier des charges et la vidéo de demonstration.
2. **ml-dev** (Pôle Intelligence Artificielle) : Regroupe toute la partie conception de l'IA.
     **Dossier ML/** : Contient le "cerveau" du modèle, le dataset d'entraînement et le Notebook de test pour valider les performances.
3. **android-dev** (Pôle Développement Mobile) : Contient le code source de l'application mobile de test développée en Kotlin.
   
## 🚀 Pipeline du projet
1. **Entraînement** : YOLOv8n sur le dataset Kaggle Bee Detection.
2. **Optimisation** : Conversion en TFLite pour l'accélération matérielle.
3. **Application** : Intégration dans une app Android native (Kotlin + Jetpack Compose + LiteRT).

## 🛠️ Installation
```bash
pip install ultralytics
python train_yolo.py
```


