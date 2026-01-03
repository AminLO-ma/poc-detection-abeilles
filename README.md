# 🐝 Bee Detection - Mobile Edge AI POC

Ce projet est un **Proof of Concept (POC)** visant à détecter des abeilles en temps réel sur des appareils mobiles Android. 

L'objectif est de créer une solution **Edge AI** (intelligence artificielle embarquée) capable de fonctionner **sans connexion internet**, en utilisant un modèle de vision par ordinateur optimisé pour les contraintes matérielles (batterie, chauffe, latence).

---

## 🏗 Architecture & Workflow

Le projet suit un pipeline en 3 étapes principales. Actuellement, nous sommes à l'**Étape 1**.

1.  **🧠 Entraînement (ML Pipeline)** :
    *   Préparation du dataset.
    *   Fine-tuning du modèle **YOLOv8 Nano** (le plus léger).
    *   Validation des performances (mAP).
2.  **⚙️ Conversion & Optimisation** *(À venir)* :
    *   Exportation du modèle vers **TensorFlow Lite (.tflite)**.
    *   Quantification (Float16 ou INT8) pour l'accélération NPU/GPU mobile.
3.  **📱 Intégration Android** *(À venir)* :
    *   Développement d'une app native (Kotlin + Jetpack Compose).
    *   Intégration du runtime **LiteRT** (ex-TFLite) et CameraX.

---

## 🛠 Technologies (Partie ML)

*   **Langage** : Python 3.10+
*   **Framework** : [Ultralytics YOLOv8](https://github.com/ultralytics/ultralytics) (PyTorch)
*   **Dataset** : [Bee Detection Dataset](https://www.kaggle.com/datasets/lara311/bee-detection-dataset) (Kaggle)
*   **Matériel supporté** :
    *   🍎 **MacOS (Apple Silicon)** : Accélération via MPS (Metal Performance Shaders).
    *   🪟 **Windows / 🐧 Linux** : Accélération via CUDA (NVIDIA) ou CPU.

---

## 📂 Structure du Projet

L'organisation des fichiers pour la partie Machine Learning (`ML/`) est la suivante :

```text
mon_projet_abeilles/
├── .venv/                 # Environnement virtuel Python
├── ML/
│   ├── datasets/          # Données d'entraînement
│   │   └── bees/
│   │       ├── train/     # Images & Labels d'entraînement
│   │       ├── val/       # Images & Labels de validation
│   │       └── test/      # Images de test
│   ├── runs/              # Logs d'entraînement et modèles sauvegardés
│   │   └── train/
│   │       └── bee_experiment/
│   │           └── weights/
│   │               └── best.pt  <-- LE MODÈLE FINAL
│   ├── bee_data.yaml      # Configuration du dataset pour YOLO
│   ├── requirements.txt   # Dépendances Python
│   ├── train_yolo.py      # Script d'entraînement
│   └── yolov8n.pt         # Modèle de base (téléchargé auto.)
└── README.md
```

---

## 🚀 Installation & Setup

Ce guide couvre Windows, MacOS et Linux.

### 1. Prérequis
*   Python 3.10 ou supérieur installé.
*   (Optionnel) Un compte Kaggle pour télécharger le dataset via API.

### 2. Création de l'environnement virtuel

Ouvrez un terminal à la racine du projet `mon_projet_abeilles`.

**Sur MacOS / Linux :**
```bash
python3 -m venv .venv
source .venv/bin/activate
```

**Sur Windows (PowerShell) :**
```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

### 3. Installation des dépendances

Assurez-vous d'être dans le dossier `ML` pour trouver le fichier requirements.

```bash
cd ML
pip install --upgrade pip
pip install -r requirements.txt
```

> **Note Mac M1/M2/M3 :** Le fichier `requirements.txt` est optimisé pour installer `torch` avec le support ARM64. Vérifiez que votre terminal n'utilise pas Rosetta.

---

## 📊 Préparation des Données

Nous utilisons le dataset "Bee Detection" de Kaggle.

1.  **Téléchargement** :
    *   Via l'interface web : [Lien Kaggle](https://www.kaggle.com/datasets/lara311/bee-detection-dataset).
    *   Ou via l'API (si configurée) : `kaggle datasets download -d lara311/bee-detection-dataset`
2.  **Organisation** :
    Décompressez le dataset pour obtenir la structure suivante dans `ML/datasets/bees/`. Le dataset doit contenir les dossiers `train`, `val` (et optionnellement `test`), chacun contenant `images` et `labels`.

3.  **Configuration** :
    Vérifiez que le fichier `ML/bee_data.yaml` pointe bien vers ces dossiers :
    ```yaml
    path: ./datasets/bees
    train: train/images
    val: val/images
    nc: 1
    names:
        0: bee
    ```

---

## 🧠 Lancer l'Entraînement

Le script `train_yolo.py` lance le fine-tuning sur 50 époques avec une résolution de 640px.

**Commande :**
```bash
# Depuis le dossier ML/
python train_yolo.py
```

Le script détectera automatiquement votre matériel :
*   `MPS` sur Mac (Apple Silicon)
*   `CUDA` sur PC (si GPU Nvidia présent)
*   `CPU` sinon.

### Résultats
Une fois terminé, les résultats se trouvent dans `ML/runs/train/bee_experiment/` :
*   **`weights/best.pt`** : Le modèle ayant obtenu le meilleur score. **C'est ce fichier qui sera utilisé pour l'application mobile.**
*   `results.csv` : Historique des métriques (pertes, précision).

---

## 🔮 Prochaines Étapes

1.  Valider le modèle sur des vidéos de test inédites.
2.  Convertir `best.pt` en format `.tflite`.
3.  Débuter le développement de l'application Android avec CameraX.
