package edu.columbia.rascal.business.service;

import java.io.InputStream;
import java.util.*;

import javax.annotation.Resource;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Attachment;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import edu.columbia.rascal.business.service.auxiliary.IacucCorrespondence;
import edu.columbia.rascal.business.service.auxiliary.IacucDistributeSubcommitteeForm;
import edu.columbia.rascal.business.service.auxiliary.IacucStatus;
import edu.columbia.rascal.business.service.auxiliary.IacucTaskForm;

@Service
class IacucProcessService {

    public static final String ProtocolProcessDefKey = "IacucApprovalProcess";
    public static final String AdverseEventDefKey = "IacucAdverseEvent";

    private static final String STARTED_BY = "STARTED_BY";
    private static final String PROTOCOL_ID = "protocolId";


    private static final String START_GATEWAY = "START_GATEWAY";

    private static final String SNAPSHOT = "snapshot";
    private static final String IACUC_ADMIN = "IACUC_ADMIN";
    private static final String IACUC_COORESPONDENCE = "IacucCorrespondence";

    // used by distribute protocol selection
    private static final String REVIEWER_LIST = "REVIEWER_LIST";
    private static final String ADMIN_NOTE = "ADMIN_NOTE";
    //
    private static final String DESIGNATED_REVIEW_OUTPUT = "DESIGNATED_REVIEW_OUTPUT";
    private static final String TASK_CANCELLED_BY = "TASK_CANCELLED_BY";
    private static final int DESIGNATED_REVIEW_GO_NEXT = 1;
    private static final int DESIGNATED_REVIEW_GO_DISTRIBUTE = 2;

    //
    private static final Map<String, Integer> UndoApprovalGatewayMap = new HashMap<String, Integer>();

    static {
        UndoApprovalGatewayMap.put(IacucStatus.ASSIGNEEREVIEW.taskDefKey(), IacucStatus.ASSIGNEEREVIEW.gatewayValue());
        UndoApprovalGatewayMap.put(IacucStatus.DISTRIBUTE.taskDefKey(), IacucStatus.DISTRIBUTE.gatewayValue());
    }

    private static final String ORIGINAL_REVIEWER = "ORIGINAL_REVIEWER";
    private static final String OTHER_INFO = "OTHER_INFO";

    private static final Logger log = LoggerFactory.getLogger(IacucProcessService.class);

    static final List<String> ListFoo = new ArrayList<String>();

    static {
        ListFoo.add(IacucStatus.DISTRIBUTE.taskDefKey());
        ListFoo.add(IacucStatus.SubcommitteeReview.taskDefKey());
    }

    static final Map<String, List<String>> KeyMap = new HashMap<String, List<String>>();

    static {
        KeyMap.put(IacucStatus.SUBMIT.taskDefKey(), new ArrayList<String>(ListFoo));
    }

    @Resource
    private RuntimeService runtimeService;
    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;

    @Resource
    private IacucProcessQueryService iacucProcessQueryService;

    @Transactional
    boolean startProtocolProcess(String protocolId, String userId, Map<String, Object> processInput) {
        Assert.notNull(processInput);
        if (isProtocolProcessStarted(protocolId)) {
            log.warn("Process was already started for protocolId=" + protocolId);
            return false;
        }
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.SUBMIT.gatewayValue());

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, protocolId, processInput);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.SUBMIT.name());
        log.info("protocolId={}, activityId={}, processId={}", protocolId, instance.getActivityId(), instance.getId());
        return true;
    }


    boolean isProtocolProcessStarted(String bizKey) {
        return getProtocolProcessInstance(bizKey, IacucStatus.SUBMIT.name()) != null;
    }

    boolean hasTaskForAssignee(String protocolId, String userId) {
        return iacucProcessQueryService.hasTaskForAssignee(protocolId, userId);
    }


    @Transactional
    boolean replaceAssignee(String protocolId, String newAssignee, String oldAssignee, String adminNote) {
        Task task = iacucProcessQueryService.getTasksByAssignee(protocolId, oldAssignee);
        if (task == null) {
            log.error("no task for oldAssignee=" + oldAssignee + ",protocolId=" + protocolId);
            return false;
        }
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        } else {
            log.warn("no adminNote in replacement protocolId=" + protocolId + ",oldAssignee=" + oldAssignee + ",newAssignee=" + newAssignee);
        }
        task.setAssignee(newAssignee);
        taskService.saveTask(task);
        return true;
    }

    @Transactional
    String attachSnapshotToTask(String protocolId, String taskDefKey, InputStream content) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, taskDefKey);
        if (task == null) {
            log.error("can't find task=" + taskDefKey);
            return null;
        }
        String attachmentType = "IACUC " + taskDefKey + " " + SNAPSHOT;
        // name: taskDefKey.protocolId.yyyyMMddHHmmss.pdf
        String attachmentName = taskDefKey + "." + protocolId + "." + getCurrentDateString() + ".pdf";
        String attachmentDescription = taskDefKey + " " + SNAPSHOT;

        return attachSnapshot(attachmentType,
                task.getId(),
                task.getProcessInstanceId(),
                attachmentName,
                attachmentDescription,
                content);
    }


    private String attachSnapshot(String attachmentType, String taskId, String procId,
                                  String attachmentName, String description, InputStream content) {
        Attachment attachment = taskService.createAttachment(attachmentType,
                taskId,
                procId,
                attachmentName,
                description,
                content);
        return attachment != null ? attachment.getId() : null;
    }

    private String getCurrentDateString() {
        DateTime dt = new DateTime();
        return dt.toString("yyyyMMddHHmmss");
    }

    boolean hasTaskByTaskDefKey(String protocolId, String taskDefKey) {
        return iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, taskDefKey) != null;
    }

    List<String> getCurrentTaskDefKey(String bizKey) {
        List<String> keySet = new ArrayList<String>();
        try {
            List<Task> taskList = iacucProcessQueryService.getTaskByBizKey(bizKey);
            for (Task task : taskList) {
                keySet.add(task.getTaskDefinitionKey());
            }
        } catch (Exception e) {
            log.error("caught exception:", e);
        }
        return keySet;
    }

    /**
     * @param attachmentId String attachmentId that is the snapshotId
     * @return InputStream if it has one, otherwise null
     */
    InputStream getSnapshotContent(String attachmentId) {
        Attachment attachment = taskService.getAttachment(attachmentId);
        if (attachment != null) {
            log.info(attachment.getName());
        } else {
            return null;
        }
        return taskService.getAttachmentContent(attachmentId);
    }

    List<String> mostRecentReviewerList(String protocolId) {
        List<String> list = new ArrayList<String>();
        HistoricTaskInstance ht = iacucProcessQueryService.getHistoricDistributeTaskByBizKey(protocolId);
        if (ht == null) return list;
        Map<String, Object> map = ht.getTaskLocalVariables();
        if (map.get(REVIEWER_LIST) != null) {
            List<?> reviewers = (ArrayList<?>) map.get(REVIEWER_LIST);
            for (Object o : reviewers) {
                list.add(o.toString());
            }
        }
        return list;
    }


    private void completeTask(Task task, String userId, Map<String, Object> map) {
        String taskId = task.getId();
        taskService.setVariablesLocal(taskId, map);
        taskService.claim(taskId, userId);
        taskService.complete(taskId, map);
    }

    private void completeTask(Task task, String userId) {
        String taskId = task.getId();
        taskService.claim(taskId, userId);
        taskService.complete(taskId);
    }


    IacucTaskForm getPreviousApprovedData(String protocolId) {

        HistoricTaskInstance hs = iacucProcessQueryService.getHistoricApprovalTaskInstance(protocolId);
        if (hs == null) {
            log.warn("couldn't get hs for protcolId={}", protocolId);
            return null;
        }
        IacucTaskForm form = new IacucTaskForm();
        form.setTaskDefKey(IacucStatus.FINALAPPROVAL.taskDefKey());
        form.setTaskName(hs.getName());
        form.setEndTime(hs.getEndTime());
        form.setAuthor(hs.getAssignee());
        form.setTaskId(hs.getId());
        //
        Map<String, Object> localMap = hs.getTaskLocalVariables();
        @SuppressWarnings("unchecked")
        Map<String, String> taskMap = (Map<String, String>) localMap.get("iacucTaskForm" + hs.getId());
        if (taskMap != null) {
            form.setProperties(taskMap);
            form.setComment(getCommentText(form.getCommentId()));
        }
        //
        @SuppressWarnings("unchecked")
        Map<String, String> corrMap = (Map<String, String>) localMap.get(IACUC_COORESPONDENCE + hs.getId());
        if (corrMap != null && !corrMap.isEmpty()) {
            IacucCorrespondence corr = new IacucCorrespondence();
            corr.setProperties(corrMap);
            form.setCorrespondence(corr);
        }
        return form;
    }


    private String getAttachmentId(String taskId) {
        List<Attachment> list = taskService.getTaskAttachments(taskId);
        if (list == null || list.isEmpty()) return null;
        Attachment attachment = list.get(0);
        return attachment.getId();
    }

    IacucTaskForm getHistoryByTaskIdForPdfComparison(String taskId) {
        HistoricTaskInstance hs = iacucProcessQueryService.getHistoricTaskInstanceByTaskId(taskId);
        if (hs == null) {
            log.error("cannot get HistoricTaskInstance by taskId={}", taskId);
            return null;
        }

        String taskDefKey = hs.getTaskDefinitionKey();
        Map<String, Object> procMap = hs.getProcessVariables();
        Object protocolIdObj = procMap.get(PROTOCOL_ID);
        if (protocolIdObj == null) {
            log.error("couldn't get protocolId by taskId={}" + taskId);
            return null;
        }

        String protocolId = protocolIdObj.toString();
        IacucTaskForm history = new IacucTaskForm();
        history.setBizKey(protocolId);
        history.setTaskId(taskId);
        history.setTaskDefKey(taskDefKey);
        history.setSnapshotId(getAttachmentId(hs.getId()));

        return history;
    }

    @Transactional
    void deleteSnapshotById(String attachmentId) {
        taskService.deleteAttachment(attachmentId);
    }


    Set<String> getUnfinishedTaskBizKeyByAssignee(String uni) {
        Set<String> list = new TreeSet<String>();
        List<Task> taskList = iacucProcessQueryService.getUnfinishedTasksByAssignee(uni);
        if (taskList == null) return list;
        for (Task task : taskList) {
            Map<String, Object> map = task.getProcessVariables();
            if (map.get(PROTOCOL_ID) != null) {
                list.add(map.get(PROTOCOL_ID).toString());
            }
        }
        return list;
    }

    Set<String> getFinishedTaskBizKeyByAssignee(String uni) {
        Set<String> bizKeys = new TreeSet<String>();
        List<HistoricTaskInstance> fetchList = iacucProcessQueryService.getHistoricTaskInstanceListByAssignee(uni);
        if (fetchList == null) return bizKeys;
        for (HistoricTaskInstance hs : fetchList) {
            if (hs.getEndTime() == null) {
                continue;
            }
            Map<String, Object> map = hs.getProcessVariables();
            if (map.get(PROTOCOL_ID) != null) {
                bizKeys.add(map.get(PROTOCOL_ID).toString());
            }
        }
        return bizKeys;
    }

    @Transactional
    boolean terminateProtocol(String protocolId, String userId) {
        if (isProtocolProcessStarted(protocolId)) {
            log.error("cannot terminate this protocol because it is process, protocolId={}", protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.TERMINATE.gatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, protocolId, processInput);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.TERMINATE.name());
        log.info("protocolId={}, activityId={}, processId={}", protocolId, instance.getActivityId(), instance.getId());
        return true;
    }

    @Transactional
    boolean suspendProtocol(String protocolId, String userId) {
        if (isProtocolProcessStarted(protocolId)) {
            log.error("cannot suspend this protocol because it is process, protocolId={}", protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.SUSPEND.gatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, protocolId, processInput);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.SUSPEND.name());
        log.info("protocolId={}, activityId={}, processId={}", protocolId, instance.getActivityId(), instance.getId());
        return true;
    }

    @Transactional
    boolean reinstateProtocol(String protocolId, String userId) {
        if (isProtocolProcessStarted(protocolId)) {
            log.error("cannot reinstate this protocol because it is in process, protocolId={}", protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.REINSTATE.gatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, protocolId, processInput);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.REINSTATE.name());
        log.info("protocolId={}, activityId={}, processId={}", protocolId, instance.getActivityId(), instance.getId());
        return true;
    }

    @Transactional
    boolean withdrawProtocol(String protocolId, String userId) {
        if (isProtocolProcessStarted(protocolId)) {
            log.error("cannot withdraw this protocol because it is process, protocolId={}", protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.WITHDRAW.gatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, protocolId, processInput);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.REINSTATE.name());
        log.info("protocolId={}, activityId={}, processId={}", protocolId, instance.getActivityId(), instance.getId());
        return true;
    }


    Set<String> getBizKeyWithDesignatedReviewerTask() {
        Set<String> set = new TreeSet<String>();
        List<Task> taskList = iacucProcessQueryService.getDesignatedReviewerTasks();
        if (taskList == null) return set;
        for (Task task : taskList) {
            Map<String, Object> map = task.getProcessVariables();
            if (map.get(PROTOCOL_ID) != null) {
                set.add(map.get(PROTOCOL_ID).toString());
            }
        }
        return set;
    }

    Set<String> getCurrentReviewerListByProtocolId(String bizKey) {
        Set<String> set = new TreeSet<String>();
        List<Task> list = iacucProcessQueryService.getDesignatedReviewerTasksByBizKey(bizKey);
        if (list == null) return set;
        for (Task task : list) {
            String reviewer = task.getAssignee();
            if (!StringUtils.isBlank(reviewer)) {
                set.add(reviewer);
            }
        }
        return set;
    }

    Map<String, Date> getHistoricSuspendedBizKeyAndDate() {
        Map<String, Date> map = new TreeMap<String, Date>();
        List<HistoricTaskInstance> list = iacucProcessQueryService.getHistoricSuspendedRecord();
        for (HistoricTaskInstance hs : list) {
            Map<String, Object> procMap = hs.getProcessVariables();
            if (procMap == null) continue;
            if (procMap.get(PROTOCOL_ID) != null) {
                String protocolId = procMap.get(PROTOCOL_ID).toString();
                Date endTime = hs.getEndTime();
                if (endTime != null && !StringUtils.isBlank(protocolId)) {
                    map.put(protocolId, endTime);
                }
            }
        }
        return map;
    }


    Map<String, Date> getAdverseEventIdSubmitDate() {
        return getBizKeyAndSubmitDate(AdverseEventDefKey, IacucStatus.SUBMIT.taskDefKey());
    }

    private Map<String, Date> getBizKeyAndSubmitDate(String processDefKey, String taskDefKey) {
        Map<String, Date> map = new TreeMap<String, Date>();
        List<ProcessInstance> instanceList = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey(processDefKey)
                .list();
        for (ProcessInstance instance : instanceList) {
            String businessKey = instance.getBusinessKey();
            Task task = taskService.createTaskQuery()
                    .processDefinitionKey(processDefKey)
                    .processInstanceId(instance.getProcessInstanceId())
                    .taskDefinitionKey(taskDefKey)
                    .singleResult();
            if (task != null) {
                map.put(businessKey, task.getCreateTime());
            }
        }
        return map;
    }

    List<Task> getOpenTasksByBizKeyAndCandidateGroup(String bizKey, List<String> candidateGroup) {
        return iacucProcessQueryService.getOpenTasksByBizKeyAndCandidateGroup(bizKey, candidateGroup);
    }

    Map<String, String> getOpenTaskByBizKey(String bizKey) {
        Map<String, String> map = new TreeMap<String, String>();
        List<Task> list = iacucProcessQueryService.getOpenTasksByBizKey(bizKey);
        for (Task task : list) {
            map.put(task.getTaskDefinitionKey(), task.getName());
        }
        return map;
    }


    private String getCommentText(String commentId) {
        if (commentId == null) return null;
        Comment comment = taskService.getComment(commentId);
        return comment != null ? comment.getFullMessage() : null;
    }

    // add correspondence process
    boolean addCorrespondence(IacucTaskForm taskForm) {
        if (startAddCorrespondenceProcess(taskForm.getBizKey()) != null) {
            completeTaskByTaskForm(ProtocolProcessDefKey, taskForm);
            return true;
        }
        return false;
    }

    boolean addNote(IacucTaskForm taskForm) {
        if (startAddNoteProcess(taskForm.getBizKey()) == null) {
            return false;
        }
        completeTaskByTaskForm(ProtocolProcessDefKey, taskForm);
        return true;
    }

    private ProcessInstance startAddCorrespondenceProcess(String bizKey) {
        ProcessInstance instance = getCorrProcessInstance(bizKey);
        if (instance != null) {
            log.error("add correspondence process is still running, protocolId={}", bizKey);
            return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(START_GATEWAY, IacucStatus.AddCorrespondence.gatewayValue());
        instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, bizKey, map);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.AddCorrespondence.name());
        return instance;
    }

    private ProcessInstance startAddNoteProcess(String bizKey) {
        ProcessInstance instance = getNoteProcessInstance(bizKey);
        if (instance != null) {
            log.error("add note process is still running, protocolId={}", bizKey);
            return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(START_GATEWAY, IacucStatus.AddNote.gatewayValue());
        instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, bizKey, map);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(),
                IacucStatus.AddNote.name());
        return instance;
    }


    private ProcessInstance getCorrProcessInstance(String bizKey) {
        return getProcessInstanceByName(bizKey, IacucStatus.AddCorrespondence.name());
    }

    private ProcessInstance getNoteProcessInstance(String bizKey) {
        return getProcessInstanceByName(bizKey, IacucStatus.AddNote.name());
    }

    private ProcessInstance getProcessInstanceByName(String bizKey, String instanceName) {
        return runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey(ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .processInstanceName(instanceName)
                .singleResult();
    }


    private ProcessInstance getProtocolProcessInstance(String bizKey, String instanceName) {
        return getProcessInstanceByName(bizKey, instanceName);
    }

    Map<String, Date> getBizKeyMeetingDate(Set<String> bizKeys) {
        Assert.notNull(bizKeys);
        Map<String, Date> bizKeyMeetingDate = new HashMap<String, Date>();
        List<ProcessInstance> list = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey(ProtocolProcessDefKey)
                .processInstanceName(IacucStatus.SUBMIT.name())
                .includeProcessVariables().list();
        for (ProcessInstance instance : list) {
            String protocolId = instance.getBusinessKey();
            bizKeys.add(protocolId);
            Map<String, Object> map = instance.getProcessVariables();
            if (map != null) {
                Object obj = map.get("meetingDate");
                if (obj != null) {
                    bizKeyMeetingDate.put(protocolId, (Date) obj);
                }
            }
        }
        return bizKeyMeetingDate;
    }

    Date getMeetingDateByBizKey(String bizKey) {
        List<Task> list = taskService.createTaskQuery()
                .processDefinitionKey(ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .includeProcessVariables()
                .list();
        if (list == null || list.isEmpty())
            return null;
        Task task = list.get(0);
        Map<String, Object> map = task.getProcessVariables();
        return (map.get("meetingDate") == null) ? null : (Date) map.get("meetingDate");
    }

    void deleteProcess(String bizKey, String reason) {
        ProcessInstance instance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(bizKey)
                .singleResult();
        if (instance != null)
            runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), reason);
    }

    List<IacucTaskForm> getTaskFormByBizKeyAndAssginee(String bizKey, String assignee) {
        List<IacucTaskForm> list = new ArrayList<IacucTaskForm>();
        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey(ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .taskAssignee(assignee)
                .list();
        for (Task task : taskList) {
            IacucTaskForm taskForm = new IacucTaskForm();
            taskForm.setBizKey(bizKey);
            taskForm.setBizKey(assignee);
            taskForm.setTaskDefKey(task.getTaskDefinitionKey());
            taskForm.setTaskName(task.getName());
            taskForm.setTaskId(task.getId());
            list.add(taskForm);
        }
        return list;
    }

    List<IacucTaskForm> getPreviousNote(String bizKey) {
        List<IacucTaskForm> list = new ArrayList<IacucTaskForm>();
        List<HistoricTaskInstance> hsList = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey(ProtocolProcessDefKey)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(IacucStatus.AddNote.taskDefKey())
                .finished()
                .includeTaskLocalVariables()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();

        if (hsList == null) return list;

        for (HistoricTaskInstance hs : hsList) {
            Map<String, Object> localMap = hs.getTaskLocalVariables();
            if (localMap == null) continue;

            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) localMap.get("iacucTaskForm" + hs.getId());
            if (map == null) continue;
            IacucTaskForm taskForm = new IacucTaskForm();
            taskForm.setTaskId(hs.getId());
            taskForm.setEndTime(hs.getEndTime());
            taskForm.setProperties(map);
            taskForm.setComment(getCommentText(taskForm.getCommentId()));
            list.add(taskForm);
        }
        return list;
    }

    @Transactional
    boolean startAdverseEventProcess(final String adverseEventId, final String userId) {
        if (getAdverseEventProcessInstance(adverseEventId) != null) {
            log.error("Process was already started for adverseEventId={}, userId={}", adverseEventId, userId);
            return false;
        }
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(AdverseEventDefKey, adverseEventId);
        log.info("adverseEventId={}, activityId={}, processId={}", adverseEventId, instance.getActivityId(), instance.getId());
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.ADVERSEEVENT.name());
        return instance != null;
    }

    private ProcessInstance getAdverseEventProcessInstance(String bizKey) {
        return runtimeService
                .createProcessInstanceQuery()
                .processDefinitionKey(AdverseEventDefKey)
                .processInstanceBusinessKey(bizKey)
                .singleResult();
    }


    @Transactional
    void completeTaskByTaskForm(String processDefKey, IacucTaskForm iacucTaskForm) {
        Assert.notNull(iacucTaskForm);
        String bizKey = iacucTaskForm.getBizKey();
        Assert.notNull(bizKey);
        String taskDefKey = iacucTaskForm.getTaskDefKey();
        Task task = getTask(processDefKey, bizKey, taskDefKey);
        Assert.notNull(task);
        String taskId = task.getId();
        if (task.getAssignee() == null) {
            task.setAssignee(iacucTaskForm.getAuthor());
            taskService.claim(taskId, iacucTaskForm.getAuthor());
        }

        // if you want to store comment in activity task comment, then... otherwise do nothing
        String content = iacucTaskForm.getComment();
        if (content != null) {
            Comment comment = taskService.addComment(taskId, task.getProcessInstanceId(), taskDefKey, content);
            iacucTaskForm.setCommentId(comment.getId());
        }

        // attach attribute to this task
        Map<String, String> attribute = iacucTaskForm.getProperties();
        if (attribute != null && !attribute.isEmpty())
            taskService.setVariableLocal(taskId, "iacucTaskForm" + taskId, attribute);

        // attach correspondence to this task
        IacucCorrespondence corr = iacucTaskForm.getCorrespondence();
        if (corr != null) {
            corr.apply();
            Map<String, String> corrProperties = corr.getProperties();
            if (!corrProperties.isEmpty()) {
                taskService.setVariableLocal(taskId, IACUC_COORESPONDENCE + taskId, corrProperties);
            }
        }

        // for show business
        if (IacucStatus.DistributeSubcommittee.isDefKey(taskDefKey)) {
            if (iacucTaskForm instanceof IacucDistributeSubcommitteeForm) {
                taskService.setVariable(taskId, "meetingDate", iacucTaskForm.getDate());
            }
        }

        // determine the direction
        Map<String, Object> map = iacucTaskForm.getTaskVariables();
        if (map != null && !map.isEmpty())
            taskService.complete(taskId, map); // go left/right/middle or go ...
        else
            taskService.complete(taskId); // go straight
    }


    @Transactional
    String attachSnapshotToAdverseEventTask(final String adverseEvtId, final String taskDefKey, final InputStream content) {
        Task task = getTask(AdverseEventDefKey, adverseEvtId, taskDefKey);
        if (task == null) {
            log.error("no task taskDefKey={}, adverseEvtId={}", taskDefKey, adverseEvtId);
            return null;
        }
        String attachmentType = "IACUC_ADVERSE_EVT_" + taskDefKey + "_" + SNAPSHOT;
        // name: taskDefKey.adverseEvtid.yyyyMMddHHmmss.pdf
        String attachmentName = taskDefKey + ".adverse.evt." + adverseEvtId + "." + getCurrentDateString() + ".pdf";
        String attachmentDescription = taskDefKey + " " + SNAPSHOT;
        return attachSnapshot(attachmentType,
                task.getId(),
                task.getProcessInstanceId(),
                attachmentName,
                attachmentDescription,
                content);
    }


    private Task getTask(String processDefKey, String bizKey, String taskDefKey) {
        TaskQuery query = taskService.createTaskQuery()
                .processDefinitionKey(processDefKey)
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(taskDefKey);
        return query == null ? null : query.singleResult();
    }


    List<IacucTaskForm> getIacucProtocolHistory(String protocolId) {
        return getHistory(ProtocolProcessDefKey, protocolId);
    }

    List<IacucTaskForm> getIacucAdverseHistory(String aevtId) {
        return getHistory(AdverseEventDefKey, aevtId);
    }

    private List<IacucTaskForm> getHistory(String processDefKey, String bizKey) {

        List<IacucTaskForm> listIacucTaskForm = new ArrayList<IacucTaskForm>();

        List<HistoricTaskInstance> list = getHistoriceTaskInstance(processDefKey, bizKey);

        if (list == null || list.isEmpty())
            return listIacucTaskForm;

        for (HistoricTaskInstance hs : list) {
            IacucTaskForm iacucTaskForm = new IacucTaskForm();
            iacucTaskForm.setTaskId(hs.getId());
            iacucTaskForm.setEndTime(hs.getEndTime());
            //
            Map<String, Object> localMap = hs.getTaskLocalVariables();
            @SuppressWarnings("unchecked")
            Map<String, String> taskMap = (Map<String, String>) localMap.get("iacucTaskForm" + hs.getId());

            // restore the original attribute
            iacucTaskForm.setProperties(taskMap);

            // two options:
            // if comment is stored in variable, then do nothing
            // if comment is stored in task comment, then as follow
            iacucTaskForm.setComment(getCommentText(iacucTaskForm.getCommentId()));

            // two options:
            // if the snapshot id is retrieved from here, then bla bla ...
            // iacucTaskForm.setSnapshotId(snapshotId);
            // if the snapshot id is pre-stored in properties, then do nothing

            // restore the original correspondence if any
            @SuppressWarnings("unchecked")
            Map<String, String> corrMap = (Map<String, String>) localMap.get(IACUC_COORESPONDENCE + hs.getId());
            if (corrMap != null && !corrMap.isEmpty()) {
                IacucCorrespondence corr = new IacucCorrespondence();
                corr.setProperties(corrMap);
                iacucTaskForm.setCorrespondence(corr);
            }

            // for the sake of old data
            if (iacucTaskForm.getTaskDefKey() == null) {
                iacucTaskForm.setTaskDefKey(hs.getTaskDefinitionKey());
            }
            if (iacucTaskForm.getTaskName() == null) {
                iacucTaskForm.setTaskName(hs.getName());
            }
            if (iacucTaskForm.getAuthor() == null) {
                iacucTaskForm.setAuthor(hs.getAssignee());
            }

            listIacucTaskForm.add(iacucTaskForm);
        }

        return listIacucTaskForm;
    }

    private List<HistoricTaskInstance> getHistoriceTaskInstance(String processDefKey, String bizKey) {
        // if taskDeleteReason="deleted", that task was closed by activity.
        // if taskDeleteReason="completed", that task was closed by user action
        HistoricTaskInstanceQuery query = historyService
                .createHistoricTaskInstanceQuery()
                .processDefinitionKey(processDefKey)
                .processInstanceBusinessKey(bizKey).finished()
                .taskDeleteReason("completed").includeTaskLocalVariables()
                .orderByHistoricTaskInstanceEndTime().desc();
        return query != null ? query.list() : null;
    }
}
