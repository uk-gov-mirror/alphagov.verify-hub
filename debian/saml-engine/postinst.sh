#!/bin/sh

set -e

if [ "$1" != "configure" ]; then
  exit 0
fi

service saml-engine restart
