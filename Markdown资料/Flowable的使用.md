# Flowable的使用

### 1.创建一个Maven工程，导入相关依赖：

```xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.0.5.RELEASE</version>
        <relativePath />
    </parent>
    <dependencies>
        <!--flowable-->
        <dependency>
            <groupId>org.flowable</groupId>
            <artifactId>flowable-spring-boot-starter</artifactId>
            <version>6.4.0</version>
        </dependency>
        <!--数据库-->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.45</version>
        </dependency>
        <!--rest支持-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
```

### 2.在application.yml中配置数据源和Flowable：

```yaml
#数据源配置
spring:
  datasource:
    url: jdbc:mysql://192.168.23.129:3306/repair?characterEncoding=UTF-8
    username: root
    password: 1123

#flowable配置
#自动部署验证设置:true-开启（默认）、false-关闭
flowable:
  check-process-definitions: false
```

### 3.配置启动类：

```java
package com.flowable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RepairApp {
    /**
     *日志信息
     */
    private static final Logger log = LoggerFactory.getLogger(RepairApp.class);

    public static void main(String[] args) {
        log.info("项目开始启动");
        SpringApplication.run(RepairApp.class,args);
        log.info("项目启动完成");
    }
}
```

### 4.启动项目，此时你会发现你的数据库中多了很多ACT_开头的表

![1607604677406](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607604677406.png)

#### 4.1对表的分类：

      ##### 清单：

![1607919971843](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607919971843.png)

##### 通用数据表：

![1607920031129](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607920031129.png)

##### 流程定义存储表

![1607920604362](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607920604362.png)

##### 身份数据表：

![1607920658590](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607920658590.png)

##### 运行时流程数据表

![1607920710333](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607920710333.png)

##### 历史流程数据表

![1607920744797](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607920744797.png)





### 5.设计流程图(http://47.94.16.7:9999/index.html#/processes)：保存并导出

![1607656764506](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607656764506.png)

![1607663461306](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607663461306.png)

### 6.将自动生成的XML文件放在resouses/processes目录下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC" xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             typeLanguage="http://www.w3.org/2001/XMLSchema" expressionLanguage="http://www.w3.org/1999/XPath"
             targetNamespace="http://www.flowable.org/processdef">
    <!--
    标签以及属性的含义：
             process：流程 isExecutable：是否可执行
             startEvent：开启事件
             sequenceFlow：下一个流程   sourceRef：本流程上一个流程的Ref targetRef：本流程下一个流程的Ref
             userTask：用户任务
             flowable:candidateGroups：角色或部门
             exclusiveGateway：排他网关
    -->
    <process id="Expense" name="Leave" isExecutable="true">
        <documentation>报销流程</documentation>
        <startEvent id="start" name="开始"></startEvent>
        <userTask id="fillTask" name="请假" flowable:assignee="${taskUser}">
            <extensionElements>
                <modeler:initiator-can-complete xmlns:modeler="http://flowable.org/modeler">
                    <![CDATA[false]]></modeler:initiator-can-complete>
            </extensionElements>
        </userTask>
        <exclusiveGateway id="judgeTask"></exclusiveGateway>
        <userTask id="directorTak" name="院长审批">
            <extensionElements>
                <flowable:taskListener event="create"
                                       class="com.haiyang.flowable.listener.ManagerTaskHandler"></flowable:taskListener>
            </extensionElements>
        </userTask>
        <userTask id="bossTask" name="辅导员审批">
            <extensionElements>
                <flowable:taskListener event="create"
                                       class="com.haiyang.flowable.listener.BossTaskHandler"></flowable:taskListener>
            </extensionElements>
        </userTask>
        <endEvent id="end" name="结束"></endEvent>
        <sequenceFlow id="flow1" sourceRef="start" targetRef="fillTask"></sequenceFlow>
        <sequenceFlow id="flow2" sourceRef="fillTask" targetRef="judgeTask"></sequenceFlow>
        <sequenceFlow id="directorPassFlow" name="通过" sourceRef="directorTak" targetRef="end">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[${outcome=='通过'}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="bossPassFlow" name="通过" sourceRef="bossTask" targetRef="end">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[${outcome=='通过'}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="directorNotPassFlow" name="驳回" sourceRef="directorTak" targetRef="fillTask">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[${outcome=='驳回'}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="judgeLess" name="大于7天" sourceRef="judgeTask" targetRef="directorTak">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[${day>7}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="judgeMore" name="一周之内" sourceRef="judgeTask" targetRef="bossTask">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[${day<=7}]]></conditionExpression>
        </sequenceFlow>
        <sequenceFlow id="bossNotPassFlow" name="驳回" sourceRef="bossTask" targetRef="fillTask">
            <conditionExpression xsi:type="tFormalExpression"><![CDATA[${outcome=='驳回'}]]></conditionExpression>
        </sequenceFlow>
    </process>
    <bpmndi:BPMNDiagram id="BPMNDiagram_Expense">
        <bpmndi:BPMNPlane bpmnElement="Expense" id="BPMNPlane_Expense">
            <bpmndi:BPMNShape bpmnElement="start" id="BPMNShape_start">
                <omgdc:Bounds height="30.0" width="30.0" x="285.0" y="135.0"></omgdc:Bounds>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape bpmnElement="fillTask" id="BPMNShape_fillTask">
                <omgdc:Bounds height="80.0" width="100.0" x="405.0" y="110.0"></omgdc:Bounds>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape bpmnElement="judgeTask" id="BPMNShape_judgeTask">
                <omgdc:Bounds height="40.0" width="40.0" x="585.0" y="130.0"></omgdc:Bounds>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape bpmnElement="directorTak" id="BPMNShape_directorTak">
                <omgdc:Bounds height="80.0" width="100.0" x="720.0" y="110.0"></omgdc:Bounds>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape bpmnElement="bossTask" id="BPMNShape_bossTask">
                <omgdc:Bounds height="80.0" width="100.0" x="555.0" y="255.0"></omgdc:Bounds>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape bpmnElement="end" id="BPMNShape_end">
                <omgdc:Bounds height="28.0" width="28.0" x="756.0" y="281.0"></omgdc:Bounds>
            </bpmndi:BPMNShape>
            <bpmndi:BPMNEdge bpmnElement="flow1" id="BPMNEdge_flow1">
                <omgdi:waypoint x="314.9499992392744" y="150.0"></omgdi:waypoint>
                <omgdi:waypoint x="404.9999999999684" y="150.0"></omgdi:waypoint>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge bpmnElement="flow2" id="BPMNEdge_flow2">
                <omgdi:waypoint x="504.95000000000005" y="150.0"></omgdi:waypoint>
                <omgdi:waypoint x="585.0" y="150.0"></omgdi:waypoint>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge bpmnElement="judgeLess" id="BPMNEdge_judgeLess">
                <omgdi:waypoint x="624.9439582071294" y="150.0"></omgdi:waypoint>
                <omgdi:waypoint x="719.9999999999847" y="150.0"></omgdi:waypoint>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge bpmnElement="directorNotPassFlow" id="BPMNEdge_directorNotPassFlow">
                <omgdi:waypoint x="770.0" y="110.0"></omgdi:waypoint>
                <omgdi:waypoint x="770.0" y="37.0"></omgdi:waypoint>
                <omgdi:waypoint x="455.0" y="37.0"></omgdi:waypoint>
                <omgdi:waypoint x="455.0" y="110.0"></omgdi:waypoint>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge bpmnElement="bossPassFlow" id="BPMNEdge_bossPassFlow">
                <omgdi:waypoint x="654.9499999998314" y="295.0"></omgdi:waypoint>
                <omgdi:waypoint x="752.640625" y="295.0"></omgdi:waypoint>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge bpmnElement="judgeMore" id="BPMNEdge_judgeMore">
                <omgdi:waypoint x="605.0" y="169.94312543073747"></omgdi:waypoint>
                <omgdi:waypoint x="605.0" y="255.0"></omgdi:waypoint>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge bpmnElement="directorPassFlow" id="BPMNEdge_directorPassFlow">
                <omgdi:waypoint x="770.0" y="189.95"></omgdi:waypoint>
                <omgdi:waypoint x="770.0" y="281.0"></omgdi:waypoint>
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge bpmnElement="bossNotPassFlow" id="BPMNEdge_bossNotPassFlow">
                <omgdi:waypoint x="555.0" y="295.0"></omgdi:waypoint>
                <omgdi:waypoint x="455.0" y="295.0"></omgdi:waypoint>
                <omgdi:waypoint x="455.0" y="189.95"></omgdi:waypoint>
            </bpmndi:BPMNEdge>
        </bpmndi:BPMNPlane>
    </bpmndi:BPMNDiagram>
</definitions>
```



### 7.实现controller层和service层

#### 7.1**部署流程**

service层：

```java
/**
     * 部署流程
     * filePath 文件路径 name 流程名字
     */
    public Map<String, Object> deploymentFlow(String filePath, String name) {
        try {
            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
                    .addClasspathResource(filePath).name(name);
            Deployment deployment = deploymentBuilder.deploy();
            logger.info("成功：部署工作流程：" + filePath);
            logger.error("deployment.getKey():" + deployment.getKey());
            logger.error("deployment.getName():" + deployment.getName());
            //acr_re_deployment表的id
            String id = deployment.getId();

            ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
            //搜索条件deploymentId
            query.deploymentId(id);
            //最新版本过滤
            query.latestVersion();
            //查询
            ProcessDefinition definition = query.singleResult();
            //act_re_procdef表的key
            String key = definition.getKey();
            System.out.println("act_re_procdef表的key" + key);
            String id1 = definition.getId();
            System.out.println("流程定义id1:::" + id1);

            Map<String, Object> map = new HashMap<>();
            map.put("act_re_procdef表的key", key);
            map.put("act_re_procdef表的id", id1);
            return map;
        } catch (Exception e) {
            logger.error("失败：部署工作流：" + e);
            return null;
        }
    }
```

controller层：

```java
 /**
     * 部署流程
     * */
    @RequestMapping("/deploy")
    public Map<String,Object> deploymentFlow(String filePath, String name){
        return flowableService.deploymentFlow(filePath,name);
    }
```

测试:http://localhost:8080/leave/deploy?filePath=processes/leave.bpmn20.xml&name=leave

![1607926565383](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607926565383.png)

#### 7.2启动流程：

service层：

```java
/**
     * 开始流程
     */
    public ProcessInstance startProcessInstance(String processKey, Map map) {
        // 定义流程的key
        // String processDefinitionKey = processKey;
        if (StringUtils.isEmpty(processKey)) {
            return null;
        }
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(processKey, map);

        System.out.println("processInstanceId流程实例ID:" + pi.getId());
        System.out.println("ProcessDefinitionId流程定义ID:" + pi.getProcessDefinitionId());
        return pi;
    }
```

controller层：

```java
/**
     * 启动流程
     * */
    @RequestMapping("/start")
    public Map<String, Object> startProcessInstance(String processKey, String userId){
        Map<String, Object> map = new HashMap<>();
        map.put("taskUser",userId);
        ProcessInstance pi = flowableService.startProcessInstance(processKey, map);
        Map<String, Object> pra = new HashMap<>();
        pra.put("流程实例ID", pi.getId());
        pra.put("流程定义ID:", pi.getProcessDefinitionId());
        return pra;
    }
```

测试：<http://localhost:8080/leave/start?processKey=Expense&userId=1234>

![1607926948879](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607926948879.png)

#### 7.3查看代办任务

service层：

```java
    /**
     * 查询代理人任务
     */
    public List<Task> queryAssigneeTask(String assignee) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        return tasks;
    }
```

controller层：

```java
    /**
     * 查询代理人任务
     * */
    @RequestMapping("/queryTask")
    public Map<String, Object> queryAssigneeTask(String assignee){
        List<Task> tasks=flowableService.queryAssigneeTask(assignee);
        Map<String, Object> map = new HashMap<>();
        int i = 1 ;
        for (Task task : tasks) {
            map.put("task"+i,task.toString());
            i++;
        }
        return map;
    }
```

测试：<http://localhost:8080/leave/queryTask?assignee=1234>

![1607927384934](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607927384934.png)



#### 7.4提交请假申请：

service层：

```java
    /**
     * 完成任务
     */
    public boolean completeTask(String taskId, Map<String, Object> paras) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            logger.error("task:" + task);
            return false;
        }

        if (null == paras) {
            taskService.complete(taskId);
        } else {
            taskService.complete(taskId, paras);
        }

        return true;
    }
```

controller层：

```java
/**
    * 出差报销 
    */
    @RequestMapping("addLeave")
    public String reimburse(String taskId, Integer money) {    
    	Map<String, Object> map = new HashMap<>();    
    	map.put("money", money);    
    	try {        
    		flowableService.completeTask(taskId, map);    
    	} catch (Exception e) {        
    		e.printStackTrace();       
    		return "系统异常";    
    	}    
    	return "申请报销";
    }
```

测试：http://localhost:8080/leave/addLeave?taskId=91541636-3dd4-11eb-a224-005056c00008&day=12

![1607928291573](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607928291573.png)



#### 7.5审核通过;

service层：同上

controller层：

```java
    /**
     * 审核通过
     * */
    @RequestMapping("/pass")
    @ResponseBody
    public String pass(String taskId){
        Map<String, Object> map = new HashMap<>();
        map.put("outcome", "通过");
        try {
            flowableService.completeTask(taskId, map);
        }catch (Exception e){
            e.printStackTrace();
            return "系统异常";
        }
        return "审核通过";
    }
```

测试：http://localhost:8080/leave/pass?taskId=91541636-3dd4-11eb-a224-005056c00008

![1607928536998](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607928536998.png)

#### 7.6驳回

service层：同上

controller层：

```java
    /**
     * 驳回
     * */
    @RequestMapping("/rejectLeave")
    @ResponseBody
    public String rejectLeave(String taskId){
        Map<String, Object> map = new HashMap<>();
        map.put("outcome", "驳回");
        try {
            flowableService.completeTask(taskId, map);
        }catch (Exception e){
            e.printStackTrace();
            return "系统异常";
        }
        return "请假被驳回";
    }

```

#### 7.7查看流程图：

service层：

```java
    /**
     * 查看流程是否完成
     */
    public boolean isFinished(String processInstanceId) {
        return historyService.createHistoricProcessInstanceQuery().finished()
                .processInstanceId(processInstanceId).count() > 0;
    }

   /**
     * 查看流程图
     */
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) {
        /**
         * 获得当前活动的节点
         */
        String processDefinitionId = "";
        if (this.isFinished(processId)) {
            // 如果流程已经结束，则得到结束节点
            HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processId).singleResult();

            processDefinitionId = pi.getProcessDefinitionId();
        } else {
            // 如果流程没有结束，则取当前活动节点
            // 根据流程实例ID获得当前处于活动状态的ActivityId合集
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        }
        List<String> highLightedActivitis = new ArrayList<String>();

        /**
         * 获得活动的节点
         */
        List<HistoricActivityInstance> highLightedActivitList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processId).orderByHistoricActivityInstanceStartTime().asc().list();

        for (HistoricActivityInstance tempActivity : highLightedActivitList) {
            String activityId = tempActivity.getActivityId();
            highLightedActivitis.add(activityId);
        }

        List<String> flows = new ArrayList<>();

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();

        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "bmp", highLightedActivitis, flows, engconf.getActivityFontName(),
                engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(), 1.0, true);
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int legth = 0;
        try {
            out = httpServletResponse.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } catch (IOException e) {
            logger.error("操作异常", e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
```

controller层：

```java
   /**
     * 查看流程图
     */
    @RequestMapping("/processDiagram")
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
        flowableService.genProcessDiagram(httpServletResponse, processId);
    }
```

解决流程图中文乱码：

```java
package com.haiyang.flowable.config;


import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;

/**
 * @author WangChenyang
 * date:  2020-12-14
 * desc: flowable配置----为放置生成的流程图中中文乱码
 */
@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {


    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        engineConfiguration.setActivityFontName("宋体");
        engineConfiguration.setLabelFontName("宋体");
        engineConfiguration.setAnnotationFontName("宋体");
    }
}

```



测试：http://localhost:8080/leave/processDiagram?processId=d7f8661c-3dd9-11eb-92cc-005056c00008

![1607929535708](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607929535708.png)



### 8.流程启动到结束数据库变化：

 部署完毕后，act_re_deployment表中会有一条部署记录，记录这次部署的基本信息，然后是act_ge_bytearray表中有两条记录，记录的是本次上传的bpmn文件和对应的图片文件，每条记录都有act_re_deployment表的外键关联，然后是act_re_procdef表中有一条记录，记录的是该bpmn文件包含的基本信息，包含act_re_deployment表外键。

流程启动，首先向act_ru_execution表中插入一条记录，记录的是这个流程定义的执行实例，其中id和proc_inst_id相同都是流程执行实例id，也就是本次执行这个流程定义的id，包含流程定义的id外键。

然后向act_ru_task插入一条记录，记录的是第一个任务的信息，也就是开始执行第一个任务。包括act_ru_execution表中的execution_id外键和proc_inst_id外键，也就是本次执行实例id。

然后向act_hi_procinst表和act_hi_taskinst表中各插入一条记录，记录的是本次执行实例和任务的历史记录：

任务提交后，首先向act_ru_variable表中插入变量信息，包含本次流程执行实例的两个id外键，但不包括任务的id，因为setVariable方法设置的是全局变量，也就是整个流程都会有效的变量：

当流程中的一个节点任务完成后，进入下一个节点任务，act_ru_task表中这个节点任务的记录被删除，插入新的节点任务的记录。

同时act_ru_execution表中的记录并没有删除，而是将正在执行的任务变成新的节点任务。

同时向act_hi_var_inst和act_hi_taskinst插入历史记录。

整个流程执行完毕，act_ru_task，act_ru_execution和act_ru_variable表相关记录全被清空。

全程有一个表一直在记录所有动作，就是act_hi_actinst表：

以上就是flowable流程启动到结束的所有流程的变化。



### 9.flowable中的五个引擎

- 内容引擎 ContentEngine
- 身份识别引擎 IdmEngine
- 表单引擎 FormEngine
- 决策引擎DmnEngine
- 流程引擎 ProcessEngine

1.**流程引擎 ProcessEngine**
1.1 **RepositoryService

管理流程定义

1.2 **RuntimeService

执行管理，包括启动、推进、删除流程实例等操作

1.3 **TaskService

任务管理

1.4 **HistoryService

历史管理(执行完的数据的管理)

1.5 IdentityService

组织机构管理

1.6 FormService

一个可选服务，任务表单管理

1.7 ManagerService

获取引擎所在的数据库中存在的表、获取表的元数据信息、创建删除等作业、执行命令类、执行自定义SQL、操作事件日志。

1.8 DynamicBpmnService

动态修改Bpmn流程定义以及部署库等操作。

**2.内容引擎ContentEngine**

2.1 内容引擎包含的服务有：ContentService和ContentManagementService。

2.2 ContentManagementService提供对数据库表的管理操作。

Map<String, Long> getTableCount(); 获取每个表的记录数量；
String getTableName(Class<?> flowableEntityClass);根据实体类获得对应的数据库表名
TableMetaData getTableMetaData(String tableName);根据实体类获得对应的数据库表名
TablePageQuery createTablePageQuery();创建一个可以进行排序、根据条件分页的查询类
**3.身份识别引擎 IdmEngine**

身份识别引擎包含的服务有：IdmIdentityService、IdmManagementService、IdmEngineConfiguration。

3.1 IdmIdentityService

- 提供用户的创建、修改、删除、密码修改、登录、用户头像设置等；
- 提供组Group的创建、删除、用户与组关系的关联、删除关联；
- 提供权限的创建、删除、关联等.

3.2 IdmManagementService

对身份识别相关的数据库表进行统计、获取表的列信息。

3.3 IdmEngineConfiguration

提供数据库配置信息。

**4.表单引擎 FormEngine**

4.1 FormManagementService

提供对数据库表的管理操作。

4.2 FormRepositoryService

表单资源服务。

4.3 FormService

提供表单实例的增删改查操作服务。

**5.决策引擎DmnEngine**

5.1 DmnManagementService

该类主要用于获取一系列的数据表元数据信息。

5.2 DmnRepositoryService

动态部署流程资源。

5.3 DmnRuleService

按照规则启动流程实例。

5.4 DmnHistoryService

提供对决策执行历史的访问的服务。