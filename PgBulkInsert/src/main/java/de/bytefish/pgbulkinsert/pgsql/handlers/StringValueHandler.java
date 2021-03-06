// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package de.bytefish.pgbulkinsert.pgsql.handlers;

import de.bytefish.pgbulkinsert.util.StringUtils;

import java.io.DataOutputStream;

public class StringValueHandler extends BaseValueHandler<String> {

    @Override
    protected void internalHandle(DataOutputStream buffer, final String value) throws Exception {
        byte[] utf8Bytes = StringUtils.getUtf8Bytes(value);

        buffer.writeInt(utf8Bytes.length);
        buffer.write(utf8Bytes);
    }
}
