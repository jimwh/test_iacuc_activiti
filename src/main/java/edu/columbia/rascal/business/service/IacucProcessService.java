package edu.columbia.rascal.business.service;

import java.io.InputStream;
import java.util.*;

import javax.annotation.Resource;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Attachment;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import edu.columbia.rascal.business.service.auxiliary.IacucActivitiAdminProcessForm;
import edu.columbia.rascal.business.service.auxiliary.IacucCorrespondence;
import edu.columbia.rascal.business.service.auxiliary.IacucDesignatedUserReview;
import edu.columbia.rascal.business.service.auxiliary.IacucDistributeSubcommitteeForm;
import edu.columbia.rascal.business.service.auxiliary.IacucStatus;
import edu.columbia.rascal.business.service.auxiliary.IacucTaskForm;

@Service
class IacucProcessService {

    public static final String ProtocolProcessDefKey = "IacucApprovalProcess";
    private static final String STARTED_BY = "STARTED_BY";
    private static final String PROTOCOL_ID = "protocolId";

    private static final String ADVERSE_EVENT_APPROVED = "ADVERSE_EVENT_APPROVED";
    private static final String ADVERSE_EVENT_ID = "adverseEventId";

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
    private static final Map<String, Integer>UndoApprovalGatewayMap=new HashMap<String, Integer>();
    static {
        UndoApprovalGatewayMap.put(IacucStatus.ASSIGNEEREVIEW.taskDefKey(), IacucStatus.ASSIGNEEREVIEW.gatewayValue());
        UndoApprovalGatewayMap.put(IacucStatus.DISTRIBUTE.taskDefKey(), IacucStatus.DISTRIBUTE.gatewayValue());
    }

    private static final String ORIGINAL_REVIEWER="ORIGINAL_REVIEWER";
    private static final String OTHER_INFO="OTHER_INFO";
    
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
    boolean startProtocolProcess(String protocolId, String userId, Map<String,Object>processInput) {
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

    // adverseEventId is the real OID
    private String adverseEventBizKey(String adverseEventId) {
        return "AEVT-" + adverseEventId;
    }

    @Transactional
    boolean startAdverseEventProcess(final String adverseEventId, final String userId) {
        final String adverseEvtBizKey = adverseEventBizKey(adverseEventId);
        if (isProtocolProcessStarted(adverseEvtBizKey)) {
            log.warn("Process was already started for adverseEventId=" + adverseEventId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(ADVERSE_EVENT_ID, adverseEvtBizKey);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.ADVERSEEVENT.gatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, adverseEvtBizKey, processInput);
        log.info("adverseEventId=" + adverseEventId + ",activityId=" + instance.getActivityId() + ",processId=" + instance.getId());
        return true;
    }

    boolean isAdverEventProcessStarted(final String adverseEventId) {
        final String adverseEvtBizKey = adverseEventBizKey(adverseEventId);
        return getProcessInstanceByName(adverseEvtBizKey, IacucStatus.ADVERSEEVENT.name()) != null;
    }

    boolean isProtocolProcessStarted(String bizKey) {
        return getProtocolProcessInstance(bizKey, IacucStatus.SUBMIT.name()) != null;
    }
    
    /*
    private ProcessInstance getProcessInstanceByBizKey(String bizKey) {
        ProcessInstanceQuery instanceQuery = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey);
        return (instanceQuery != null) ? instanceQuery.singleResult() : null;
    }
	*/


    boolean hasTaskForAssignee(String protocolId, String userId) {
        return iacucProcessQueryService.hasTaskForAssignee(protocolId, userId);
    }

    @Transactional
    boolean completeTaskByAssignee(String protocolId, String assignee, IacucDesignatedUserReview detail) {
        if (detail == null) {
            log.error("input data undefined by assignee=" + assignee);
            return false;
        }
        // it may not be set in the front view
        if (StringUtils.isBlank(detail.getUserId())) {
            detail.setUserId(assignee);
        }
        detail.applyAction();
        Map<String, Object> review = detail.fieldsToMap();
        if (review.isEmpty()) {
            log.error("input data undefined by assignee=" + assignee);
            return false;
        }
        Task task = iacucProcessQueryService.getTasksByAssignee(protocolId, assignee);
        if (task == null) {
            log.error("no designated reviewer task by assignee=" + assignee + ",protocolId=" + protocolId);
            return false;
        }
        // if there are any open task has correspondences, then move them to here
        // then clean that previous attached the correspondence for view purpose later on
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(assignee);
        task.setName( detail.getDisplayAction() );
        Map<String, Object> map = new HashMap<String, Object>();
        String key = assignee + "_" + task.getId();
        map.put(key, review);
        map.put(DESIGNATED_REVIEW_OUTPUT, DESIGNATED_REVIEW_GO_NEXT);
        map.put(OTHER_INFO + task.getId(), detail.getDisplayAction());
        
        taskService.saveTask(task);
        completeTask(task, assignee, map);
        return true;
    }
    

    @Transactional
    boolean completeAdverseEventApprovalTask(String adverseEventId,
                                             String userId,
                                             boolean bool,
                                             String note) {
        final String adverseEvtBizKey = adverseEventBizKey(adverseEventId);
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(adverseEvtBizKey, IacucStatus.ADVERSEEVENT.taskDefKey());
        if (task == null) {
            log.error("No task=" + IacucStatus.ADVERSEEVENT.taskDefKey() + " for adverseEventId=" + adverseEventId);
            return false;
        }
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ADVERSE_EVENT_APPROVED, bool);
        if (!StringUtils.isBlank(note)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, note);
        }
        completeTask(task, userId, map);
        return true;
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


    /**
     * Cancel all assignee tasks and go back to distribution task
     */

    boolean cancelReviewersTaskGoRedistribute(String protocolId, String userId, IacucActivitiAdminProcessForm form) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DESIGNATED_REVIEW_OUTPUT, DESIGNATED_REVIEW_GO_DISTRIBUTE);
        return adminStepIntoTask(protocolId, userId, map, form);
    }


    @Transactional
    private boolean adminStepIntoTask(String protocolId, String userId, Map<String, Object> map, IacucActivitiAdminProcessForm form) {
        List<Task> taskList = iacucProcessQueryService.getDesignatedReviewerTasksByBizKey(protocolId);
        if (taskList == null) {
            log.error("no task=" + IacucStatus.ASSIGNEEREVIEW.taskDefKey() + " for protocolId=" + protocolId);
            return false;
        }
        boolean adminNoteAdded = false; // don't need to put the same note for a list of tasks
        for (Task task : taskList) {
            String old = task.getAssignee();
            String key = TASK_CANCELLED_BY + task.getId();
            String text = old + "'s review task cancelled";
            log.info("text=" + text + ", userId=" + userId);
            map.put(key, text);
            map.put(ORIGINAL_REVIEWER + task.getId(), old);
            if ( !adminNoteAdded ) {
                String adminNote = form.getAdminNote();
                if (!StringUtils.isBlank(adminNote)) {
                    taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
                    adminNoteAdded = true;
                }
            }
            taskService.unclaim(task.getId());
            task.setAssignee(userId);
            taskService.claim(task.getId(), userId);
            completeTask(task, userId, map);
        }
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

    @Transactional
    String attachSnapshotToAdverseEventTask(final String adverseEvtId, final String taskDefKey, final InputStream content) {
        final String adverseEvtBizKey = adverseEventBizKey(adverseEvtId);
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(adverseEvtBizKey, taskDefKey);
        if (task == null) {
            log.error("can't find task=" + taskDefKey + " by adverseEvtId=" + adverseEvtId);
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

    boolean hasAdverseEventTaskByTaskDefKey(String aevtId, String taskDefKey) {
        final String adverseEvtBizKey = adverseEventBizKey(aevtId);
        return iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(adverseEvtBizKey, taskDefKey) != null;
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

    private List<Attachment> getAttachmentList(String protocolId) {
        List<Attachment> attachmentList = new ArrayList<Attachment>();
        List<HistoricTaskInstance> htList = iacucProcessQueryService.getHistoricTaskInstanceByBizKey(protocolId);
        for (HistoricTaskInstance ht : htList) {
            if (IacucStatus.SUBMIT.isDefKey(ht.getTaskDefinitionKey())) {
                List<Attachment> procList = taskService.getProcessInstanceAttachments(ht.getProcessInstanceId());
                if (!procList.isEmpty()) {
                    attachmentList.addAll(procList);
                }
            } else {
                List<Attachment> list = taskService.getTaskAttachments(ht.getId());
                if (!list.isEmpty()) {
                    attachmentList.addAll(list);
                }
            }
        }
        return attachmentList;
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

    
    List<IacucTaskForm> getIacucAdverseHistory(final String adverseEventId) {
    	//TODO
        return new ArrayList<IacucTaskForm>();
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
        IacucTaskForm form=new IacucTaskForm();
        form.setTaskDefKey(IacucStatus.FINALAPPROVAL.taskDefKey());
        form.setTaskName(hs.getName());
        form.setEndTime(hs.getEndTime());
        form.setAuthor(hs.getAssignee());
        form.setTaskId(hs.getId());
        //
        Map<String, Object> localMap = hs.getTaskLocalVariables();
        @SuppressWarnings("unchecked")
		Map<String, String> taskMap = (Map<String, String>) localMap.get("iacucTaskForm" + hs.getId());
        if( taskMap != null) {
        	form.setProperties(taskMap);
        	form.setComment(getCommentText(form.getCommentId()));
        }
        //
        @SuppressWarnings("unchecked")
		Map<String, String> corrMap = (Map<String, String>) localMap.get("IacucCorrespondence" + hs.getId());
        if (corrMap != null && !corrMap.isEmpty()) {
            IacucCorrespondence corr = new IacucCorrespondence();
            corr.setProperties(corrMap);
            form.setCorrespondence(corr);
        }
        return form;
    }

    
    private void fillSnapshotId(final IacucTaskForm history, final String id) {

    	List<Attachment> attachmentList = taskService.getTaskAttachments(id);
        if(attachmentList == null) return;
        for(Attachment attachment: attachmentList) {
        	if(id.equals(attachment.getTaskId())) {
        		history.setSnapshotId(attachment.getId());
        		break;
        	}
        }
    }

    
    IacucTaskForm getHistoryByTaskIdForPdfComparison(String taskId) {
        HistoricTaskInstance hs = iacucProcessQueryService.getHistoricTaskInstanceByTaskId(taskId);
        if (hs == null) {
            log.error("cannot get HistoricTaskInstance by taskId=" + taskId);
            return null;
        }

        String taskDefKey = hs.getTaskDefinitionKey();
        Map<String, Object> procMap = hs.getProcessVariables();
        Object protocolIdObj = procMap.get(PROTOCOL_ID);
        if (protocolIdObj == null) {
            log.error("couldn't get protocolId by taskId=" + taskId);
            return null;
        }
        String protocolId = protocolIdObj.toString();
        IacucTaskForm history = new IacucTaskForm(); //IacucProcessHistoricData.create(hs.getProcessInstanceId(), protocolId, hs.getId(), true);
        history.setBizKey(protocolId);
        history.setTaskId(taskId);
        history.setTaskDefKey(taskDefKey);

        fillSnapshotId(history, hs.getId());
        return history;
    }


    /**
     * @param bizKey       - for protocol it will be header id; for adverse event it will be adverse event id
     * @param attachmentId - snapshotId
     */
    @Transactional
    void deleteSnapshotById(String bizKey, String attachmentId) {
        List<Attachment> attachmentList = getAttachmentList(bizKey);
        for (Attachment a : attachmentList) {
            // has to test it, otherwise it will throw exception
            if (a.getId().equals(attachmentId)) {
                taskService.deleteAttachment(attachmentId);
                return;
            }
        }
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
            log.error("cannot terminate this protocol because it is process, protocolId={}",protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.TERMINATE.gatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, protocolId, processInput);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.TERMINATE.name());
        log.info("protocolId={}, activityId={}, processId={}",protocolId, instance.getActivityId(), instance.getId());
        return true;
    }

    @Transactional
    boolean suspendProtocol(String protocolId, String userId) {
        if (isProtocolProcessStarted(protocolId)) {
            log.error("cannot suspend this protocol because it is process, protocolId={}",protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.SUSPEND.gatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, protocolId, processInput);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.SUSPEND.name());
        log.info("protocolId={}, activityId={}, processId={}",protocolId, instance.getActivityId(), instance.getId());
        return true;
    }

    @Transactional
    boolean reinstateProtocol(String protocolId, String userId) {
        if (isProtocolProcessStarted(protocolId)) {
            log.error("cannot reinstate this protocol because it is in process, protocolId={}",protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.REINSTATE.gatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, protocolId, processInput);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.REINSTATE.name());
        log.info("protocolId={}, activityId={}, processId={}",protocolId, instance.getActivityId(), instance.getId());
        return true;
    }

    @Transactional
    boolean withdrawProtocol(String protocolId, String userId) {
        if (isProtocolProcessStarted(protocolId)) {
            log.error("cannot withdraw this protocol because it is process, protocolId={}",protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.WITHDRAW.gatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, protocolId, processInput);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.REINSTATE.name());
        log.info("protocolId={}, activityId={}, processId={}",protocolId, instance.getActivityId(), instance.getId());
        return true;
    }


    Map<String, String> getBizKeyTaskDefKeyMap() {
    	Map<String, String> retmap = new TreeMap<String, String>();
        List<Task> taskList;
        try {
            taskList = iacucProcessQueryService.getAllTasks();
        } catch (Exception e) {
            log.error("caught error:", e);
            return retmap;
        }
        if (taskList == null) return retmap;
        for (Task task : taskList) {
            Map<String, Object> taskMap = task.getProcessVariables();
            if (taskMap.get(PROTOCOL_ID) == null) { continue; }
            String protocolId=taskMap.get(PROTOCOL_ID).toString();
           	retmap.put(protocolId,  task.getTaskDefinitionKey());
           	log.info("protocolId={},taskDefKey={}", protocolId,task.getTaskDefinitionKey() );
        }
        return retmap;
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


    Map<String, Date>getHistoricSuspendedBizKeyAndDate() {
    	Map<String, Date>map = new TreeMap<String, Date>();
    	List<HistoricTaskInstance> list = iacucProcessQueryService.getHistoricSuspendedRecord();
    	for(HistoricTaskInstance hs: list) {
    		Map<String, Object>procMap= hs.getProcessVariables();
    		if(procMap==null) continue;
    		if( procMap.get(PROTOCOL_ID)!=null ) {
    			String protocolId = procMap.get(PROTOCOL_ID).toString();
    			Date endTime = hs.getEndTime();
    			if(endTime!=null && !StringUtils.isBlank(protocolId)) {
    				map.put(protocolId, endTime);
    			}
    		}
    	}
    	return map;
    }
    

    Map<String, Date>getAdverseEventIdSubmitDate() {
    	Map<String, Date>map=new HashMap<String, Date>();
    	List<Task> taskList=iacucProcessQueryService.getTaskListByTaskDefKey(IacucStatus.ADVERSEEVENT.taskDefKey());
    	if(taskList==null || taskList.isEmpty() ) return map;
    	for(Task task: taskList) {
    		Map<String, Object>taskMap=task.getProcessVariables();
    		if( taskMap.get(ADVERSE_EVENT_ID) != null ) {
    			String bizKey = taskMap.get(ADVERSE_EVENT_ID).toString();
    			String[] sa = bizKey.split("-");
    			if( sa.length != 2 ) continue;
    			map.put(sa[1],  task.getCreateTime());
    		}
    	}
    	return map;
    }
    
    List<Task> getOpenTasksByBizKeyAndCandidateGroup(String bizKey, List<String> candidateGroup) {
    	
    	return iacucProcessQueryService.getOpenTasksByBizKeyAndCandidateGroup(bizKey, candidateGroup);
    }

    Map<String,String>getOpenTaskByBizKey(String bizKey) {
    	Map<String, String>map=new TreeMap<String,String>();
    	List<Task>list=iacucProcessQueryService.getOpenTasksByBizKey(bizKey);
    	for(Task task: list) {
    		map.put(task.getTaskDefinitionKey(), task.getName());
    	}
    	return map;
    }


    @Transactional
    void completeTaskByTaskForm(IacucTaskForm iacucTaskForm) {
        Assert.notNull(iacucTaskForm);
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(iacucTaskForm.getBizKey(), iacucTaskForm.getTaskDefKey());
        Assert.notNull(task);
        String taskId = task.getId();
        taskService.claim(taskId, iacucTaskForm.getAuthor());

        // if you want to store comment in activity task comment, then... otherwise do nothing
        String content = iacucTaskForm.getComment();
        if (content != null) {
            Comment comment = taskService.addComment(taskId, task.getProcessInstanceId(), iacucTaskForm.getTaskDefKey(), content);
            iacucTaskForm.setCommentId(comment.getId());
        }

        // attach attribute to this task
        Map<String, String> attribute = iacucTaskForm.getProperties();
        if (attribute != null && !attribute.isEmpty())
            taskService.setVariableLocal(taskId, "iacucTaskForm" + taskId, attribute);

        // attach correspondence to this task
        IacucCorrespondence corr = iacucTaskForm.getCorrespondence();
        if (corr != null) {
        	Map<String, String> corrProperties = corr.getProperties();
            if (!corrProperties.isEmpty()) {
                taskService.setVariableLocal(taskId, IACUC_COORESPONDENCE + taskId, corrProperties);
            }
        }
        
        // for show business
        if( IacucStatus.DistributeSubcommittee.isDefKey(iacucTaskForm.getTaskDefKey())) {
            if (iacucTaskForm instanceof IacucDistributeSubcommitteeForm) {
                taskService.setVariable(taskId,"meetingDate", iacucTaskForm.getDate());
            }
        }

        // determine the direction
        Map<String, Object> map = iacucTaskForm.getTaskVariables();
        if (map != null && !map.isEmpty())
            taskService.complete(taskId, map); // go left/right/middle or go ...
        else
            taskService.complete(taskId); // go straight
    }

    
    List<IacucTaskForm> getIacucProtocolHistory(String protocolId) {
    	
    	List<IacucTaskForm> listIacucTaskForm=new ArrayList<IacucTaskForm>();
    	
        List<HistoricTaskInstance> list = iacucProcessQueryService.getHistoricTaskInstanceByBizKey(protocolId);
        if(list==null || list.isEmpty()) return listIacucTaskForm;
        
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
            if( iacucTaskForm.getTaskDefKey()==null ) {
            	iacucTaskForm.setTaskDefKey(hs.getTaskDefinitionKey());
            }
            if( iacucTaskForm.getTaskName()==null ) {
            	iacucTaskForm.setTaskName(hs.getName());
            }
            if( iacucTaskForm.getAuthor()==null) {
            	iacucTaskForm.setAuthor(hs.getAssignee());
            }
            listIacucTaskForm.add(iacucTaskForm);
        }
        return listIacucTaskForm;
    }

    private String getCommentText(String commentId) {
        if (commentId == null) return null;
        Comment comment = taskService.getComment(commentId);
        return comment != null ? comment.getFullMessage() : null;
    }

    // add correspondence process
    boolean addCorrespondence(IacucTaskForm taskForm) {
    	if( startAddCorrespondenceProcess(taskForm.getBizKey()) != null ) {
    		completeTaskByTaskForm(taskForm);
    		return true;
    	}
    	return false;
    }
    
    private ProcessInstance startAddCorrespondenceProcess(String bizKey) {
        ProcessInstance instance = getCorrProcessInstance(bizKey);
        if( instance != null ) {
        	log.error("add correspondence process is still running, protocolId={}", bizKey);
        	return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(START_GATEWAY, IacucStatus.AddCorrespondence.gatewayValue());
        instance = runtimeService.startProcessInstanceByKey(ProtocolProcessDefKey, bizKey, map);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), IacucStatus.AddCorrespondence.name());
        return instance;
    }

    private ProcessInstance getCorrProcessInstance(String bizKey) {
        return getProcessInstanceByName(bizKey, IacucStatus.AddCorrespondence.name());
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
					bizKeyMeetingDate.put(protocolId, (Date)obj);
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
		if (map.get("meetingDate") == null)
			return null;
		return (Date) map.get("meetingDate");
	}
}
