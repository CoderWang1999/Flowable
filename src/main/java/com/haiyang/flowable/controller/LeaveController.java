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