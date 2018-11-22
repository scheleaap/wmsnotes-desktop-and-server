# WMS Notes Server

The server component for WMS Notes, a hierarchical or tree-based note-taking application.

## Running

```bash
docker run \
  --detach=true \
  --name wmsnotes-server \
  --restart=unless-stopped \
  --publish 6565:6565 \
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
