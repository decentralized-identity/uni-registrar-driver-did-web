![DIF Logo](https://raw.githubusercontent.com/decentralized-identity/universal-registrar/master/docs/logo-dif.png)

# Universal Registrar Driver: web

This is a [Universal Registrar](https://github.com/decentralized-identity/universal-registrar/) driver for **did:web**
identifiers.

## Specifications

* [Decentralized Identifiers](https://w3c.github.io/did-core/)
* [DID Method Specification](https://w3c-ccg.github.io/did-method-web/)

## Build and Run (Docker)

### Build
```
docker build -f ./docker/Dockerfile . -t universalregistrar/driver-did-web
```

### Run
```
docker run -p 9080:9080 -v dids-volume:<uniregistrar_driver_did_web_basePath> universalregistrar/driver-did-web
```
mounting a volume is not needed if the `uniregistrar_driver_did_web_basePath` is set under /opt
```
docker run -p 9080:9080 universalregistrar/driver-did-web
```

## Driver Environment Variables

```
uniregistrar_driver_did_web_basePath
uniregistrar_driver_did_web_baseUrl
uniregistrar_driver_did_web_generatedFolder
```
