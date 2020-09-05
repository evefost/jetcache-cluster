package com.jetcahe.support.extend;

import com.alicp.jetcache.anno.support.CachedAnnoConfig;

/**
 * @author xieyang
 */
public class BatchCachedAnnoConfig extends CachedAnnoConfig {

    /**
     * 返回值表达式
     */
    private String returnKey;

    /**
     * 返回值列表名称
     */
    private String returnListName;

    /**
     * 是否为hash 操作
     */
    private boolean isHash;

    /**
     * hash操作的field
     */
    private String hashField;

    public String getReturnListName() {
        return returnListName;
    }

    public void setReturnListName(String returnListName) {
        this.returnListName = returnListName;
    }

    public String getReturnKey() {
        return returnKey;
    }

    public void setReturnKey(String returnKey) {
        this.returnKey = returnKey;
    }

    public boolean isHash() {
        return isHash;
    }

    public void setHash(boolean hash) {
        isHash = hash;
    }

    public String getHashField() {
        return hashField;
    }

    public void setHashField(String hashField) {
        this.hashField = hashField;
    }
}
