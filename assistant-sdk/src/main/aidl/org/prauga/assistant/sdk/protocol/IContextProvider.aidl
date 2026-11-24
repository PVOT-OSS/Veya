// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk.protocol;

import org.prauga.assistant.sdk.protocol.ContextItemParcel;
import org.prauga.assistant.sdk.protocol.ContextQueryParcel;
import org.prauga.assistant.sdk.protocol.ContextTypeParcel;

interface IContextProvider {
    int protocolVersion() = 0;

    oneway void requestSync() = 1;

    ContextItemParcel[] query(in ContextQueryParcel query) = 2;

    String[] types() = 3;

    int maxSensitivity() = 4;

    ContextTypeParcel[] describe() = 5;
}
