#!/usr/bin/env bash
#
# Copyright (c) 2019, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#

SDK_VERSION=${1:?SDK version not specified.}

set -e

echo "SDK version is $SDK_VERSION"

sed -e "s/^\(sdk-version\): .*$/\1: $SDK_VERSION/" -i src/test/resources/ping-pong/daml.yaml
sed -e 's/\(daml_sdk_version\): ".*"$/\1: "'"$SDK_VERSION"'"/' -i .circleci/config.yml
