# Android Webcam MJPEG

This Android application turns your phone into a webcam by streaming live camera feed over HTTP using the MJPEG (Motion JPEG) format. It allows you to access the camera stream from any device on the same network via a web browser or compatible applications.

## Features

- **Live Camera Streaming**: Captures and streams video from the phone's camera in real-time.
- **MJPEG Format**: Uses MJPEG for efficient streaming over HTTP.
- **Camera Switching**: Switch between front and back cameras.
- **HTTP Server**: Runs a built-in HTTP server on port 8080.
- **Web Interface**: Provides a simple web page to view the stream directly in a browser.
- **Network Access**: Accessible from any device on the local network.

## Requirements

- Android device with camera hardware.
- Android API level 21 or higher.
- Permissions: Camera, Internet, Wi-Fi state access.

## Installation and Setup

1. Clone or download this repository.
2. Open the project in Android Studio.
3. Build and install the app on your Android device using Gradle:
   ```
   ./gradlew build
   ./gradlew installDebug
   ```
4. Grant camera permissions when prompted.

## Usage Tutorial

1. **Launch the App**: Open the app on your Android device. It will request camera permissions if not already granted.

2. **Start Streaming**: The app automatically starts the camera preview and HTTP server. You'll see the current camera (front or back) and the stream URL displayed on the screen, e.g., `http://192.168.0.113:8080/mjpeg`.

3. **Access the Stream**:
   - **Via Web Browser**: Open a web browser on any device connected to the same network and navigate to `http://<phone_ip>:8080/`. This will display a simple page with the live camera feed.
   - **Direct MJPEG URL**: Use `http://<phone_ip>:8080/mjpeg` in applications that support MJPEG streams.

4. **Switch Cameras**: Tap the "Switch Camera" button in the app to toggle between front and back cameras.

5. **Stop Streaming**: Close the app to stop the server and camera access.

## Testing the Stream

A Python test script is provided in the `Tools/` directory to verify the stream using OpenCV:

- **webcamTest.py**: This script captures the MJPEG stream and displays it in a window. Update the `url` variable with the IP address shown in the app, then run:
  ```
  python Tools/webcamTest.py
  ```
  Press 'q' to quit the viewer.

Ensure OpenCV is installed (`pip install opencv-python`) before running the script.

## Technical Details

- **Camera Management**: Uses Android CameraX for camera access and frame capture.
- **Image Processing**: Converts camera frames to JPEG format for streaming.
- **Server**: Built with NanoHTTPD for lightweight HTTP serving.
- **Stream Format**: Multipart MJPEG with approximately 20 FPS.

## Permissions

The app requires the following permissions:
- `CAMERA`: To access the device's camera.
- `INTERNET`: To serve the stream over the network.
- `ACCESS_WIFI_STATE`: To retrieve the device's IP address.

## Notes

- Ensure your phone and the accessing device are on the same Wi-Fi network.
- For security, this app is intended for local network use only.
- The stream may consume battery and data; monitor usage accordingly.