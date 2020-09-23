package com.jetcahe.support.extend;


import com.alicp.jetcache.CacheException;
import com.alicp.jetcache.anno.method.CacheInvokeConfig;
import com.alicp.jetcache.anno.method.CacheInvokeContext;
import com.alicp.jetcache.anno.support.CacheAnnoConfig;
import com.jetcahe.support.BaseParamParseResult;
import com.jetcahe.support.InParamParseResult;
import com.jetcahe.support.OutParamParseResult;
import com.jetcahe.support.Pair;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量表达式解释工具
 * @author xieyang
 */
public class BatchExpressUtils {

    private static ExpressionParser parser;

    private static ParameterNameDiscoverer parameterNameDiscoverer;

    static {
        try {
            //since spring 4.1
            Class modeClass = Class.forName("org.springframework.expression.spel.SpelCompilerMode");

            try {
                Constructor<SpelParserConfiguration> c = SpelParserConfiguration.class
                        .getConstructor(modeClass, ClassLoader.class);
                Object mode = modeClass.getField("IMMEDIATE").get(null);
                SpelParserConfiguration config = c.newInstance(mode, BatchExpressUtils.class.getClassLoader());
                parser = new SpelExpressionParser(config);
            } catch (Exception e) {
                throw new CacheException(e);
            }
        } catch (ClassNotFoundException e) {
            parser = new SpelExpressionParser();
        }
        parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    }

    public static InParamParseResult evalKey(CacheInvokeContext cic){
        EvaluationContext context = new StandardEvaluationContext(cic);
        CacheInvokeConfig cacheInvokeConfig = cic.getCacheInvokeConfig();
        CacheAnnoConfig cachedAnnoConfig = cacheInvokeConfig.getCachedAnnoConfig();
        if(cachedAnnoConfig== null && cacheInvokeConfig.getInvalidateAnnoConfigs() != null){
            cachedAnnoConfig = cacheInvokeConfig.getInvalidateAnnoConfigs().get(0);
        }
        String key = cachedAnnoConfig.getKey();
        Method defineMethod = cachedAnnoConfig.getDefineMethod();
        String[] parameterNames  = parameterNameDiscoverer.getParameterNames(defineMethod);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], cic.getArgs()[i]);
            }
        }
        context.setVariable("result", cic.getResult());
        InParamParseResult paramParseResult = parseInParams(key, context, parameterNames);
        return paramParseResult;
    }

    /**
     * @param srcScript 注解上的脚本
     * @return
     */
    public static InParamParseResult parseInParams(String srcScript, EvaluationContext context, String[] parameterNames) {
        InParamParseResult result = new InParamParseResult();
        result.setSrcScript(srcScript);
        result.setParser(parser);
        result.setContext(context);
        result.setParameterNames(parameterNames);

        List<Pair<String, String>> elementKeyValueScriptPairs = parseElementsScript(srcScript, context, result);
        List<Pair<Object,Object>> elementKeyValuePairs= parseElementsKeyValue(elementKeyValueScriptPairs,context);
        result.setElementsKeyValue(elementKeyValuePairs);
        return result;
    }

    private static List<Pair<String,String>> parseElementsScript(String srcScript,EvaluationContext context,InParamParseResult result){
        String listName = findListName(srcScript);
        assert listName != null;
        String listScript = "#"+listName;
        List list = (List) parser.parseExpression(listScript).getValue(context);
        //解释列表单元script,及单元目标内script
        List<Pair<String/*elementTargetScript*/,/*elementScript*/String>> elementKeyValueScriptPairs= new ArrayList<>(list.size());
        for(int i=0;i<list.size();i++){
            String elementKeyScript=srcScript.replace("[","["+i+"]");
            String elementValueScript=listScript+"["+i+"]";
            elementKeyValueScriptPairs.add(new Pair<>(elementKeyScript,elementValueScript));
        }
        result.setListName(listName);
        result.setListScript(listScript);
        return elementKeyValueScriptPairs;
    }

    private static   List<Pair<Object,Object>> parseElementsKeyValue(List<Pair<String, String>> elementKeyValueScriptPairs,EvaluationContext context){
        List<Pair<Object/*elementTargetScript*/,/*elementScript*/Object>> elementKeyValuePairs= new ArrayList<>(elementKeyValueScriptPairs.size());
        for(Pair<String,String> pair:elementKeyValueScriptPairs){
            String keyScript = pair.getKey();
            String valueScript = pair.getValue();
            Object elementKey= parser.parseExpression(keyScript).getValue(context);
            Object elementValue = parser.parseExpression(valueScript).getValue(context);
            elementKeyValuePairs.add(new Pair<>(elementKey,elementValue));
        }
        return elementKeyValuePairs;
    }



    public static OutParamParseResult parseOutParams(String srcScript, EvaluationContext context) {
        BaseParamParseResult baseParamParseResult = parseInParams(srcScript,context, null);
        OutParamParseResult parseResult = new OutParamParseResult();
        parseResult.setContext(context);
        parseResult.setParser(parser);
        parseResult.setElementsKeyValue(baseParamParseResult.getElementsKeyValue());
        parseResult.setListScript(baseParamParseResult.getListScript());
        return parseResult;
    }

    /**
     * 查询参与构建缓存 key 列表脚本
     * @param srcScript
     * @return
     */
    public static String findListName(String srcScript){
        String[] scriptArr = srcScript.split("#");
        String listName = null;
        for(String script:scriptArr){
            if(script.contains("[")){
                listName = script.split("\\[")[0];
                break;
            }
        }
        return listName;
    }



}
