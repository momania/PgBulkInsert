// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package de.bytefish.pgbulkinsert.pgsql.handlers;

import de.bytefish.pgbulkinsert.util.BigDecimalUtils;

import java.io.DataOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * The Algorithm for turning a BigDecimal into a Postgres Numeric is heavily inspired by the Intermine Implementation:
 *
 * https://github.com/intermine/intermine/blob/master/intermine/objectstore/main/src/org/intermine/sql/writebatch/BatchWriterPostgresCopyImpl.java
 *
 */
public class BigDecimalValueHandler<T extends Number> extends BaseValueHandler<T> {

    private static final int DECIMAL_DIGITS = 4;

    protected static final BigInteger TEN = new BigInteger("10");
    protected static final BigInteger TEN_THOUSAND = new BigInteger("10000");

    @Override
    protected void internalHandle(DataOutputStream buffer, final T value) throws Exception {

        Number tmpValue = value;
        if (!(value instanceof BigDecimal)) {
            // TODO does this need to be specific for every numeric type?
            tmpValue = BigDecimalUtils.toBigDecimal(value.doubleValue());
        }

        BigInteger unscaledValue = ((BigDecimal)tmpValue).unscaledValue();

        int sign = ((BigDecimal)tmpValue).signum();

        if (sign == -1) {
            unscaledValue = unscaledValue.negate();
        }

        // Number of fractional digits:
        int fractionDigits = ((BigDecimal)tmpValue).scale();

        // Number of Fraction Groups:
        int fractionGroups = (fractionDigits + 3) / 4;

        List<Integer> digits = new ArrayList<>();

        // The scale needs to be a multiple of 4:
        int scaleRemainder = fractionDigits % 4;

        // Scale the first value:
        {
            BigInteger[] result = unscaledValue.divideAndRemainder(TEN.pow(scaleRemainder));

            int digit = result[1].intValue() * (int) Math.pow(10, DECIMAL_DIGITS - scaleRemainder);

            digits.add(new Integer(digit));

            unscaledValue = result[0];
        }

        while (!unscaledValue.equals(BigInteger.ZERO)) {
            BigInteger[] result = unscaledValue.divideAndRemainder(TEN_THOUSAND);
            digits.add(new Integer(result[1].intValue()));
            unscaledValue = result[0];
        }

        buffer.writeInt(8 + (2 * digits.size()));
        buffer.writeShort(digits.size());
        buffer.writeShort(digits.size() - fractionGroups - 1);
        buffer.writeShort(sign == 1 ? 0x0000 : 0x4000);
        buffer.writeShort(fractionDigits);

        // Now write each digit:
        for (int pos = digits.size() - 1; pos >= 0; pos--) {
            int valueToWrite = digits.get(pos).intValue();
            buffer.writeShort(valueToWrite);
        }
    }
}
