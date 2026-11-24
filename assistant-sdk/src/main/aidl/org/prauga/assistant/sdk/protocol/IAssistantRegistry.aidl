// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk.protocol;

import org.prauga.assistant.sdk.protocol.ContextItemParcel;
import org.prauga.assistant.sdk.protocol.IResultCallback;

interface IAssistantRegistry {
    int protocolVersion() = 0;

    oneway void donate(String packageName, in ContextItemParcel[] items, IResultCallback callback) = 1;

    oneway void remove(String packageName, in String[] itemIds, IResultCallback callback) = 2;

    oneway void removeAll(String packageName, IResultCallback callback) = 3;

    oneway void notifyChanged(String packageName) = 4;
}
