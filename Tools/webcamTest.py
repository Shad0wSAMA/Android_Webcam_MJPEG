import cv2

#Change to current URL shown on the phone app.
url = "http://192.168.0.113:8080/mjpeg"

cap = cv2.VideoCapture(url)

if not cap.isOpened():
    print("无法打开视频流")
    exit()

while True:
    ret, frame = cap.read()

    if not ret:
        print("读取失败")
        break

    cv2.imshow("Phone Webcam", frame)

    # 按 q 退出
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()