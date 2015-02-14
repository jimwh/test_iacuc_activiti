package edu.columbia.rascal.business.service;

import java.util.List;

import javax.annotation.Resource;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import edu.columbia.rascal.business.service.auxiliary.IacucStatus;

@Service
class IacucProcessQueryService {

    @Resource
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    @Resource
    private RuntimeService runtimeService;

    private static final Logger log = LoggerFactory.getLogger(IacucProcessQueryService.class);

    // don't take for granted, it is possible returning null for all queries

    List<Task> getTaskByBizKey(String bizKey) {
    	Assert.notNull(bizKey, "undefined bizKey");
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .includeProcessVariables();
        return query != null ? query.list() : null;
    }

    Task getTaskByBizKeyAndTaskDefKey(String bizKey, String taskDefKey) {
    	Assert.notNull(bizKey, "undefined bizKey");
    	Assert.notNull(taskDefKey, "undefined taskDefKey");
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(taskDefKey)
                .includeProcessVariables();
                
        if (query == null) return null;
        List<Task> list = query.list();
        if (list == null) return null;
        return list.isEmpty() ? null : list.get(0);
    }

    List<Task> getDesignatedReviewerTasksByBizKey(String bizKey) {
    	Assert.notNull(bizKey, "undefined bizKey");
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .includeProcessVariables();
        return query != null ? query.list() : null;
    }

    List<Task> getOpenTasksByBizKey(String bizKey) {
    	Assert.notNull(bizKey, "undefined bizKey");
    	TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey).processInstanceBusinessKey(bizKey)
                .includeProcessVariables();
        return query != null ? query.list() : null;
    }

    List<Task> getOpenTasksByBizKeyAndCandidateGroup(String bizKey, List<String> candidateGroup) {
    	log.info("candidateGroup.siz="+candidateGroup.size());
    	Assert.notNull(bizKey, "undefined bizKey");
    	TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey).processInstanceBusinessKey(bizKey)
                // .taskCandidateGroupIn(candidateGroup)
                .includeProcessVariables();
        return query != null ? query.list() : null;
    }

    List<Task> getAllTasks() {
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .includeProcessVariables();
        return query != null ? query.list() : null;
    }

    List<Task> getAllAdminTasks() {
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .taskCandidateGroup("ADMIN CAN APPROVE bla bla...")
                .includeProcessVariables();
        return query != null ? query.list() : null;
    }

    List<Task> getDesignatedReviewerTasks() {
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .includeProcessVariables();
        return query != null ? query.list() : null;
    }

    boolean hasTaskForAssignee(String bizKey, String assignee) {
    	Assert.notNull(bizKey, "undefined bizKey");
    	Assert.notNull(assignee, "undefined assignee");
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .includeProcessVariables()
                .taskAssignee(assignee);
        return (query != null) && (query.singleResult() != null);
    }


    List<Task> getUnfinishedTasksByAssignee(String assignee) {
    	Assert.notNull(assignee, "undefined assignee");
    	TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .includeProcessVariables()
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .taskAssignee(assignee)
                .orderByTaskCreateTime()
                .desc();
        return query != null ? query.list() : null;
    }

    Task getTasksByAssignee(String bizKey, String assignee) {
    	Assert.notNull(bizKey, "undefined bizKey");
    	Assert.notNull(assignee, "undefined assignee");

        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .includeProcessVariables()
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .taskAssignee(assignee);
        return query != null ? query.singleResult() : null;
    }

    List<HistoricTaskInstance> getHistoricTaskInstanceByBizKey(String bizKey) {
    	Assert.notNull(bizKey, "undefined bizKey");
        
        // if taskDeleteReason="deleted", that task was closed by activiti.
        // if taskDeleteReason="completed", that task was closed by user action
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .finished()
                .taskDeleteReason("completed")
                .includeTaskLocalVariables()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        return query != null ? query.list() : null;
    }

    List<HistoricTaskInstance> getHistoricTaskInstanceByBizKeyAndProcId(String bizKey, String procId) {
    	Assert.notNull(bizKey, "undefined bizKey");
    	Assert.notNull(procId, "undefined processInstanceId");

    	HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .processInstanceId(procId)
                .finished().includeProcessVariables()
                .orderByTaskCreateTime().desc();
        return query != null ? query.list() : null;
    }


    HistoricTaskInstance getHistoricDistributeTaskByBizKey(String bizKey) {
    	Assert.notNull(bizKey, "undefined bizKey");
        // most recent distribution
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(IacucStatus.DISTRIBUTE.taskDefKey())
                .includeProcessVariables()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        if (query == null) return null;
        List<HistoricTaskInstance> list = query.list();
        return list.isEmpty() ? null : list.get(0);
    }


    List<HistoricTaskInstance> getHistoricTaskInstanceListByAssignee(String assignee) {
    	Assert.notNull(assignee,"undefined assignee");

        try {
            HistoricTaskInstanceQuery query = historyService
                    .createHistoricTaskInstanceQuery()
                    .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                    .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                    .finished().includeProcessVariables()
                    .taskAssignee(assignee)
                    .orderByHistoricTaskInstanceEndTime()
                    .desc();
            return query != null ? query.list() : null;
        } catch (Exception e) {
            log.error("caught error in query:", e);
            return null;
        }
    }

    List<HistoricTaskInstance> getHistoricDesignatedUserReviewTasksByBizKeyAndProcessId(String bizKey, String processId) {
    	Assert.notNull(bizKey, "undefined bizKey");
    	Assert.notNull(processId,"undefined processId");

        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .processInstanceId(processId)
                .taskDefinitionKey(IacucStatus.ASSIGNEEREVIEW.taskDefKey())
                .finished().includeProcessVariables()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        return query != null ? query.list() : null;
    }

    HistoricTaskInstance getHistoricTaskInstanceByBizKeyAndTaskId(String bizKey, String taskId) {
    	Assert.notNull(bizKey, "undefined bizKey");
    	Assert.notNull(taskId, "undefined taskId");

    	HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .finished().includeProcessVariables()
                .taskId(taskId); 
        if (query == null) return null;
        List<HistoricTaskInstance> list = query.list();
        if (list == null) return null;
        return list.isEmpty() ? null : list.get(0);
    }

    HistoricTaskInstance getHistoricTaskInstanceByTaskId(String taskId) {
    	Assert.notNull(taskId, "undefined taskId");
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .includeProcessVariables()
                .taskId(taskId);
        if (query == null) return null;
        List<HistoricTaskInstance> list = query.list();
        if (list == null) return null;
        return list.isEmpty() ? null : list.get(0);
    }

    HistoricTaskInstance getHistoricApprovalTaskInstance(String bizKey) {
    	Assert.notNull(bizKey, "undefined bizKey");
        try {
            HistoricTaskInstanceQuery query = historyService
                    .createHistoricTaskInstanceQuery()
                    .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                    .processInstanceBusinessKey(bizKey)
                    .taskDefinitionKey(IacucStatus.FINALAPPROVAL.taskDefKey())
                    .finished()
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

    
    List<HistoricTaskInstance> getHistoricSuspendedRecord() {
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .taskDefinitionKey(IacucStatus.SUSPEND.taskDefKey())
                .finished().includeProcessVariables()
                .orderByHistoricTaskInstanceEndTime()
                .desc();
        return query != null ? query.list() : null;
    }

	List<ProcessInstance> getInstanceListNotIncludeVar() {
		ProcessInstanceQuery query = runtimeService
				.createProcessInstanceQuery()
				.processDefinitionKey(IacucProcessService.ProtocolProcessDefKey);
		return (query != null) ? query.list() : null;
	}

    List<Task> getTaskListByTaskDefKey(String taskDefKey) {
    	Assert.notNull(taskDefKey, "undefined taskDefKey");
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(IacucProcessService.ProtocolProcessDefKey)
                .taskDefinitionKey(taskDefKey)
                .includeProcessVariables();
        return query.list();
    }

}
