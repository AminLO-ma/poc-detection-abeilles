from ultralytics import YOLO
import torch

def main():
    # 1. Vérification de l'accélération
    # Sur Windows avec GPU NVIDIA, on utilise 'cuda'.
    # Si pas de GPU NVIDIA, on retombe sur 'cpu'.
    if torch.cuda.is_available():
        device = 'cuda'
        print(f"🚀 GPU NVIDIA détecté : {torch.cuda.get_device_name(0)}")
    else:
        device = 'cpu'
        print("⚠️ Pas de GPU NVIDIA détecté, utilisation du CPU (plus lent).")

    print(f"🚀 Entraînement lancé sur : {device.upper()}")

    # 2. Chargement du modèle
    # On part de 'yolov8n.pt'
    # Il sera téléchargé automatiquement au 1er lancement.
    model = YOLO('yolov8n.pt')

    # 3. Lancement de l'entraînement
    results = model.train(
        data='bee_data.yaml',   # Fichier de config
        epochs=50,              # Nombre de passes
        imgsz=640,              # Taille de l'image en entrée
        batch=16,               # Taille du lot
        device=device,          # Utilisation du GPU NVIDIA ou CPU
        project='runs/train',   # Dossier de sortie des logs
        name='bee_experiment_win',  # Nom de l'expérience (différencié pour Windows)
        exist_ok=True,          # Écrase si le dossier existe déjà
        patience=10,            # Early stopping si pas d'amélioration après 10 époques
        verbose=True
    )

    # 4. Validation finale et Test sur une image
    print("\n📊 Validation des performances...")
    metrics = model.val()
    print(f"Map50-95: {metrics.box.map}")

    # Exportation préventive
    # model.export(format='onnx') # ONNX est souvent plus utile sur Windows que TFLite de base

if __name__ == '__main__':
    main()
