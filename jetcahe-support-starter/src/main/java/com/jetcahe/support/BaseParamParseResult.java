package com.jetcahe.support;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;

import java.util.List;

/**
 * 基本参解释结果
 * @author xieyang
 */
public class BaseParamParseResult {


   protected ExpressionParser parser;

   protected EvaluationContext context;

   private String srcScript;

   private String listName;
    /**
     * 列表表达式，用于操作context 里参与批量操作 list 参数
     */
    protected String listScript;

    /**
     *  目标元素脚本(构造key)对应元素脚本
     */
    protected List<Pair<Object/*elementTargetScript*/,/*elementScript*/Object>> elementTargetValuePairs;

    public String getSrcScript() {
        return srcScript;
    }

    public void setSrcScript(String srcScript) {
        this.srcScript = srcScript;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getListScript() {
        return listScript;
    }

    public void setListScript(String listScript) {
        this.listScript = listScript;
    }

    public List<Pair<Object, Object>> getElementTargetValuePairs() {
        return elementTargetValuePairs;
    }

    public void setElementTargetValuePairs(List<Pair<Object, Object>> elementTargetValuePairs) {
        this.elementTargetValuePairs = elementTargetValuePairs;
    }

    public ExpressionParser getParser() {
        return parser;
    }

    public void setParser(ExpressionParser parser) {
        this.parser = parser;
    }

    public EvaluationContext getContext() {
        return context;
    }

    public void setContext(EvaluationContext context) {
        this.context = context;
    }
}
