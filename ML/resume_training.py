from ultralytics import YOLO

def resume():

    model = YOLO('runs/train/bee_experiment/weights/last.pt')

    model.train(resume=True)

if __name__ == '__main__':
    resume()