package com.jetcahe.support;

import java.util.List;

/**
 * 入参解释结果
 * @author xieyang
 */
public class InParamParseResult extends BaseParamParseResult {


   private String[] parameterNames;

    /**
     * 原入参列表
     */
   private Object originList;

    public void setParameterNames(String[] parameterNames) {
        this.parameterNames = parameterNames;
    }

    /**
     * 替换成新的列表值，通过引用构建新到参数
     * @param newList
     * @return
     */
    public Object[] rewriteArgsList(Object[] srArgs,List newList){
        originList = parser.parseExpression(listScript).getValue(context);
        parser.parseExpression(listScript).setValue(context,newList);
        for(int i=0;i<parameterNames.length;i++){
            String name = parameterNames[i];
            Object arg = parser.parseExpression("#"+name).getValue(context);
            srArgs[i]=arg;
        }
        return srArgs;
    }

    /**
     * 恢复被修改的入参
     * @param srArgs
     * @return
     */
    public Object[] restoreArgsList(Object[] srArgs){
        parser.parseExpression(listScript).setValue(context,originList);
        for(int i=0;i<parameterNames.length;i++){
            String name = parameterNames[i];
            Object arg = parser.parseExpression("#"+name).getValue(context);
            srArgs[i]=arg;
        }
        return srArgs;
    }


}
