# Shinycolors Song for prism ACB Finder
## Readme
Do not use this program for commercial or illegal activities. The developer is not responsible for any problems arising from the use of this program.
## Development environment
* OS: Windows 11
* Java
  -  JDK 23.0.2: [Open JDK 23](https://jdk.java.net/23/)
  -  Java Fx Sdk 23.0.2: [Java Fx 23](https://jdk.java.net/javafx23/)

 ## How to use
 1. Download the latest release file.
 2. Unzip the Zip file. Unzip it where it has enough capacity because it becomes a working directory.
 3. Execute `Run.bat`
 4. Click `Select Game Folder` to specify the directory where the resource exists.

    In the current version, it is located on the path below.

    `C:\Users\{USER_NAME}\AppData\LocalLow\BNE\imasscprism\D`
5. When the folder selection is complete, press the Decrypt button.
6. The resource file is copied to the `Current_Directory/process/today` and then decrypts.
7. When the decryption operation is complete, click the `Extract ACB Files` button.
8. After the access file is moved to the `Current Directory/process/today/acb` folder, read the contents of the file and rename it.
9. When the operation is completed, convert the acb file into a wav file using `foobar2000` and the `vgmstream` plug-in. It is recommended to use `convert -> default` when converting.
10. The converted wav file is stored in the current `Current Directory/process/today/wav` (must be saved manually by the user)
11. When all the wav files have been moved to that location, press Category WAV Files (*Optional) Click to folder using the character name existing in the file name.

## Build Runnable Jar
`javac --module-path "%PATH_TO_FX%;lib" --add-modules javafx.controls,javafx.fxml,org.controlsfx.controls,com.dlsc.formsfx -d out src/main/java/module-info.java src/main/java/com/arsud/acbfinder/ProcessApplication.java`

`jar --create --file ProcessApplication.jar --main-class=com.arsud.acbfinder.ProcessApplication -C out . -C src/main/resources .`

`java --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.fxml -jar ProcessApplication.jar`
