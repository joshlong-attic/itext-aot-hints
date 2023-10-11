#!/usr/bin/env bash

./mvnw clean -Pnative -DskipTests native:compile

rm -rf $HOME/Desktop/output.pdf

./target/text
