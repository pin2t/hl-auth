# hl-auth

This is the solution for highload.fun [Auth Server](https://highload.fun/timed_competitions/authserver) competition

## How to run

Download test data (users.jsonl, GeoLite2 database, tasks.jsonl) https://highload.fun/files/tc/authserver/data.tgz https://highload.fun/files/tc/authserver/tasks.jsonl.gz
and put it to data/ subdirectory like this
```
➜  hl-auth git:(master) ✗ ls data
COPYRIGHT.txt     GeoLite2-City-CSV LICENSE.txt       tasks.jsonl       users.jsonl
```
Then run auth server in a docker container 
```bash
gradle jar
docker build . -t auth
docker run --rm -p 8080:8080 -v "/Users/pin/hl-auth/data:/storage/data" -m 8g --cpus=2 -t auth
```

highload.fun [Auth Server](https://highload.fun/timed_competitions/authserver) competition has a limitation for 2 gigabytes of memory but Java solution is not fit at all. JVM has just so much overhead (JIT, GC, VM)

## How to test

```bash
java -cp build/libs/auth-1.0.jar Tasks
```

This command runs all tasks from `data/tasks.jsonl` file on `localhost:8080` server 