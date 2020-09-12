package com.jetcahe.support;

import java.util.List;

/**
 * @author xieyang
 */
public class CacheCompositeResult {

    private List<Object> caches;

    private List<String> noCacheKeys;

    private List<Object> noCacheParams;

    private InParamParseResult inParamParseResult;

    public List<String> getNoCacheKeys() {
        return noCacheKeys;
    }

    public void setNoCacheKeys(List<String> noCacheKeys) {
        this.noCacheKeys = noCacheKeys;
    }

    public List<Object> getCaches() {
        return caches;
    }

    public void setCaches(List<Object> caches) {
        this.caches = caches;
    }

    public List<Object> getNoCacheParams() {
        return noCacheParams;
    }

    public void setNoCacheParams(List<Object> noCacheParams) {
        this.noCacheParams = noCacheParams;
    }

    public InParamParseResult getInParamParseResult() {
        return inParamParseResult;
    }

    public void setInParamParseResult(InParamParseResult inParamParseResult) {
        this.inParamParseResult = inParamParseResult;
    }
}
