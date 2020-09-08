package com.jetcahe.support.config;

import com.jetcahe.support.annotation.EnableListCache;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2016/11/16.
 *
 */
public class BatchConfigSelector extends AdviceModeImportSelector<EnableListCache> {

    @Override
    public String[] selectImports(AdviceMode adviceMode) {
        switch (adviceMode) {
            case PROXY:
                return getProxyImports();
            case ASPECTJ:
//                return getAspectJImports();
            default:
                return null;
        }
    }

    /**
     * Return the imports to use if the {@link AdviceMode} is set to {@link AdviceMode#PROXY}.
     * <p>Take care of adding the necessary JSR-107 import if it is available.
     */
    private String[] getProxyImports() {
        List<String> result = new ArrayList<String>();
        result.add(AutoProxyRegistrar.class.getName());
        result.add(JetCacheJedisClusterProxyConfiguration.class.getName());
        return result.toArray(new String[result.size()]);
    }


}
