package edu.columbia.rascal.business.service;

import java.util.List;

import javax.annotation.Resource;

import org.activiti.engine.HistoryService;

import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;

import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.columbia.rascal.business.service.auxiliary.IacucStatus;

@Service
class IacucProcessQueryService {

    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;

    private static final Logger log = LoggerFactory.getLogger(IacucProcessQueryService.class);

    // don't take for granted, it is possible returning null for all queries

    /**
     * **
     * List<ProcessInstance> getInstanceList() {
     * ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery()
     * .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
     * .includeProcessVariables()
     * .orderByProcessInstanceId()
     * .asc();
     * return (query != null) ? query.list() : null;
     * }
     * **
     */

    Task getTaskByBizKey(String bizKey) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .includeProcessVariables()
                .includeTaskLocalVariables();
        if (query == null) return null;
        List<Task> list = query.list();
        if (list == null) return null;
        return list.isEmpty() ? null : list.get(0);
    }

    Task getTaskByBizKeyAndTaskDefKey(String bizKey, String taskDefKey) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        if (StringUtils.isBlank(taskDefKey)) {
            log.error("undefined taskDefKey");
            return null;
        }
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(taskDefKey)
                .includeProcessVariables()
                .includeTaskLocalVariables();
        if (query == null) return null;
        List<Task> list = query.list();
        if (list == null) return null;
        return list.isEmpty() ? null : list.get(0);
    }

    List<Task> getDesignatedReviewerTasksByBizKey(String bizKey) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .includeProcessVariables();
        return query != null ? query.list() : null;
    }

    List<Task> getAllTasks() {
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .includeProcessVariables();
        return query != null ? query.list() : null;
    }

    List<Task> getDesignatedReviewerTasks() {
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .includeProcessVariables();
        return query != null ? query.list() : null;
    }

    boolean hasTaskForAssignee(String bizKey, String assignee) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return false;
        }
        if (StringUtils.isBlank(assignee)) {
            log.error("undefined assignee");
            return false;
        }
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .taskAssignee(assignee);
        return (query != null) && (query.singleResult() != null);
    }


    List<Task> getUnfinishedTasksByAssignee(String assignee) {
        // it will throw exception if not test it
        if (StringUtils.isBlank(assignee)) {
            log.error("undefined assignee");
            return null;
        }
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .taskAssignee(assignee)
                .orderByTaskCreateTime()
                .desc();
        return query != null ? query.list() : null;
    }

    Task getTasksByAssignee(String bizKey, String assignee) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        if (StringUtils.isBlank(assignee)) {
            log.error("undefined assignee");
            return null;
        }
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .taskAssignee(assignee);
        return query != null ? query.singleResult() : null;
    }

    List<HistoricTaskInstance> getHistoricTaskInstanceByBizKey(String bizKey) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .includeProcessVariables()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        return query != null ? query.list() : null;
    }

    List<HistoricTaskInstance> getHistoricTaskInstanceByBizKeyAndProcId(String bizKey, String procId) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        if (StringUtils.isBlank(procId)) {
            log.error("undefined processInstanceId");
            return null;
        }
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .processInstanceId(procId)
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .orderByTaskCreateTime().desc();
        return query != null ? query.list() : null;
    }


    HistoricTaskInstance getHistoricDistributeTaskByBizKey(String bizKey) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        // most recent distribution
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(IacucStatus.DISTRIBUTE.taskDefKey())
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        if (query == null) return null;
        List<HistoricTaskInstance> list = query.list();
        return list.isEmpty() ? null : list.get(0);
    }


    List<HistoricTaskInstance> getHistoricTaskInstanceListByAssignee(String assignee) {
        // it won't throw exception but will return all records that is not the intention
        if (StringUtils.isBlank(assignee)) {
            log.error("undefined assignee");
            return null;
        }

        try {
            HistoricTaskInstanceQuery query = historyService
                    .createHistoricTaskInstanceQuery()
                    .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                    .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                    .includeProcessVariables()
                    .includeTaskLocalVariables()
                    .taskAssignee(assignee)
                    .orderByHistoricTaskInstanceEndTime()
                    .desc();
            return query != null ? query.list() : null;
        } catch (Exception e) {
            log.error("caught error in query:", e);
            return null;
        }
    }

    List<HistoricTaskInstance> getHistoricDesignatedUserReviewTasksByBizKey(String bizKey) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        return query != null ? query.list() : null;
    }

    List<HistoricTaskInstance> getHistoricDesignatedUserReviewTasksByBizKeyAndProcessId(String bizKey, String processId) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        if (StringUtils.isBlank(processId)) {
            log.error("undefined processId");
            return null;
        }

        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .processInstanceId(processId)
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        return query != null ? query.list() : null;
    }

    HistoricTaskInstance getHistoricTaskInstanceByBizKeyAndTaskId(String bizKey, String taskId) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        if (StringUtils.isBlank(taskId)) {
            log.error("undefined taskId");
            return null;
        }
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey)
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .taskId(taskId); //.list();
        if (query == null) return null;
        List<HistoricTaskInstance> list = query.list();
        if (list == null) return null;
        return list.isEmpty() ? null : list.get(0);
    }

    HistoricTaskInstance getHistoricTaskInstanceByTaskId(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            log.error("undefined taskId");
            return null;
        }
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                .includeProcessVariables()
                .includeTaskLocalVariables()
                .taskId(taskId);
        if (query == null) return null;
        List<HistoricTaskInstance> list = query.list();
        if (list == null) return null;
        return list.isEmpty() ? null : list.get(0);
    }

    HistoricTaskInstance getHistoricApprovalTaskInstance(String bizKey) {
        if (StringUtils.isBlank(bizKey)) {
            log.error("undefined bizKey");
            return null;
        }
        try {
            HistoricTaskInstanceQuery query = historyService
                    .createHistoricTaskInstanceQuery()
                    .processDefinitionKey(IacucProcessService.PROCESS_DEF_KEY)
                    .processInstanceBusinessKey(bizKey)
                    .taskDefinitionKey(IacucStatus.FINALAPPROVAL.taskDefKey())
                    .includeProcessVariables()
                    .includeTaskLocalVariables()
                    .orderByHistoricTaskInstanceEndTime()
                    .desc();
            if (query == null) return null;
            List<HistoricTaskInstance> list = query.list();
            if (list == null) return null;
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            // most likely deseriallize trouble in old data set
            log.error("caught exception:", e);
            return null;
        }
    }

}
