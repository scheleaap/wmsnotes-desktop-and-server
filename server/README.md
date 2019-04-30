# WMS Notes Server

The server component for WMS Notes, a hierarchical (tree-based) note-taking application.
For general information about WMS Notes, read the main [README](../README.md).


## Running

```bash
docker pull scheleaap/wmsnotes-server && \
docker run \
  --detach=true \
  --name wmsnotes-server \
  --restart=unless-stopped \
  --publish 6565:6565 \
  --volume /var/wmsnotes:/home/.wmsnotes/server:rw \
  scheleaap/wmsnotes-server
```

In order to allow clients to access the server, port **6565** must be opened on the firewall.

**Ubuntu with ufw**:
```bash
ufw allow 6565/tcp
```


## Building

```bash
./gradlew :server:jibDockerBuild
```
