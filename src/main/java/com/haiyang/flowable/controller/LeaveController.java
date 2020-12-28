package com.haiyang.flowable.controller;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private HistoryService historyService;


    /**
     * 部署流程
     * */
    @RequestMapping("/deploy")
    @ResponseBody
    public Map<String,Object> deploymentFlow(String filePath, String name){
        try {
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
     * 启动流程
     * */
    @RequestMapping("/start")
    @ResponseBody
    public Map<String, Object> startProcessInstance(String processKey, String userId){
        Map<String, Object> map = new HashMap<>();
        map.put("taskUser",userId);
        // 定义流程的key
        // String processDefinitionKey = processKey;
        if (StringUtils.isEmpty(processKey)) {
            return null;
        }
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(processKey, map);

        System.out.println("processInstanceId流程实例ID:" + pi.getId());
        System.out.println("ProcessDefinitionId流程定义ID:" + pi.getProcessDefinitionId());
        Map<String, Object> pra = new HashMap<>();
        pra.put("流程实例ID", pi.getId());
        pra.put("流程定义ID:", pi.getProcessDefinitionId());
        return pra;
    }

    /**
     * 查询代理人任务
     * */
    @RequestMapping("/queryTask")
    @ResponseBody
    public Map<String, Object> queryAssigneeTask(String assignee){
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();        Map<String, Object> map = new HashMap<>();
        int i = 1 ;
        for (Task task : tasks) {
            map.put("task"+i,task.toString());
            i++;
        }
        return map;
    }

    /**
     * 完成任务
     */
    public boolean completeTask(String taskId, Map<String, Object> paras) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return false;
        }
        if (null == paras) {
            taskService.complete(taskId);
        } else {
            taskService.complete(taskId, paras);
        }
        return true;
    }

    /**
     * 请假
     */
    @RequestMapping("addLeave")
    @ResponseBody
    public String reimburse(String taskId, Integer day) {
        Map<String, Object> map = new HashMap<>();
        map.put("day", day);
        try {
            completeTask(taskId, map);
        } catch (Exception e) {
            e.printStackTrace();
            return "系统异常";
        }
        return "提交请假申请";
    }


    /**
     * 审核通过
     * */
    @RequestMapping("/pass")
    @ResponseBody
    public String pass(String taskId){
        Map<String, Object> map = new HashMap<>();
        map.put("outcome", "通过");
        try {
            completeTask(taskId, map);
        }catch (Exception e){
            e.printStackTrace();
            return "系统异常";
        }
        return "审核通过";
    }

    /**
     * 驳回
     * */
    @RequestMapping("/rejectLeave")
    @ResponseBody
    public String rejectLeave(String taskId){
        Map<String, Object> map = new HashMap<>();
        map.put("outcome", "驳回");
        try {
            completeTask(taskId, map);
        }catch (Exception e){
            e.printStackTrace();
            return "系统异常";
        }
        return "请假被驳回";
    }

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
    @RequestMapping("/processDiagram")
    @ResponseBody
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
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
            e.printStackTrace();
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
        }    }
}