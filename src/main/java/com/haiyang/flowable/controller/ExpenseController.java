package com.haiyang.flowable.controller;


import org.flowable.engine.*;
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
import java.util.Map;

@Controller
@RequestMapping(value = "expense")
public class ExpenseController {
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngine processEngine;

/***************此处为业务代码******************/

    /**
     * 部署流程
     * filePath 文件路径 name 流程名字
     */
    @RequestMapping(value = "deployment")
    @ResponseBody
    public Map<String, Object> deploymentFlow() {
        try {
            String filePath = "processes/demo.bpmn20.xml";
            String name = "ExpenseProcess";
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
            System.out.println("act_re_procdef表的key" + key);
            String id1 = definition.getId();
            System.out.println("流程定义id1:::" + id1);

            Map<String, Object> map = new HashMap<>();
            map.put("act_re_procdef表的key", key);
            map.put("act_re_procdef表的id", id1);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 添加报销
     *
     * @param userId    用户Id
     * @param money     报销金额
     * @param descption 描述
     */
    @RequestMapping(value = "add")
    @ResponseBody
    public String addExpense(String userId, Integer money, String descption) {
        //启动流程
        HashMap<String, Object> map = new HashMap<>();
        map.put("taskUser", userId);
        map.put("money", money);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Expense", map);
        return "提交成功.流程Id为：" + processInstance.getId();
    }

    /**
     * 获取审批管理列表
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public Object list(String userId) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
        for (Task task : tasks) {
            System.out.println(task.toString());
        }
        return tasks.toString();
    }

    /**
     * 批准
     *
     * @param taskId 任务ID
     */
    @RequestMapping(value = "apply")
    @ResponseBody
    public String apply(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("流程不存在");
        }
        //通过审核
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "通过");
        taskService.complete(taskId, map);
        return "processed ok!";
    }

    /**
     * 拒绝
     */
    @ResponseBody
    @RequestMapping(value = "reject")
    public String reject(String taskId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "驳回");
        taskService.complete(taskId, map);
        return "reject";
    }

}