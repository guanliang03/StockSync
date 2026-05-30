StockSync
=========

A compact Android inventory app demonstrating Jetpack Compose, Room, ViewModel,
Kotlin Flow, and Navigation. Use this project as a learning reference or a
starter template for mobile inventory tooling.

Features
--------
- Add, update, sell, and delete items persisted in a local Room database.
- Built with Jetpack Compose UI, `ViewModel`, and Kotlin `Flow`.
- Simple navigation and example tests.

Requirements
------------
- Android Studio Flamingo or later
- Java JDK 11 or newer
- Android SDK (recommended API 21+)

Build & Run
-----------
### Option 1: Via Android Studio
1. Open the project in Android Studio.
2. Allow Gradle to sync and download dependencies.
3. Run the app on an emulator or a connected device.

### Option 2: Via Command Line (Gradle & ADB)
1. Ensure your Android SDK path is set in `local.properties`:
   ```properties
   sdk.dir=C:/Users/liewg/AppData/Local/Android/Sdk
   ```
2. Build and install the app on your running emulator/device:
   ```bash
   .\gradlew.bat installDebug
   ```
3. Start the application:
   ```bash
   adb shell am start -n com.seis2.loanlit/.MainActivity
   ```

Notes
-----
- Avoid committing large binary/media files to this repository. Use Git LFS
	for files larger than ~50 MB or keep them out of version control.
- The repository includes a `LICENSE` file—see it for licensing details.

Contributing
------------
- Create a feature branch, make changes, and open a pull request.
- Keep commits focused and avoid adding large files; ask if you need help with
	binary assets or LFS setup.

License
-------
See the `LICENSE` file in this repository.

Contact
-------
Repository: https://github.com/guanliang03/StockSync

