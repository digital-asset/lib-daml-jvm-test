#!/usr/bin/env bash
#
# Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#

if [ $# -ne 4 ]
then
    echo "ERROR: No parameters given! Usage: ${0} maven_login maven_password signing_key gpg_passphrase"
    exit 1
fi

MAVEN_LOGIN="${1}"
MAVEN_PASSWORD="${2}"
GPG_SIGNING_KEY="${3}"
GPG_PASSPHRASE="${4}"

set -e

# Verify we are picking id of secret key correctly 
gpg --list-secret-keys --keyid-format=long

# Import a key
echo "${GPG_SIGNING_KEY}" | base64 -d &> my.key
gpg --allow-secret-key-import --import my.key &> gpg.out
# We need to get the id and cut the : from it
GPG_SIGNING_KEY_ID=$(grep 'gpg: key ' gpg.out | sort | head -1 | cut -f3 -d' ' | cut -f1 -d':')
mkdir -p /home/circleci/.sbt/gpg/
gpg -a --export-secret-keys > /home/circleci/.sbt/gpg/secring.asc

# Verify we are picking id of secret key correctly 
gpg --list-secret-keys --keyid-format=long

# Export environment variables for SBT release process
export MAVEN_LOGIN
export MAVEN_PASSWORD
export GPG_SIGNING_KEY_ID
export GPG_PASSPHRASE

# useful debugging commands:
# sbt sonatypeLog
# sbt sonatypeStagingProfiles
# sbt 'sonatypeDrop comdaml-1518'
# older version: sbt sonatypeList

# Publishing
sbt clean \
    verify \
    packageAll \
    publishSigned \
    sonatypeBundleRelease
