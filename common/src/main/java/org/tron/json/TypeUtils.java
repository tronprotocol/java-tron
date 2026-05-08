package org.tron.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Type coercion utilities ported from {@code com.alibaba.fastjson.util.TypeUtils}
 * to maintain exact behavioral parity with Fastjson 1.x.
 *
 * <p>Key Fastjson behaviors preserved:
 * <ul>
 *   <li>Comma stripping in numeric strings ({@code "1,000"} → {@code 1000})</li>
 *   <li>Trailing-zero removal ({@code "1.0"} → {@code 1} for int/long)</li>
 *   <li>Boolean coercion from {@code "Y"/"T"/"F"/"N"} strings</li>
 *   <li>Boolean from Number: {@code intValue() == 1} (only 1 is true)</li>
 *   <li>{@code NaN}/{@code Infinity} → {@code null} for BigDecimal</li>
 *   <li>{@code null}/{@code "null"}/{@code "NULL"}/empty → {@code null}</li>
 * </ul>
 */
final class TypeUtils {

  private static final Pattern NUMBER_WITH_TRAILING_ZEROS_PATTERN =
      Pattern.compile("\\.0*$");

  private TypeUtils() {
  }

  static Boolean castToBoolean(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }

    if (value instanceof BigDecimal) {
      return intValue((BigDecimal) value) == 1;
    }

    if (value instanceof Number) {
      return ((Number) value).intValue() == 1;
    }

    if (value instanceof String) {
      String strVal = (String) value;
      if (strVal.isEmpty()
          || "null".equals(strVal)
          || "NULL".equals(strVal)) {
        return null;
      }
      if ("true".equalsIgnoreCase(strVal)
          || "1".equals(strVal)) {
        return Boolean.TRUE;
      }
      if ("false".equalsIgnoreCase(strVal)
          || "0".equals(strVal)) {
        return Boolean.FALSE;
      }
      if ("Y".equalsIgnoreCase(strVal)
          || "T".equals(strVal)) {
        return Boolean.TRUE;
      }
      if ("F".equalsIgnoreCase(strVal)
          || "N".equals(strVal)) {
        return Boolean.FALSE;
      }
    }
    throw new JSONException("can not cast to boolean, value : " + value);
  }

  static Integer castToInt(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof Integer) {
      return (Integer) value;
    }

    if (value instanceof BigDecimal) {
      return intValue((BigDecimal) value);
    }

    if (value instanceof Number) {
      return ((Number) value).intValue();
    }

    if (value instanceof String) {
      String strVal = (String) value;
      if (strVal.isEmpty()
          || "null".equals(strVal)
          || "NULL".equals(strVal)) {
        return null;
      }
      if (strVal.indexOf(',') != -1) {
        strVal = strVal.replaceAll(",", "");
      }
      strVal = NUMBER_WITH_TRAILING_ZEROS_PATTERN.matcher(strVal).replaceAll("");
      return Integer.parseInt(strVal);
    }

    if (value instanceof Boolean) {
      return (Boolean) value ? 1 : 0;
    }

    throw new JSONException("can not cast to int, value : " + value);
  }

  static Long castToLong(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof BigDecimal) {
      return longValue((BigDecimal) value);
    }

    if (value instanceof Number) {
      return ((Number) value).longValue();
    }

    if (value instanceof String) {
      String strVal = (String) value;
      if (strVal.isEmpty()
          || "null".equals(strVal)
          || "NULL".equals(strVal)) {
        return null;
      }
      if (strVal.indexOf(',') != -1) {
        strVal = strVal.replaceAll(",", "");
      }
      try {
        return Long.parseLong(strVal);
      } catch (NumberFormatException ex) {
        // Fastjson falls through to BigDecimal attempt
      }

      strVal = NUMBER_WITH_TRAILING_ZEROS_PATTERN.matcher(strVal).replaceAll("");
      return Long.parseLong(strVal);
    }

    if (value instanceof Boolean) {
      return (Boolean) value ? 1L : 0L;
    }

    throw new JSONException("can not cast to long, value : " + value);
  }

  static BigDecimal castToBigDecimal(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof Float) {
      if (Float.isNaN((Float) value) || Float.isInfinite((Float) value)) {
        return null;
      }
    } else if (value instanceof Double) {
      if (Double.isNaN((Double) value) || Double.isInfinite((Double) value)) {
        return null;
      }
    } else if (value instanceof BigDecimal) {
      return (BigDecimal) value;
    } else if (value instanceof BigInteger) {
      return new BigDecimal((BigInteger) value);
    }

    String strVal = value.toString();

    if (strVal.isEmpty() || "null".equalsIgnoreCase(strVal)) {
      return null;
    }

    if (strVal.length() > 65535) {
      throw new JSONException("decimal overflow");
    }

    if (strVal.indexOf(',') != -1) {
      strVal = strVal.replaceAll(",", "");
    }

    return new BigDecimal(strVal);
  }

  // -- BigDecimal helper methods (ported from Fastjson) --

  static int intValue(BigDecimal decimal) {
    if (decimal == null) {
      return 0;
    }
    int scale = decimal.scale();
    if (scale >= -100 && scale <= 100) {
      return decimal.intValue();
    }
    return decimal.intValueExact();
  }

  static long longValue(BigDecimal decimal) {
    if (decimal == null) {
      return 0;
    }
    int scale = decimal.scale();
    if (scale >= -100 && scale <= 100) {
      return decimal.longValue();
    }
    return decimal.longValueExact();
  }
}
