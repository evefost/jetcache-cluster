package com.jetcahe.support.extend;

import com.alicp.jetcache.CacheException;
import com.alicp.jetcache.anno.method.CacheInvokeConfig;
import com.alicp.jetcache.anno.method.CacheInvokeContext;
import com.alicp.jetcache.anno.support.CacheAnnoConfig;
import com.jetcahe.support.Pair;
import com.jetcahe.support.BaseParamParseResult;
import com.jetcahe.support.InParamParseResult;
import com.jetcahe.support.OutParamParseResult;
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
import java.util.function.Function;

/**
 * 主要实现支持批量表达式解释
 * @author xieyang
 */
public  class BatchSpelEvaluator implements Function<Object, Object> {

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
                SpelParserConfiguration config = c.newInstance(mode, BatchSpelEvaluator.class.getClassLoader());
                parser = new SpelExpressionParser(config);
            } catch (Exception e) {
                throw new CacheException(e);
            }
        } catch (ClassNotFoundException e) {
            parser = new SpelExpressionParser();
        }
        parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    }

    private String[] parameterNames;

    public BatchSpelEvaluator(String script, Method defineMethod) {
        if (defineMethod.getParameterCount() > 0) {
            parameterNames = parameterNameDiscoverer.getParameterNames(defineMethod);
        }
    }

    @Override
    public InParamParseResult apply(Object rootObject) {
        EvaluationContext context = new StandardEvaluationContext(rootObject);
        CacheInvokeContext cic = (CacheInvokeContext) rootObject;
        CacheInvokeConfig cacheInvokeConfig = cic.getCacheInvokeConfig();
        CacheAnnoConfig cachedAnnoConfig = cacheInvokeConfig.getCachedAnnoConfig();
        if(cachedAnnoConfig== null && !cacheInvokeConfig.getInvalidateAnnoConfigs().isEmpty()){
            cachedAnnoConfig = cacheInvokeConfig.getInvalidateAnnoConfigs().get(0);
        }
        String key = cachedAnnoConfig.getKey();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], cic.getArgs()[i]);
            }
        }
        context.setVariable("result", cic.getResult());
        InParamParseResult paramParseResult = parseInParams(key, parser, context, parameterNames);
        return paramParseResult;
    }


    /**
     * @param srcScript 注解上的脚本
     * @return
     */
    public static InParamParseResult parseInParams(String srcScript, ExpressionParser parser, EvaluationContext context, String[] parameterNames) {
        String listName = findListName(srcScript);
        assert listName != null;
        String listScript = "#"+listName;
        List list = (List) parser.parseExpression(listScript).getValue(context);
        //解释列表单元script,及单元目标内script
        List<Pair<String/*elementTargetScript*/,/*elementScript*/String>> elementScriptPairs= new ArrayList<>(list.size());
        for(int i=0;i<list.size();i++){
            String elementScript=listScript+"["+i+"]";
            String elementTargetScript=srcScript.replace("[","["+i+"]");
            elementScriptPairs.add(new Pair<>(elementTargetScript,elementScript));
        }
        //获取value
        List<Pair<Object/*elementTargetScript*/,/*elementScript*/Object>> elementTargetValuePairs= new ArrayList<>(list.size());
        for(Pair<String,String> pair:elementScriptPairs){
            Object elementTargetValue = parser.parseExpression(pair.getKey()).getValue(context);
            Object elementValue = parser.parseExpression(pair.getValue()).getValue(context);
            elementTargetValuePairs.add(new Pair<>(elementTargetValue,elementValue));
        }
        InParamParseResult result = new InParamParseResult();
        result.setElementTargetValuePairs(elementTargetValuePairs);
        result.setSrcScript(srcScript);
        result.setListName(listName);
        result.setListScript(listScript);
        result.setParser(parser);
        result.setContext(context);
        result.setParameterNames(parameterNames);
        return result;
    }


    public static OutParamParseResult parseOutParams(String srcScript, ExpressionParser parser, EvaluationContext context) {
        BaseParamParseResult baseParamParseResult = parseInParams(srcScript, parser, context, null);
        OutParamParseResult parseResult = new OutParamParseResult();
        parseResult.setContext(context);
        parseResult.setParser(parser);
        parseResult.setElementTargetValuePairs(baseParamParseResult.getElementTargetValuePairs());
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