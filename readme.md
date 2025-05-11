# DM Collection

⚠️ Under Construction ⚠️

### Running locally

You need to have something installed that can run containers.

- Create a directory named `db-data`. The database will be saved there and you can make copies to
  have backups.
- Create a directory named `dmcollection-images`. If you want to see images for cards, put them
  there.

If you do not want to live dangerously, replace `latest` with
a specific version in the commands below.

In bash:

```shell
docker run -d \
  -p 8080:8080 \
  -e DMCOLLECTION_IMAGE_STORAGE_PATH=/images \
  -v $(pwd)/db-data:/data \
  -v $(pwd)/dmcollection-images:/images \
  ghcr.io/dm-collection/server:latest
```

In cmd:

```batch
docker run -d ^
  -p 8080:8080 ^
  -e DMCOLLECTION_IMAGE_STORAGE_PATH=/images ^
  -v %cd%\db-data:/data ^
  -v %cd%\dmcollection-images:/images ^
  ghcr.io/dm-collection/server:latest
```

In PowerShell:

```powershell
docker run -d `
  -p 8080:8080 `
  -e DMCOLLECTION_IMAGE_STORAGE_PATH=/images `
  -v ${PWD}/db-data:/data `
  -v ${PWD}/dmcollection-images:/images `
  ghcr.io/dm-collection/server:latest
```

You can also check out the repository or download the [docker-compose.yaml](docker-compose.yaml)
file and then run it with `docker compose up`.
