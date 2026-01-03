# 🐝 POC : Détection d'Abeilles sur Mobile

Ce projet vise à créer un système de détection d'abeilles en temps réel sur smartphone Android.

## 🚀 Pipeline du projet
1. **Entraînement** : YOLOv8n sur le dataset Kaggle Bee Detection.
2. **Optimisation** : Conversion en TFLite pour l'accélération matérielle.
3. **Application** : Intégration dans une app Android native (Kotlin + Jetpack Compose + LiteRT).

## 🛠️ Installation
```bash
pip install ultralytics
python train_yolo.py
```
