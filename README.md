# Ayers-Server
Ayers is another uluru platform.

## features
Ayers + MongoDB, is all of a starter backend.

Ayers + MongoDB + Redis, is all of an integrated backend.

## architecture
[Vertx.io](https://vertx.io/) provides non-blocking tool-kits to access Mysql/Mongo/Redis/Memcached, etc, it is really amazing.

Ayers is a structural data processing server based on Vertx.io and [Netty](https://netty.io/).
***simple(code hierarchy and deploy), non-blocking io, high performance, scalability*** are the main objectives of ayers.

## roadmap

### done
- Installation CRUD

### in progress
- Object CRUD
- let all components configurable.

### todo
- user account.
- pointer
- relation
- file
- bulk operation
- acl
- role
- sms
- managing mongodb collections.
- support mongodb index api.
- support other mongodb commands.
- support plugins
  - feed and status
  - full-text search
  - live query
  - data insight
  - etc

### not to do
- cql.
- class binding.

## how to run locally
- check repository to local.
- enter `local-env` directory, make sub directory as following:
```$xslt
├── data
│   ├── mongo
│   ├── mysql
│   ├── redis
│   └── redis_senti
├── docker-compose.yml
├── mysql.cnf
└── sentinel.conf
```
- enter `local-env` directory, run `docker-compose up -d`
- run shell: `./local-run.sh`
- have fun.