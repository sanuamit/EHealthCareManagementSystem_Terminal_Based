#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out src/main/java/com/ehealth/Main.java
java -cp out com.ehealth.Main
