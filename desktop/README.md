# WMS Notes Desktop

The desktop application for WMS Notes, a hierarchical (tree-based) note-taking application.
For general information about WMS Notes, read the main [README](../README.md).


## Running

```bash
java \
  -Dserver.hostname=<server hostname>
  -jar desktop\build\libs\desktop-<version>-SNAPSHOT.jar
```

or 

```sh
java \
  -Dserver.hostname=<server hostname>
  --module-path ${JAVAFX_SDK} \
  --add-modules javafx.controls,javafx.web \
  --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED \
  --add-opens javafx.graphics/javafx.css=ALL-UNNAMED \
  -DrootDirectory=D:\Progs\Java\wmsnotes-desktop\desktop_data \
  -DlogDirectory=D:\Progs\Java\wmsnotes-desktop\desktop_data\logs
  -jar desktop\build\libs\desktop-<version>-SNAPSHOT.jar
```

## Building

```bash
./gradlew :desktop:assemble
```
