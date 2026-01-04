from ultralytics import YOLO
import shutil
import os
os.environ['ULTRALYTICS_UPDATE'] = 'false'
from pathlib import Path

# --- CONFIGURATION ---
MODEL_PATH = Path('models/yolov8n_bees_v1.pt') 

# Fichier data.yaml (Nécessaire pour la calibration INT8)
DATA_YAML = 'bee_data.yaml'

# Dossier de destination finale
DEST_DIR = Path('./models/tflite')
# ---------------------

def export_model():
    print(f"🚀 Chargement du modèle : {MODEL_PATH}")
    model = YOLO(MODEL_PATH)

    # Création du dossier de sortie
    DEST_DIR.mkdir(parents=True, exist_ok=True)

    # --- OPTION 1 : Export FP16 (Float16) ---
    # Réduit la taille par 2, très peu de perte de précision.
    # Idéal pour le GPU mobile.
    print("\n📦 Exportation format FP16...")
    model.export(
        format='tflite',
        imgsz=640,
        half=True,  # Active FP16
        int8=False
    )
    
    # --- OPTION 2 : Export INT8 (Full Integer Quantization) ---
    # Réduit la taille par 4, utilise le CPU /  NPU (Neural Processing Unit).
    # Nécessite le dataset pour "calibrer" les valeurs (savoir quelles infos garder).
    print("\n📦 Exportation format INT8 (Optimisé NPU)...")
    model.export(
        format='tflite',
        imgsz=640,
        int8=True,      # Active la quantification
        data=DATA_YAML, # Indispensable pour la calibration
        nbs=100         # Utilise 100 images pour calibrer
    )

    # --- NETTOYAGE ET RANGEMENT ---
    # Ultralytics sauvegarde les fichiers à côté du .pt original.
    
    source_dir = MODEL_PATH.parent
    
    # Déplacement du FP16
    fp16_name = MODEL_PATH.stem + '_float16.tflite'
    if (source_dir / fp16_name).exists():
        shutil.move(str(source_dir / fp16_name), str(DEST_DIR / fp16_name))
        print(f"✅ Modèle FP16 déplacé vers : {DEST_DIR / fp16_name}")

    # Déplacement du INT8
    int8_name = MODEL_PATH.stem + '_int8.tflite'
    if (source_dir / int8_name).exists():
        shutil.move(str(source_dir / int8_name), str(DEST_DIR / int8_name))
        print(f"✅ Modèle INT8 déplacé vers : {DEST_DIR / int8_name}")

    # Le fichier metadata généré (parfois créé)
    # Note : YOLOv8 intègre les métadonnées DANS le tflite, donc pas de fichier .json externe critique.

if __name__ == '__main__':
    # Vérification que le modèle existe bien
    if not MODEL_PATH.exists():
        print(f"❌ ERREUR : Modèle introuvable à {MODEL_PATH}")
    else:
        export_model()