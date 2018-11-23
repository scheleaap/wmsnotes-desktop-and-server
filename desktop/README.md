# WMS Notes Desktop

The desktop application for WMS Notes, a hierarchical or tree-based note-taking application.
For general information about WMS Notes, read the main [README](../README.md).


## Running

```bash
java \
  -Dserver.hostname=<server hostname>
  -jar desktop\build\libs\desktop-<version>-SNAPSHOT.jar
```


## Building

```bash
./gradlew :desktop:assemble
```
