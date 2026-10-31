#!/bin/bash

# exit on error
set -e

SC_DIR=$HOME/bin/share/story-cli

if [ -e "$SC_DIR" ]; then
    rm "$SC_DIR" -rf
fi

mkdir -p "$SC_DIR"
cd "$SC_DIR"

mkdir -p target/out/jvm/scala-3.9.0/story-cli/
mkdir -p src/scripts
# link will be dangling until story-cli is deployed
ln -s src/scripts/story-cli
