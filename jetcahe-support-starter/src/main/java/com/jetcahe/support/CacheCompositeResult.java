package com.jetcahe.support;

import java.util.List;
import java.util.Set;

/**
 * @author xieyang
 */
public class CacheCompositeResult<V> {

    private List<V> caches;

    private Set<String> noCacheKeys;

    private List<Object> noCacheParams;

    private InParamParseResult inParamParseResult;

    public Set<String> getNoCacheKeys() {
        return noCacheKeys;
    }

    public void setNoCacheKeys(Set<String> noCacheKeys) {
        this.noCacheKeys = noCacheKeys;
    }

    public List<V> getCaches() {
        return caches;
    }

    public void setCaches(List<V> caches) {
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
