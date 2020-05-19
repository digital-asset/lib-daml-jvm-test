#!/usr/bin/env bash
#
# Copyright (c) 2019, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#

if [ $# -eq 1 ]
then
    SDK_VERSION=$1
else
    echo "Usage: ${BASH_SOURCE[0]} [SDK_VERSION]"
    exit 1
fi

set -e

echo "SDK version is $SDK_VERSION"

sed -e "s/^\(sdk-version\): .*$/\1: $SDK_VERSION/" -i src/test/resources/ping-pong/daml.yaml
sed -e 's/\(daml_sdk_version\): ".*"$/\1: "'"$SDK_VERSION"'"/' -i .circleci/config.yml
