import com.jetcahe.support.Pair;
import com.jetcahe.support.extend.BatchSpelEvaluator;
import com.jetcahe.support.InParamParseResult;
import com.jetcahe.support.OutParamParseResult;
import jetcache.samples.Tenant;
import jetcache.samples.User;
import org.junit.Before;
import org.junit.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ELTest {

    SpelExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext context = new StandardEvaluationContext();

    @Before
    public void init(){
        Tenant tenant = buildTenant();
        User user = buildUser(10);
        User user2 = buildUser(33);
        User user3 = buildUser(44);
        List<User> users = new ArrayList<>();
        users.add(user);
        users.add(user2);
        users.add(user3);
        context.setVariable("tenant",tenant);
        context.setVariable("user",user);
        context.setVariable("users",users);
    }

    private Tenant buildTenant(){
        User user = buildUser(11);
        User user2 = buildUser(22);
        List<User> users = new ArrayList<>();
        users.add(user);
        users.add(user2);

        Tenant tenant = new Tenant();
        tenant.setStoreId(123);
        tenant.setUsers(users);
        return tenant;
    }

    private User buildUser(int id){
        User user = new User();
        user.setUserId(id);
        user.setUserName("aa"+id);
        return user;
    }

    @Test
    public void testGetUserList(){
        Object value = parser.parseExpression("#users").getValue(context);
        assert  value != null;
    }

    @Test
    public void testSimpleListEl(){
        Object value = parser.parseExpression("#tenant.storeId+':'+#tenant.users[0].userId").getValue(context);
        assert  value != null;
    }

    @Test
    public void testSimpleListEl2(){
        String script = "#tenant.storeId+':'+$#tenant.users[0].userId";
        String listScript = script.substring(script.indexOf("$")+1,script.lastIndexOf("["));
        List list = (List) parser.parseExpression(listScript).getValue(context);
        assert list.size()>0;
    }

    @Test
    public void testSimpleListEl3(){
        String script = "#tenant.storeId+':'+$#tenant.users[0].userId";
        String listScript = script.substring(script.indexOf("$")+1,script.lastIndexOf("["));
        List list = (List) parser.parseExpression(listScript).getValue(context);
        assert list.size()>0;
    }

    @Test
    public void testParseArrayEl(){
        String srcScript = "#tenant.storeId+':'+$#tenant.users[].userId";
        String[] scriptArr = srcScript.split("\\$");
        System.out.println(scriptArr);
        String listScript=scriptArr.length==1?scriptArr[0]:scriptArr[1];
        System.out.println("======:"+listScript);
        srcScript = "$#tenant.users[].userId+':'+#tenant.storeId";
        scriptArr = srcScript.split("\\$");
        listScript=scriptArr.length==1?scriptArr[0]:scriptArr[1];
        System.out.println("======:"+listScript);
        srcScript = "#tenant.users[].userId+':'+#tenant.storeId";
        scriptArr = srcScript.split("\\$");
        listScript=scriptArr.length==1?scriptArr[0]:scriptArr[1];
        System.out.println("======:"+listScript);

    }


    @Test
    public void testParseArrayKeys(){
        String srcScript = "#tenant.storeId+':'+$#tenant.users[].userId";
        String[] scriptArr = srcScript.split("\\$");
        System.out.println(scriptArr);
        String listElementScript=scriptArr.length==1?scriptArr[0]:scriptArr[1];
        String[] listArr = listElementScript.split("\\[\\]");
        String listScript = listElementScript.split("\\[\\]")[0];
        List list = (List) parser.parseExpression(listScript).getValue(context);
        //解释列表单元script,及单元目标内script
        boolean morePart=scriptArr.length>1;
        List<Pair<String/*elementTargetScript*/,/*elementScript*/String>> elementScriptPairs= new ArrayList<>(list.size());
        for(int i=0;i<list.size();i++){
            String elementScript=listScript+"["+i+"]";
            String elementTargetScript=morePart?(scriptArr[0]+elementScript+listArr[1]):elementScript+listArr[1];
            elementScriptPairs.add(new Pair<>(elementTargetScript,elementScript));
        }
        assert  elementScriptPairs.size()>0;
        //获取value
        List<Pair<Object/*elementTargetScript*/,/*elementScript*/Object>> elementTargetValuePairs= new ArrayList<>(list.size());
        for(Pair<String,String> pair:elementScriptPairs){
            Object elementTargetValue = parser.parseExpression(pair.getKey()).getValue(context);
            Object elementValue = parser.parseExpression(pair.getValue()).getValue(context);
            elementTargetValuePairs.add(new Pair<>(elementTargetValue,elementValue));
        }
        assert  elementTargetValuePairs.size()>0;

        //替换原列表值
        List newListParams = new ArrayList();
        newListParams.add(buildUser(777));
        newListParams.add(buildUser(888));
        newListParams.add(buildUser(999));
        parser.parseExpression(listScript).setValue(context,newListParams);
        Object value = parser.parseExpression("#tenant").getValue(context);
        assert value != null;

    }


    @Test
    public void testParseArrayKeys2(){
        String srcScript = "#tenant.storeId+':'+#tenant.users[].userId+':'+#tenant.users[].userName";
        String listScript = findListScript(srcScript);
        assert listScript != null;
        List list = (List) parser.parseExpression(listScript).getValue(context);
        //解释列表单元script,及单元目标内script
        List<Pair<String/*elementTargetScript*/,/*elementScript*/String>> elementScriptPairs= new ArrayList<>(list.size());
        for(int i=0;i<list.size();i++){
            String elementScript=listScript+"["+i+"]";
            String elementTargetScript=srcScript.replace("[]","["+i+"]");
            elementScriptPairs.add(new Pair<>(elementTargetScript,elementScript));
        }
        assert  elementScriptPairs.size()>0;
        //获取value
        List<Pair<Object/*elementTargetScript*/,/*elementScript*/Object>> elementTargetValuePairs= new ArrayList<>(list.size());
        for(Pair<String,String> pair:elementScriptPairs){
            Object elementTargetValue = parser.parseExpression(pair.getKey()).getValue(context);
            Object elementValue = parser.parseExpression(pair.getValue()).getValue(context);
            elementTargetValuePairs.add(new Pair<>(elementTargetValue,elementValue));
        }
        assert  elementTargetValuePairs.size()>0;

        //替换原列表值
        List newListParams = new ArrayList();
        newListParams.add(buildUser(777));
        newListParams.add(buildUser(888));
        newListParams.add(buildUser(999));
        parser.parseExpression(listScript).setValue(context,newListParams);
        Object value = parser.parseExpression("#tenant").getValue(context);
        assert value != null;

    }

    private String findListScript(String srcScript){
        String[] scriptArr = srcScript.split("#");
        String listScript = null;
        for(String script:scriptArr){
            if(script.contains("[]")){
                listScript = "#"+script.split("\\[\\]")[0];
                break;
            }
        }
        return listScript;
    }

    @Test
    public void testFindNoDataKey(){
        List<User> paramList = new ArrayList<>();
        User user = new User();
        user.setUserId(1);
        User user2 = new User();
        user2.setUserId(2);
        User user3 = new User();
        user3.setUserId(3);
        paramList.add(user);
        paramList.add(user2);
        paramList.add(user3);
        String inScript ="'key:'+#paramList[.userId";
        context.setVariable("paramList",paramList);
        InParamParseResult inParamParseResult = BatchSpelEvaluator.parseInParams(inScript, parser, context, null);
        List<Pair<Object, Object>> elementTargetValuePairs = inParamParseResult.getElementTargetValuePairs();
        List<String> allKey = elementTargetValuePairs.stream().map(p -> p.getKey().toString()).collect(Collectors.toList());
        System.out.println(allKey);
        //无数据key=检查缓存数据+db数据

        List<Pair<String, ?>> cachePairs = new ArrayList<>();
        cachePairs.add(new Pair<String,Object>("key:1",user));
        cachePairs.add(new Pair<String,Object>("key:2",null));
        cachePairs.add(new Pair<String,Object>("key:3",null));

        List<Object> dbUsers = new ArrayList<>();
        dbUsers.add(user3);
        String outScript ="'key:'+#returnList[.userId";
        context.setVariable("returnList",dbUsers);
        OutParamParseResult outParamParseResult = BatchSpelEvaluator.parseOutParams(outScript, parser, context);
        List<Pair<Object, Object>> outEmlementPairs = outParamParseResult.getElementTargetValuePairs();
        List<String> dbValueKeys = outEmlementPairs.stream().map(p -> p.getKey().toString()).collect(Collectors.toList());
        List<String> cacheHasValueKeys = cachePairs.stream().filter(p->p.getValue() != null).map(p -> p.getKey()).collect(Collectors.toList());
        allKey.removeAll(dbValueKeys);
        allKey.removeAll(cacheHasValueKeys);
        List<String> noDataKeys = allKey;
        System.out.println(noDataKeys);


    }




}
