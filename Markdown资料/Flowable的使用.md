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



### 7.实现controller层：

```java
package com.haiyang.flowable.controller;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping(value = "leave")
public class LeaveController {
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngine processEngine;

    /**
     * 添加请假
     *
     * @param userId    用户Id
     * @param day     请假天数
     * @param descption 描述
     */
    @RequestMapping(value = "add")
    @ResponseBody
    public String addLeave(String userId, Integer day, String descption) {
        String filePath = "processes/leave.bpmn20.xml";
        String name = "leave";
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
                .addClasspathResource(filePath).name(name);
        Deployment deployment = deploymentBuilder.deploy();
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
        //启动流程
        HashMap<String, Object> map = new HashMap<>();
        map.put("taskUser", userId);
        map.put("day", day);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key, map);
        return "提交成功.流程Id为：" + processInstance.getId();
    }


    /**
     * 获取taskId
     * @param userId
     * @return
     */
    public String getTaskId(String userId) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
        try {
            String id = tasks.get(tasks.size() - 1).getId();
            return id;
        }catch (ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
            return "此申请已通过或已驳回！";
        }
    }

    /**
     * 批准
     * @param userId 任务ID
     */
    @RequestMapping(value = "apply")
    @ResponseBody
    public String apply(String userId) {
        String taskId = getTaskId(userId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "此申请已通过或已驳回！";
        }
        //通过审核
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "通过");
        taskService.complete(taskId, map);
        return "已批准";
    }


    /**
     * 驳回
     * @param userId
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "reject")
    public String reject(String userId) {
        String taskId = getTaskId(userId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "驳回");
        taskService.complete(taskId, map);
        return "被驳回";
    }
}
```



### 8.添加两个代理类：

```java
package com.haiyang.flowable.listener;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;
@Component
public class DeanTaskHandler implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.setAssignee("院长");
    }

}

```



```java
package com.haiyang.flowable.listener;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

@Component
public class InstructorTaskHandler implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.setAssignee("辅导员");
    }

}
```



### 9.测试:

1.新建请假：在地址栏输入http://localhost:8080/leave/add?userId=1616&day=6

![1607667093759](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607667093759.png)

2.通过：在地址栏输入http://localhost:8080/leave/apply?userId=1616

![1607667175318](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607667175318.png)

3.驳回：在地址栏输入http://localhost:8080/leave/reject?userId=1717

![1607667456637](C:\Users\Coder Wang\AppData\Roaming\Typora\typora-user-images\1607667456637.png)