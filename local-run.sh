#!/usr/bin/env bash

export LAUNCHER="io.vertx.core.Launcher"
export VERTICLE="cn.leancloud.platform.ayers.MainVerticle"
export CMD="mvn compile"
export VERTX_CMD="run"

mvn compile dependency:copy-dependencies
java \
  -cp  $(echo target/dependency/*.jar | tr ' ' ':'):"target/classes" \
  $LAUNCHER $VERTX_CMD $VERTICLE \
  --launcher-class=$LAUNCHER \
  $@

## --redeploy="src/main/**/*" --on-redeploy="$CMD" \
