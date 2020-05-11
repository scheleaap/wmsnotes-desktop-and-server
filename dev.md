# Development

## Downloading Data

* From Android:
```sh
C:\Users\<USER>\AppData\Local\Android\sdk\platform-tools\adb.exe pull "/storage/emulated/0/wmsnotes" D:\Progs\Java\wmsnotes-desktop\tmp\android
```
* From server:
```sh
rsync -rtPv user@host:/var/wmsnotes/ /mnt/d/Progs/Java/wmsnotes-desktop/tmp/server/
```
* From desktop:
```sh
xcopy "C:\Users\<USER>\AppData\Roaming\WMS Notes\Desktop" "D:\Progs\Java\wmsnotes-desktop\tmp\desktop\" /E/H
```

## Analyzing Differences

```sh
find android/events/ -type f | sort | cut -d'/' -f2-4 > android.list
find server/events/ -type f | sort | cut -d'/' -f2-4 > server.list
find desktop/events/ -type f | sort | cut -d'/' -f2-4 > desktop.list
```
