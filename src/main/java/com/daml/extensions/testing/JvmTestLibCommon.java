/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing;

import com.google.common.collect.Range;

public class JvmTestLibCommon {
    public static final String DEFAULT_IMAGE = "digitalasset/canton-open-source";
    public static final int CANTON_PARTICIPANT1_LEDGER_API_PORT = 5011;
    public static final Range<Integer> SANDBOX_PORT_RANGE = Range.closed(6860, 6890);

}
