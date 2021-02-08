#!/usr/bin/env bash
#
# Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#

if [ $# -ne 5 ]
then
    echo "ERROR: No parameters given! Usage: ${0} maven_login maven_password signing_key gpg_passphrase sdk_version"
    exit 1
fi

MAVEN_LOGIN="${1}"
MAVEN_PASSWORD="${2}"
SIGNING_KEY="${3}"
GPG_PASSPHRASE="${4}"
SDK_VERSION="${5}"
BASE_DIR="$(dirname "$(readlink -f "$0")")"

${BASE_DIR}/install-daml.sh ${SDK_VERSION}

# Import a key
echo ${GPG_SIGNING_KEY} | base64 -d &> my.key
gpg --import my.key &> gpg.out
# We need to get the id and cut the : from it
GPG_SIGNING_KEY_ID=$(cat gpg.out | grep 'gpg: key ' | sort | head -1 | cut -f3 -d' ' | cut -f1 -d':')
mkdir -p /home/circleci/.sbt/gpg/
gpg -a --export-secret-keys > /home/circleci/.sbt/gpg/secring.asc

# Export environment variables for SBT release process
export PATH=$PATH:~/.daml/bin
export MAVEN_LOGIN
export MAVEN_PASSWORD
export GPG_SIGNING_KEY_ID
export GPG_PASSPHRASE

# Publishing
echo drop
sbt sonatypeDrop "comdigitalasset-1507"
echo drop2
sbt sonatypeDrop "comdigitalasset-1508"
echo drop3
sbt sonatypeDrop "comdigitalasset-1509"
echo dr
