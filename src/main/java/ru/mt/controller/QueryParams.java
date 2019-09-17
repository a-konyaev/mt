package ru.mt.controller;

import ru.mt.utils.ApiUtils;

import java.util.List;
import java.util.Map;

class QueryParams {
    private final Map<String, List<String>> paramMap;

    private QueryParams(Map<String, List<String>> paramMap) {
        this.paramMap = paramMap;
    }

    static QueryParams fromRawQuery(String rawQuery) {
        return new QueryParams(ApiUtils.splitQuery(rawQuery));
    }

    String getParamString(String paramName) throws QueryParamsException {
        var valueList = paramMap.get(paramName);

        if (valueList == null || valueList.size() == 0) {
            throw new QueryParamsException("Query parameter not found: " + paramName);
        }

        if (valueList.size() > 1) {
            throw new QueryParamsException(String.format(
                    "Query parameter '%s' should have only one value, but several values were found: %s",
                    paramName, valueList));
        }

        return valueList.get(0);
    }

    double getParamDouble(String paramName) throws QueryParamsException {
        var value = getParamString(paramName);

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw getConvertException(paramName, value, "Double");
        }
    }

    private QueryParamsException getConvertException(
            String paramName, String value, String targetTypeName) throws QueryParamsException {

        return new QueryParamsException(String.format(
                "Query parameter '%s' has an invalid value '%s' that cannot be converted to type '%s'",
                paramName, value, targetTypeName));
    }
}