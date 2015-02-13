package edu.columbia.rascal.business.service;

import java.io.InputStream;
import java.util.*;

import javax.annotation.Resource;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Attachment;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.columbia.rascal.business.service.auxiliary.IacucActivitiAdminProcessForm;
import edu.columbia.rascal.business.service.auxiliary.IacucApprovalType;
import edu.columbia.rascal.business.service.auxiliary.IacucCorrespondence;
import edu.columbia.rascal.business.service.auxiliary.IacucDesignatedUserReview;
import edu.columbia.rascal.business.service.auxiliary.IacucProcessHistoricData;
import edu.columbia.rascal.business.service.auxiliary.IacucReviewType;
import edu.columbia.rascal.business.service.auxiliary.IacucStatus;

@Service
class IacucProcessService {

    public static final String PROCESS_DEF_KEY = "TestApprovalProcess";
    private static final String STARTED_BY = "STARTED_BY";
    private static final String PROTOCOL_ID = "protocolId";

    private static final String ADVERSE_EVENT_APPROVED = "ADVERSE_EVENT_APPROVED";
    private static final String ADVERSE_EVENT_ID = "adverseEventId";

    private static final String START_GATEWAY = "START_GATEWAY";

    private static final String SNAPSHOT = "snapshot";
    private static final String IACUC_ADMIN = "IACUC_ADMIN";
    private static final String IACUC_COORESPONDENCE = "IacucCorrespondence";

    // used by distribute protocol selection
    private static final String DISTRIBUTE_SELECTION = "DISTRIBUTE_SELECTION";
    private static final String REVIEWER_LIST = "REVIEWER_LIST";
    private static final String ADMIN_NOTE = "ADMIN_NOTE";
    private static final String MEETING_DATE = "MEETING_DATE";
    private static final String REVIEW_TYPE = "REVIEW_TYPE";
    //
    private static final String RETURN_TO_PI = "RETURN_TO_PI";
    //
    private static final String DESIGNATED_REVIEW_OUTPUT = "DESIGNATED_REVIEW_OUTPUT";
    private static final String TASK_CANCELLED_BY = "TASK_CANCELLED_BY";
    private static final int DESIGNATED_REVIEW_GO_NEXT = 1;
    private static final int DESIGNATED_REVIEW_GO_DISTRIBUTE = 2;
    private static final int DESIGNATED_REVIEW_GO_RETURNTOPI = 3;
    //
    private static final String ADMIN_REVIEW_OUTPUT = "ADMIN_REVIEW_OUTPUT";
    //
    private static final String SUB_COMMITTEE_REVIEW_OUTPUT = "SUB_COMMITTEE_REVIEW_OUTPUT";
    private static final int SUB_COMMITTEE_GO_RETURNTOPI = 1;
    private static final int SUB_COMMITTEE_GO_DISTRIBUTE = 2;
    private static final int SUB_COMMITTEE_GO_APPROVAL = 3;
    //
    private static final String RETURNTOPI_OUTPUT = "RETURNTOPI_OUTPUT";
    private static final int RETURNTOPI_OUTPUT_GO_UNDO = 1;
    private static final int RETURNTOPI_OUTPUT_GO_RELEASE = 2;
    private static final int RETURNTOPI_OUTPUT_GO_END = 3;
    //
    private static final String UNDO_RETURNTOPI_OUTPUT = "UNDO_RETURNTOPI_OUTPUT";
    private static final Map<String, Integer>UndoReturnToPiGatewayMap=new HashMap<String, Integer>();
    static {
        UndoReturnToPiGatewayMap.put(IacucStatus.SUBMIT.taskDefKey(), IacucStatus.SUBMIT.startGatewayValue());
        UndoReturnToPiGatewayMap.put(IacucStatus.DISTRIBUTE.taskDefKey(), IacucStatus.DISTRIBUTE.startGatewayValue());
        UndoReturnToPiGatewayMap.put(IacucStatus.SUBCOMITTEEREVIEW.taskDefKey(), IacucStatus.SUBCOMITTEEREVIEW.startGatewayValue());
        UndoReturnToPiGatewayMap.put(IacucStatus.ADMINREVIEW.taskDefKey(), IacucStatus.ADMINREVIEW.startGatewayValue());
        UndoReturnToPiGatewayMap.put(IacucStatus.ASSIGNEEREVIEW.taskDefKey(), IacucStatus.ASSIGNEEREVIEW.startGatewayValue());
        UndoReturnToPiGatewayMap.put(IacucStatus.RETURNTOPI.taskDefKey(), IacucStatus.RETURNTOPI.startGatewayValue());
    }
    //
    private static final String APPROVAL_OUTPUT = "APPROVAL_OUTPUT";
    private static final int APPROVAL_OUTPUT_GO_UNDO = 1;
    private static final int APPROVAL_OUTPUT_GO_RELEASE = 2;
    private static final int APPROVAL_OUTPUT_GO_END = 3;
    //
    private static final String UNDO_APPROVAL_OUTPUT = "UNDO_APPROVAL_OUTPUT";
    private static final Map<String, Integer>UndoApprovalGatewayMap=new HashMap<String, Integer>();
    static {
        UndoApprovalGatewayMap.put(IacucStatus.ADMINREVIEW.taskDefKey(), IacucStatus.ADMINREVIEW.startGatewayValue());
        UndoApprovalGatewayMap.put(IacucStatus.ASSIGNEEREVIEW.taskDefKey(), IacucStatus.ASSIGNEEREVIEW.startGatewayValue());
        UndoApprovalGatewayMap.put(IacucStatus.DISTRIBUTE.taskDefKey(), IacucStatus.DISTRIBUTE.startGatewayValue());
    }

    private static final Logger log = LoggerFactory.getLogger(IacucProcessService.class);

    @Resource
    private RuntimeService runtimeService;
    @Resource
    private TaskService taskService;
    @Resource
    private IacucProcessQueryService iacucProcessQueryService;

    @Transactional
    String startProtocolProcess(String protocolId, String userId) {
        if (isProcessStarted(protocolId)) {
            log.warn("Process was already started for protocolId=" + protocolId);
            return null;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.SUBMIT.startGatewayValue());

        try {
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY, protocolId, processInput);
            // log.info("protocolId=" + protocolId + ",activityId=" + instance.getActivityId() + ",processId=" + instance.getId());
            return instance.getProcessInstanceId();
        }catch(Exception e) {
            log.error("protocolId="+protocolId+",userId="+userId, e);
            return null;
        }
    }

    // adverseEventId is the real OID
    private String adverseEventBizKey(String adverseEventId) {
        return "AEVT-" + adverseEventId;
    }

    @Transactional
    boolean startAdverseEventProcess(final String adverseEventId, final String userId) {
        final String adverseEvtBizKey = adverseEventBizKey(adverseEventId);
        if (isProcessStarted(adverseEvtBizKey)) {
            log.warn("Process was already started for adverseEventId=" + adverseEventId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(ADVERSE_EVENT_ID, adverseEvtBizKey);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.ADVERSEEVENT.startGatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY, adverseEvtBizKey, processInput);
        log.info("adverseEventId=" + adverseEventId + ",activityId=" + instance.getActivityId() + ",processId=" + instance.getId());
        return true;
    }

    boolean isAdverEventProcessStarted(final String adverseEventId) {
        final String adverseEvtBizKey = adverseEventBizKey(adverseEventId);
        return getProcessInstanceByBizKey(adverseEvtBizKey) != null;
    }

    boolean isProcessStarted(String bizKey) {
        return getProcessInstanceByBizKey(bizKey) != null;
    }

    private ProcessInstance getProcessInstanceByBizKey(String bizKey) {
        ProcessInstanceQuery instanceQuery = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_DEF_KEY)
                .processInstanceBusinessKey(bizKey);
        return (instanceQuery != null) ? instanceQuery.singleResult() : null;
    }

    @Transactional
    String completePreliminaryReviewTask(String protocolId, String userId, boolean returnToPiBool) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.SUBMIT.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.SUBMIT.taskDefKey() + " for protocolId=" + protocolId);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RETURN_TO_PI, returnToPiBool);
        completeTask(task, userId, map);
        return taskId;
    }

    @Transactional
    String completeDistributeTaskForApproval(String protocolId, String userId, String note) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.DISTRIBUTE.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.DISTRIBUTE.taskDefKey() + ",protocolId=" + protocolId);
            return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DISTRIBUTE_SELECTION, 99);
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        if (!StringUtils.isBlank(note)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, note);
        }
        completeTask(task, userId, map);
        return taskId;
    }

    @Transactional
    String completeDistributeTaskForReturnToPi(String protocolId, String userId, String note) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.DISTRIBUTE.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.DISTRIBUTE.taskDefKey() + ",protocolId=" + protocolId);
            return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DISTRIBUTE_SELECTION, 98);
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        if (!StringUtils.isBlank(note)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, note);
        }
        completeTask(task, userId, map);
        return taskId;
    }

    @Transactional
    String completeDistributeTask(String protocolId, String userId, IacucActivitiAdminProcessForm form) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.DISTRIBUTE.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.DISTRIBUTE.taskDefKey() + ",protocolId=" + protocolId);
            return null;
        }
        String reviewType = form.getReviewType();
        Map<String, Object> map = new HashMap<String, Object>();
        if (form.getMeetingDate() != null) {
            map.put(MEETING_DATE + task.getId(), form.getMeetingDate());
        }

        if (IacucReviewType.SubIacucCommitte.isType(reviewType)) {
            map.put(DISTRIBUTE_SELECTION, IacucReviewType.SubIacucCommitte.getGatewayValue());
            map.put(REVIEW_TYPE, IacucReviewType.SubIacucCommitte.typeCode());
        } else if (IacucReviewType.DesignatedReviewers.isType(reviewType)) {
            map.put(DISTRIBUTE_SELECTION, IacucReviewType.DesignatedReviewers.getGatewayValue());
            map.put(REVIEW_TYPE, IacucReviewType.DesignatedReviewers.typeCode());
            List<String> assigneeList = form.getReviewerList();
            if (assigneeList == null || assigneeList.isEmpty()) {
                log.error("reviewers undefined for task=" + IacucStatus.DISTRIBUTE.taskDefKey() + ",protocolId=" + protocolId);
                return null;
            }
            map.put(REVIEWER_LIST, assigneeList);
            map.put("nrOfInstances", assigneeList.size());
        } else {
            log.error("unknown reviewType=" + reviewType + ",task=" + IacucStatus.DISTRIBUTE.taskDefKey() + ",protocolId=" + protocolId);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        String adminNote = form.getAdminNote();
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        completeTask(task, userId, map);
        return taskId;
    }

    @Transactional
    String completeSubCommitteeReviewTask(String protocolId, String userId, final boolean goApprovalPath) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.SUBCOMITTEEREVIEW.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.SUBCOMITTEEREVIEW.taskDefKey() + " for protocolId=" + protocolId);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IacucReviewType.SubIacucCommitte.typeCode());
        task.setAssignee(userId);
        Map<String, Object> map = new HashMap<String, Object>();
        if(goApprovalPath) {
            map.put(SUB_COMMITTEE_REVIEW_OUTPUT, SUB_COMMITTEE_GO_APPROVAL);
        }else {
            map.put(SUB_COMMITTEE_REVIEW_OUTPUT, SUB_COMMITTEE_GO_RETURNTOPI);
        }
        completeTask(task, userId, map);
        return taskId;
    }

    boolean adminCancelSubCommitteeReviewTask(String protocolId, String userId, String adminNote) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.SUBCOMITTEEREVIEW.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.SUBCOMITTEEREVIEW.taskDefKey() + " for protocolId=" + protocolId);
            return false;
        }
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(TASK_CANCELLED_BY + task.getId(), "Task cancelled by " + userId);
        map.put(SUB_COMMITTEE_REVIEW_OUTPUT, SUB_COMMITTEE_GO_DISTRIBUTE);
        task.setOwner(IacucReviewType.SubIacucCommitte.typeCode());
        task.setAssignee(userId);
        completeTask(task, userId, map);
        return true;
    }

    boolean hasTaskForAssignee(String protocolId, String userId) {
        return iacucProcessQueryService.hasTaskForAssignee(protocolId, userId);
    }

    @Transactional
    String completeTaskByAssignee(String protocolId, String assignee, IacucDesignatedUserReview detail) {
        if (detail == null) {
            log.error("input data undefined by assignee=" + assignee);
            return null;
        }
        // it may not be set in the front view
        if (StringUtils.isBlank(detail.getUserId())) {
            detail.setUserId(assignee);
        }
        detail.applyAction();
        Map<String, Object> review = detail.fieldsToMap();
        if (review.isEmpty()) {
            log.error("input data undefined by assignee=" + assignee);
            return null;
        }
        Task task = iacucProcessQueryService.getTasksByAssignee(protocolId, assignee);
        if (task == null) {
            log.error("no designated reviewer task by assignee=" + assignee + ",protocolId=" + protocolId);
            return null;
        }
        final String taskId=task.getId();
        // if there are any open task has correspondences, then move them to here
        // then clean that previous attached the correspondence for view purpose later on
        // moveCorrPosition(task, protocolId);
        //
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(assignee);
        Map<String, Object> map = new HashMap<String, Object>();
        String key = assignee + "_" + task.getId();
        map.put(key, review);
        map.put(DESIGNATED_REVIEW_OUTPUT, DESIGNATED_REVIEW_GO_NEXT);
        completeTask(task, assignee, map);
        return taskId;
    }

    public Set<String> getAssigneeListByBizKey(String protocolId) {
        Set<String> assigneeList=new HashSet<String>();
        List<Task> list = iacucProcessQueryService.getDesignatedReviewerTasksByBizKey(protocolId);
        if(list != null) {
            for(Task task: list) {
                String userId=task.getAssignee();
                if(userId!=null) {
                    assigneeList.add(userId);
                }
            }
        }
        return assigneeList;
    }
    private void moveCorrPosition(Task paramTask, String protocolId) {

    	List<Task> list = iacucProcessQueryService.getDesignatedReviewerTasksByBizKey(protocolId);
        if (list == null) return;
        if (list.isEmpty()) return;
        if (list.size() == 1) return;
        Map<String, Map<String, Object>> corrMap = new TreeMap<String, Map<String, Object>>();
        
        for (Task task : list) {
            if (task.getId().equals(paramTask.getId())) continue;
            Map<String, Map<String, Object>> corrMapTemp = getCorrMapByTaskId(task);
            if (corrMapTemp != null && !corrMapTemp.isEmpty()) {
                corrMap.putAll(corrMapTemp);
                taskService.removeVariable(task.getId(), IACUC_COORESPONDENCE + task.getId());
            }
        }

        if (corrMap.isEmpty()) {
            return;
        }

        Map<String, Map<String, Object>> paramCorrMap = getCorrMapByTaskId(paramTask);
        if (paramCorrMap != null && !paramCorrMap.isEmpty()) {
            corrMap.putAll(paramCorrMap);
        }

        taskService.setVariable(paramTask.getId(), IACUC_COORESPONDENCE + paramTask.getId(), corrMap);
    }
    
    private Map<String, Map<String, Object>> getCorrMapByTaskId(Task task) {
        String corrKey = IACUC_COORESPONDENCE + task.getId();
        Map<String, Object> procMap = task.getProcessVariables();
        if (procMap == null) return null;
        //
        Object obj = procMap.get(corrKey);
        if (obj == null) return null;
        //
        if (obj instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> savedMap = (Map<String, Map<String, Object>>) obj;
            return savedMap;
        }
        return null;
    }

    String completeAdminReviewTask(String protocolId, String userId, IacucActivitiAdminProcessForm form) {
        if (form == null) {
            log.error("input data undefined for task=" + IacucStatus.ADMINREVIEW.taskDefKey() + ",protocolId=" + protocolId);
            return null;
        }
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.ADMINREVIEW.taskDefKey());
        if (task == null) {
            log.error("cannot find task=" + IacucStatus.ADMINREVIEW.taskDefKey() + ",protocolId=" + protocolId);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        Map<String, Object> map = new HashMap<String, Object>();
        String adminNote = form.getAdminNote();
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        String reviewOutcome = form.getAdminReviewOutcome();
        int gatewayValue = getAdminReviewOutputGatewayValue(reviewOutcome);
        if (gatewayValue == 0) {
            log.error("undefined adminReviewOutcome=" + reviewOutcome + ",task=" + IacucStatus.ADMINREVIEW.taskDefKey() + ",protocolId=" + protocolId);
            return null;
        } else {
            map.put(ADMIN_REVIEW_OUTPUT, gatewayValue);
        }
        completeTask(task, userId, map);
        return taskId;
    }

    private int getAdminReviewOutputGatewayValue(String outcome) {
        if (IacucApprovalType.APPROVE.isType(outcome)) {
            return IacucApprovalType.APPROVE.getGatewayValue();
        } else if (IacucApprovalType.RETURNTOPI.isType(outcome)) {
            return IacucApprovalType.RETURNTOPI.getGatewayValue();
        } else if (IacucApprovalType.REDISTRIBUTE.isType(outcome)) {
            return IacucApprovalType.REDISTRIBUTE.getGatewayValue();
        } else if (IacucApprovalType.FULLCOMMITTEE.isType(outcome)) {
            return IacucApprovalType.FULLCOMMITTEE.getGatewayValue();
        } else {
            return 0;
        }
    }

    // bizKey: protocolId for protocol; eventId for adverse event
    @Transactional
    String completeReturnToPI(String bizKey, String userId, String adminNote) {
        return completeReturnToPI(bizKey,userId,adminNote, RETURNTOPI_OUTPUT_GO_UNDO);
    }

    @Transactional
    String completeReturnToPiTaskGoRelease(String bizKey, String userId, String adminNote) {
        return completeReturnToPI(bizKey, userId, adminNote,RETURNTOPI_OUTPUT_GO_RELEASE);
    }

    @Transactional
    String completeReturnToPiTaskGoEnd(String bizKey, String userId, String adminNote) {
        return completeReturnToPI(bizKey, userId, adminNote, RETURNTOPI_OUTPUT_GO_END);
    }
    private String completeReturnToPI(String bizKey, String userId, String adminNote, int gatewayValue) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(bizKey, IacucStatus.RETURNTOPI.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.RETURNTOPI.taskDefKey() + " for bizKey=" + bizKey);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RETURNTOPI_OUTPUT, gatewayValue);
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        completeTask(task, userId, map);
        return taskId;
    }

    @Transactional
    String completeUndoReturnToPI(String bizKey, String userId, String adminNote) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(bizKey, IacucStatus.UndoReturnToPI.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.UndoReturnToPI.taskDefKey() + " for bizKey=" + bizKey);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        Map<String, Object>map=new HashMap<String, Object>();
        map.put(UNDO_RETURNTOPI_OUTPUT, getUndoReturnToPIGateway(bizKey));
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        completeTask(task, userId, map);
        return taskId;
    }

    @Transactional
    String cancelUndoReturnToPI(String bizKey, String userId, String adminNote) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(bizKey, IacucStatus.UndoReturnToPI.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.UndoReturnToPI.taskDefKey() + " for bizKey=" + bizKey);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        Map<String, Object>map=new HashMap<String, Object>();
        map.put(UNDO_RETURNTOPI_OUTPUT, 100);
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        completeTask(task, userId, map);
        return taskId;
    }

    private int getUndoReturnToPIGateway(String protocolId) {
        List<HistoricTaskInstance> list = iacucProcessQueryService.getHistoricTaskInstanceByBizKey(protocolId);
        // 0 - current open task; 1 - return to PI task; 2 - anything before return to PI
        if( list==null || list.size()<3 ) {
            // default rollback to distribution
            return UndoReturnToPiGatewayMap.get(IacucStatus.DISTRIBUTE.taskDefKey());
        }
        String taskDefKey = list.get(2).getTaskDefinitionKey();
        if( UndoReturnToPiGatewayMap.get(taskDefKey)!=null ) {
            return UndoReturnToPiGatewayMap.get(taskDefKey);
        }
        // default rollback to distribution
        return UndoReturnToPiGatewayMap.get(IacucStatus.DISTRIBUTE.taskDefKey());
    }

    @Transactional
    String completeFinalApprovalTask(String protocolId, String userId, String adminNote) {
        return completeApproval(protocolId, userId, adminNote, APPROVAL_OUTPUT_GO_UNDO);
    }
    @Transactional
    String completeApprovalTaskGoRelease(String protocolId, String userId, String adminNote) {
        return completeApproval(protocolId, userId, adminNote, APPROVAL_OUTPUT_GO_RELEASE);
    }
    @Transactional
    String completeFinalApprovalTaskGoEnd(String protocolId, String userId, String adminNote) {
        return completeApproval(protocolId, userId, adminNote, APPROVAL_OUTPUT_GO_END);
    }

    private String completeApproval(String protocolId, String userId, String adminNote, int gatewayValue) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.FINALAPPROVAL.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.FINALAPPROVAL.taskDefKey() + ",protocolId=" + protocolId);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(APPROVAL_OUTPUT, gatewayValue);
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        completeTask(task, userId, map);
        return taskId;
    }

    @Transactional
    String completeUndoApprovalTask(String protocolId, String userId, String adminNote) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.UndoApproval.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.UndoApproval.taskDefKey() + ",protocolId=" + protocolId);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(UNDO_APPROVAL_OUTPUT, getUndoApprovalGateway(protocolId));
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        completeTask(task, userId, map);
        return taskId;
    }

    private int getUndoApprovalGateway(String protocolId) {
        List<HistoricTaskInstance> list = iacucProcessQueryService.getHistoricTaskInstanceByBizKey(protocolId);
        if( list==null || list.isEmpty() ) {
            log.warn("sanity check it should never be happened");
            return UndoApprovalGatewayMap.get(IacucStatus.DISTRIBUTE.taskDefKey());
        }
        // typically 0 - current, 1 - approval, 2 - before approval
        if( list.size() < 3 ) {
            log.warn("abnormal size");
            return UndoApprovalGatewayMap.get(IacucStatus.DISTRIBUTE.taskDefKey());
        }
        String taskDefKey=list.get(2).getTaskDefinitionKey();
        if( IacucStatus.ADMINREVIEW.isDefKey(taskDefKey) ) {
            log.info("gateway value for undoApproval="+UndoApprovalGatewayMap.get(taskDefKey));
            if( UndoApprovalGatewayMap.get(taskDefKey) != null ) {
                return UndoApprovalGatewayMap.get(taskDefKey);
            }
        }
        // default rollback to distribution
        return UndoApprovalGatewayMap.get(IacucStatus.DISTRIBUTE.taskDefKey());
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

    /**
     * Cancel all assignee tasks and go back to ReturnToPI task
     */
    boolean cancelReviewersTaskGoReturnToPi(String protocolId, String userId, IacucActivitiAdminProcessForm form) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DESIGNATED_REVIEW_OUTPUT, DESIGNATED_REVIEW_GO_RETURNTOPI);
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
            if (!adminNoteAdded) {
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
    String attachSnapshotToTask(String protocolId, String taskDefKey, InputStream content, Date date) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, taskDefKey);
        if (task == null) {
            log.error("can't find task=" + taskDefKey);
            return null;
        }
        String attachmentType = "IACUC " + taskDefKey + " " + SNAPSHOT;
        // name: taskDefKey.protocolId.yyyyMMddHHmmss.pdf
        String attachmentName = taskDefKey + "." + protocolId + "." + getCurrentDateString(date) + ".pdf";
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

    @Transactional
    String attachSnapshotToProcess(String protocolId, InputStream content) {
        ProcessInstance instance = getProcessInstanceByBizKey(protocolId);
        if (instance == null) {
            log.error("can't get instance by protocolId=" + protocolId);
            return null;
        }
        String attachmentType = "IACUC " + IacucStatus.SUBMIT.statusName() + " " + SNAPSHOT;
        // name: submit.protocolId.yyyyMMddHHmmss.pdf
        String attachmentName = IacucStatus.SUBMIT.statusName() + "." + protocolId + "." + getCurrentDateString() + ".pdf";
        String attachmentDescription = IacucStatus.SUBMIT.statusName() + " " + SNAPSHOT;

        return attachSnapshot(attachmentType,
                null,
                instance.getProcessInstanceId(),
                attachmentName,
                attachmentDescription,
                content);
    }
    @Transactional
    String attachSnapshotToProcess(String protocolId, InputStream content, Date date) {
        ProcessInstance instance = getProcessInstanceByBizKey(protocolId);
        if (instance == null) {
            log.error("can't get instance by protocolId=" + protocolId);
            return null;
        }
        String attachmentType = "IACUC " + IacucStatus.SUBMIT.statusName() + " " + SNAPSHOT;
        // name: submit.protocolId.yyyyMMddHHmmss.pdf
        String attachmentName = IacucStatus.SUBMIT.statusName() + "." + protocolId + "." + getCurrentDateString(date) + ".pdf";
        String attachmentDescription = IacucStatus.SUBMIT.statusName() + " " + SNAPSHOT;

        return attachSnapshot(attachmentType,
                null,
                instance.getProcessInstanceId(),
                attachmentName,
                attachmentDescription,
                content);
    }

    @Transactional
    String attachSnapshotToAdverseEventProcess(final String adverseEventId, final InputStream content) {
        final String adverseEvtBizKey = adverseEventBizKey(adverseEventId);
        ProcessInstance instance = getProcessInstanceByBizKey(adverseEvtBizKey);
        if (instance == null) {
            log.error("can't get instance by adverseEventId=" + adverseEventId);
            return null;
        }
        String attachmentType = "IACUC_ADVERSE_EVT_" + IacucStatus.SUBMIT.statusName() + "_" + SNAPSHOT;
        // name: submit.adverse.evt.evtId.yyyyMMddHHmmss.pdf
        String attachmentName = IacucStatus.SUBMIT.statusName() + ".adverse.evt." + adverseEventId + "." + getCurrentDateString() + ".pdf";
        String attachmentDescription = IacucStatus.SUBMIT.statusName() + " " + SNAPSHOT;

        return attachSnapshot(attachmentType,
                null,
                instance.getProcessInstanceId(),
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
    private String getCurrentDateString(Date date) {
        DateTime dt = new DateTime(date);
        return dt.toString("yyyyMMddHHmmss");
    }

    boolean hasAdverseEventTaskByTaskDefKey(String aevtId, String taskDefKey) {
        final String adverseEvtBizKey = adverseEventBizKey(aevtId);
        return iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(adverseEvtBizKey, taskDefKey) != null;
    }

    boolean hasTaskByTaskDefKey(String protocolId, String taskDefKey) {
        return iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, taskDefKey) != null;
    }

    String getCurrentTaskDefKey(String bizKey) {
        try {
            Task task = iacucProcessQueryService.getTaskByBizKey(bizKey);
            return task != null ? task.getTaskDefinitionKey() : null;
        } catch (Exception e) {
            log.error("caught exception:", e);
            return null;
        }
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

    /**
     * Basically it is trying to filter out the previous vote, i.e.:
     * 1. reviewers: yy229, rscl1099, at2582
     * 2. yy229: vote Approve; rscl1099: vote Hold; at2582 had not voted yet.
     * <p/>
     * Administrator came in and cancelled all these tasks and redistribute
     * 3. to new reviewers: rscl1099, yy229, es3139, rscl1088
     * 4. yy229: vote Full Committee Review;  rscl1099: vote Hold; es3139: vote Approve;
     * <p/>
     * above kind of cycles could be over and over again.
     * So the return list should be contained only the most recent voting results.
     */
    List<IacucDesignatedUserReview> getCurrentReviewersVotingStatus(String protocolId) {
        List<IacucDesignatedUserReview> retList = new ArrayList<IacucDesignatedUserReview>();
        ProcessInstance currentInst = getProcessInstanceByBizKey(protocolId);
        if (currentInst == null) return retList;
        String instanceId = currentInst.getId();
        // only pick up finished tasks from current process
        List<HistoricTaskInstance> htList = iacucProcessQueryService.getHistoricDesignatedUserReviewTasksByBizKeyAndProcessId(protocolId, instanceId);
        if (htList == null) return retList;
        // temporary hold because if there were undo task involved, it could be have multiple tasks performed by the same assignee 
        Map<String, IacucDesignatedUserReview> tmp = new HashMap<String, IacucDesignatedUserReview>();
        Date cancelledTime = null;
        for (HistoricTaskInstance ht : htList) {
            Map<String, Object> so = ht.getProcessVariables();
            // filter out cancelled task 
            if (so.get(TASK_CANCELLED_BY + ht.getId()) != null) {
                cancelledTime = ht.getEndTime();
                continue;
            }
            // filter out any tasks performed before that cancelled time
            if (cancelledTime != null) {
                if (ht.getEndTime().before(cancelledTime)) continue;
            }
            // get vote
            String key = ht.getAssignee() + "_" + ht.getId();
            Object obj = so.get(key);
            if (obj == null) continue;
            if (!(obj instanceof Map<?, ?>)) continue;
            String assignee = ht.getAssignee();
            if (StringUtils.isBlank(assignee)) continue;
            // 
            @SuppressWarnings("unchecked")
            Map<String, Object> reviewMap = (Map<String, Object>) obj;
            IacucDesignatedUserReview review = new IacucDesignatedUserReview();
            review.mapToFields(reviewMap);
            IacucDesignatedUserReview fromTmp = tmp.get(assignee);
            if (fromTmp == null) {
                tmp.put(assignee, review);
                continue;
            }
            Date fromTmpActionDate = fromTmp.getActionDate();
            if (fromTmpActionDate == null) continue;
            Date reviewActionDate = review.getActionDate();
            if (reviewActionDate == null) continue;
            // 
            if (fromTmpActionDate.before(reviewActionDate)) {
                // keep most recent and overwrite previous one
                tmp.put(assignee, review);
            }
        }

        retList.addAll(tmp.values());
        return retList;
    }

    boolean canAdminRedistribute(String protocolId) {
        // actual #
        List<IacucDesignatedUserReview> reviewList = getCurrentReviewersVotingStatus(protocolId);
        return (reviewList == null || reviewList.isEmpty());
    }

    boolean getPreApprovalConditionFromVotingStatus(String protocolId) {
        // supposed #
        List<String> rvList = mostRecentReviewerList(protocolId);
        // actual #
        List<IacucDesignatedUserReview> reviewList = getCurrentReviewersVotingStatus(protocolId);
        if (reviewList == null || reviewList.isEmpty()) {
            log.info("no reviewer action");
            return false;
        }
        if (reviewList.size() < rvList.size()) return false;
        //
        int approvalCount = 0;
        for (IacucDesignatedUserReview review : reviewList) {
            String vote = review.getAction();
            if (IacucApprovalType.HOLD.isType(vote)) {
                return false;
            } else if (IacucApprovalType.FULLCOMMITTEE.isType(vote)) {
                return false;
            } else if (IacucApprovalType.APPROVE.isType(vote)) {
                approvalCount += 1;
            }
        }
        return rvList.size() == approvalCount;
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


    List<IacucProcessHistoricData> getIacucProtocolHistory(String protocolId) {
        List<HistoricTaskInstance> htiList = iacucProcessQueryService.getHistoricTaskInstanceByBizKey(protocolId);
        return getIacucHistoricData(htiList, protocolId);
    }

    List<IacucProcessHistoricData> getIacucAdverseHistory(final String adverseEventId) {
        final String adverseEvtBizKey = adverseEventBizKey(adverseEventId);
        List<HistoricTaskInstance> htiList = iacucProcessQueryService.getHistoricTaskInstanceByBizKey(adverseEvtBizKey);
        return getAdverseEventHistoryData(htiList, adverseEventId);
    }

    private List<IacucProcessHistoricData> getAdverseEventHistoryData(List<HistoricTaskInstance> htiList, String adverseEventId) {
        List<IacucProcessHistoricData> list = new ArrayList<IacucProcessHistoricData>();
        for (HistoricTaskInstance in : htiList) {
            String taskDefKey = in.getTaskDefinitionKey();
            IacucProcessHistoricData adverseEvtHistory = IacucProcessHistoricData.create(in.getProcessInstanceId(), adverseEventId, in.getId(), false);
            adverseEvtHistory.setAdverseEventId(adverseEventId);
            adverseEvtHistory.setTaskDefKey(taskDefKey);
            Map<String, Object> procMap = in.getProcessVariables();
            if (procMap.get(ADVERSE_EVENT_APPROVED) != null) {
                adverseEvtHistory.setApprovalStatus((Boolean) procMap.get(ADVERSE_EVENT_APPROVED));
            }
            //
            adverseEvtHistory.setName(in.getName());
            if (IacucStatus.ADVERSEEVENT.isDefKey(taskDefKey)) {
                if (in.getEndTime() != null) {
                    if (adverseEvtHistory.getApprovalStatus()) {
                        adverseEvtHistory.setName("Approve");
                    } else {
                        adverseEvtHistory.setName("Return to PI");
                    }
                    fillSnapshotId(adverseEvtHistory, in.getId(), true);
                } else {
                    adverseEvtHistory.setName("Wait for approval");
                }
            }

            adverseEvtHistory.setStartTime(in.getStartTime());
            adverseEvtHistory.setEndTime(in.getEndTime());

            fillAdminNote(adverseEvtHistory, in.getId());

            // get correspondence info
            Map<String, IacucCorrespondence> fetchedMap = getCorrespondenceMap(procMap, in);
            if (fetchedMap != null) {
                List<IacucCorrespondence> hiList = new ArrayList<IacucCorrespondence>();
                hiList.addAll(fetchedMap.values());
                adverseEvtHistory.setCorrespondence(hiList);
            }

            if (in.getAssignee() != null) {
                adverseEvtHistory.setCreatedBy(in.getAssignee());
            }
            list.add(adverseEvtHistory);
            // add one more for submit status because submission doesn't have a task
            Object startedByObj = procMap.get(STARTED_BY);
            if (IacucStatus.ADVERSEEVENT.isDefKey(taskDefKey)) {
                IacucProcessHistoricData one = IacucProcessHistoricData.create(in.getProcessInstanceId(), adverseEventId, in.getId(), false);
                one.setName("Submit");
                one.setStartTime(in.getStartTime());
                one.setEndTime(in.getStartTime());
                fillSnapshotId(one, in.getProcessInstanceId(), false);
                one.setCreatedBy(startedByObj != null ? startedByObj.toString() : "");
                list.add(one);
            }
        }

        return list;
    }

    List<IacucProcessHistoricData> getAllTaskStatusFromCurrentProcess(String protocolId) {
        List<IacucProcessHistoricData> list = new ArrayList<IacucProcessHistoricData>();
        ProcessInstance instance = getProcessInstanceByBizKey(protocolId);
        if (instance == null) return list;
        List<HistoricTaskInstance> htiList = iacucProcessQueryService.getHistoricTaskInstanceByBizKeyAndProcId(protocolId, instance.getId());
        return getIacucHistoricData(htiList, protocolId);
    }

    String getNotesFromCurrentProcess(String protocolId) {
        ProcessInstance instance = getProcessInstanceByBizKey(protocolId);
        if (instance == null) return null;
        List<HistoricTaskInstance> htiList = iacucProcessQueryService.getHistoricTaskInstanceByBizKeyAndProcId(protocolId, instance.getId());
        List<String> list = getNotesFromCurrentProcess(htiList);
        if (list.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (String note : list) {
            sb.append(note).append("\n\n");
        }
        return sb.toString();
    }

    //
    private List<String> getNotesFromCurrentProcess(List<HistoricTaskInstance> htiList) {
        List<String> noteList = new ArrayList<String>();
        for (HistoricTaskInstance hsTaskInstance : htiList) {
            String taskId = hsTaskInstance.getId();
            List<Comment> listComments = taskService.getTaskComments(taskId, ADMIN_NOTE);
            if (listComments == null) return noteList;
            if (!listComments.isEmpty()) {
                for (Comment c : listComments) {
                    String message = c.getFullMessage();
                    if (!StringUtils.isBlank(message)) {
                        noteList.add(c.getTime() + "\n" + message);
                    }
                }
            }
        }
        return noteList;
    }


    // map: processVariables
    private Map<String, IacucCorrespondence> getCorrespondenceMap(Map<String, Object> map, HistoricTaskInstance hs) {
        String lookupKey = IACUC_COORESPONDENCE + hs.getId();
        Object obj = map.get(lookupKey);
        if (obj != null && obj instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> objectMap = (Map<String, Object>) obj;
            Map<String, IacucCorrespondence> fetchedMap = new TreeMap<String, IacucCorrespondence>();
            for (String key : objectMap.keySet()) {
                Object o = objectMap.get(key);
                if (o != null && o instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> so = (Map<String, Object>) o;
                    if (so.isEmpty()) continue;
                    IacucCorrespondence corr = new IacucCorrespondence();
                    if (corr.mapToFields(so)) {
                        fetchedMap.put(corr.getId(), corr);
                    } else {
                        log.error("error in fetch correspondence");
                    }
                }
            }
            return fetchedMap;
        }
        return null;
    }

    private void fillMeetingDate(final IacucProcessHistoricData history, final Map<String, Object> taskMap, final String taskId) {
        String meetingDateKey = MEETING_DATE + taskId;
        if (taskMap.get(meetingDateKey) != null) {
            Object o = taskMap.get(meetingDateKey);
            if (o instanceof Date) {
                history.setMeetingDate((Date) o);
            }
        }
    }


    String getAdminNoteById(String adminNoteId) {
        Comment comment = taskService.getComment(adminNoteId);
        return comment == null ? null : comment.getFullMessage();
    }


    public String completeMailOnlyTask(String protocolId, IacucCorrespondence correspondence) {
        String userId = correspondence.getFrom();
        /* let it go
        if (isProcessStarted(protocolId)) {
            log.warn("Process was already started for protocolId=" + protocolId);
            return null;
        }
        */
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.EMAILONLY.startGatewayValue());

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY, protocolId, processInput);
        //log.info("protocolId=" + protocolId + ",activityId=" + instance.getActivityId() + ",processId=" + instance.getId());

        //
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.EMAILONLY.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.EMAILONLY.taskDefKey() + " for protocolId=" + protocolId);
            return null;
        }
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        attachCorrespondence(task, correspondence);
        String taskId = task.getId();
        taskService.claim(taskId, userId);
        taskService.complete(taskId);
        return taskId;
    }

    String saveAdverseEventCorrespondence(final String adverseEventId, final IacucCorrespondence corr) {
        final String adverseEvtBizKey = adverseEventBizKey(adverseEventId);
        return saveCorrespondence(adverseEvtBizKey, corr);
    }

    @Transactional
    String saveCorrespondence(String bizKey, IacucCorrespondence corr) {
        if (corr == null) {
            log.error("undefined correspondence, protocolId=" + bizKey);
            return null;
        }
        if (StringUtils.isBlank(corr.getFrom())) {
            log.error("undefined user, protocolId=" + bizKey);
        }
        // call this before save and don't call this in anywhere else!!!
        corr.apply();
        if (!corr.isValid()) {
            log.error("invalid corr=" + corr + ",bizKey=" + bizKey + ",userId=" + corr.getFrom());
            return null;
        }

        String taskDefKey = getCurrentTaskDefKey(bizKey);
        if (taskDefKey == null) {
            // log.info("No processInstance for bizKey=" + bizKey);
            return completeMailOnlyTask(bizKey, corr);
        }
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(bizKey, taskDefKey);
        if (task == null) {
            log.error("No task=" + taskDefKey + " for bizKey=" + bizKey);
            return null;
        }
        String taskId=task.getId();
        attachCorrespondence(task, corr);
        return taskId;
    }

    private void attachCorrespondence(Task task, IacucCorrespondence corr) {
        Map<String, Map<String, Object>> corrMap = new TreeMap<String, Map<String, Object>>();
        Map<String, Object> fieldsToMap = corr.fieldToMap();
        String key = IACUC_COORESPONDENCE + task.getId();
        Map<String, Object> procMap = task.getProcessVariables();
        if (procMap != null) {
            Object obj = procMap.get(key);
            if (obj != null && obj instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> savedMap = (Map<String, Map<String, Object>>) obj;
                corrMap.putAll(savedMap);
            }
        }
        corrMap.put(corr.getId(), fieldsToMap);
        taskService.setVariable(task.getId(), key, corrMap);
    }

    Map<String, IacucCorrespondence> getCorrespondence(String protocolId) {
        Map<String, IacucCorrespondence> retMap = new TreeMap<String, IacucCorrespondence>();
        List<HistoricTaskInstance> hsList = iacucProcessQueryService.getHistoricTaskInstanceByBizKey(protocolId);
        for (HistoricTaskInstance hs : hsList) {
            Map<String, Object> map = hs.getProcessVariables();
            Map<String, IacucCorrespondence> fetchedMap = getCorrespondenceMap(map, hs);
            if (fetchedMap != null) {
                retMap.putAll(fetchedMap);
            }
        }
        return retMap;
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

    IacucCorrespondence getIacucCorrespondenceById(String protocolId, String id) {
        Map<String, IacucCorrespondence> map = getCorrespondence(protocolId);
        return map.get(id);
    }


    IacucProcessHistoricData getPreviousApprovedData(String protocolId) {
        HistoricTaskInstance hs = iacucProcessQueryService.getHistoricApprovalTaskInstance(protocolId);

        if (hs == null) {
            log.warn("couldn't get hs for protcolId=" + protocolId);
            return null;
        }
        IacucProcessHistoricData history = IacucProcessHistoricData.create(hs.getProcessInstanceId(), protocolId, hs.getId(), true);

        history.setTaskDefKey(IacucStatus.FINALAPPROVAL.taskDefKey());
        history.setName(hs.getName());
        history.setDescription(hs.getDescription());
        history.setStartTime(hs.getStartTime());
        history.setEndTime(hs.getEndTime());
        history.setCreatedBy(hs.getAssignee());

        fillAdminNote(history, hs.getId());

        fillSnapshotId(history, hs.getId(), true);

        // get correspondence info
        Map<String, Object> procMap = hs.getProcessVariables();
        Map<String, IacucCorrespondence> corrMap = getCorrespondenceMap(procMap, hs);
        if (corrMap != null) {
            fillCorrespondenceList(history, corrMap);
        }

        return history;
    }

    private void fillSnapshotId(final IacucProcessHistoricData history, final String id, final boolean isTaskId) {
        List<Attachment> attachmentList;
        if (isTaskId) {
            attachmentList = taskService.getTaskAttachments(id);
        } else {
            attachmentList = taskService.getProcessInstanceAttachments(id);
        }

        if (attachmentList != null && !attachmentList.isEmpty()) {
            history.setSnapshotId(attachmentList.get(0).getId());
        }
    }

    private void fillCorrespondenceList(final IacucProcessHistoricData history, final Map<String, IacucCorrespondence> map) {
        if (map != null) {
            List<IacucCorrespondence> hiList = new ArrayList<IacucCorrespondence>();
            hiList.addAll(map.values());
            history.setCorrespondence(hiList);
        }
    }

    private void fillAdminNote(final IacucProcessHistoricData history, final String taskId) {
        List<Comment> listComments = taskService.getTaskComments(taskId, ADMIN_NOTE);
        if (listComments == null) return;
        if (!listComments.isEmpty()) {
            Comment c = listComments.get(0);
            history.setAdminNoteId(c.getId());
            history.setAdminNote(c.getFullMessage());
        }
    }

    IacucProcessHistoricData getHistoryByProtocolIdTaskId(String protocolId, String taskId) {
        HistoricTaskInstance hs = iacucProcessQueryService.getHistoricTaskInstanceByBizKeyAndTaskId(protocolId, taskId);
        if (hs == null) {
            log.error("no HistoricTaskInstance for protcolId=" + protocolId + ", taskId=" + taskId);
            return null;
        }
        String taskDefKey = hs.getTaskDefinitionKey();
        IacucProcessHistoricData history =
                IacucProcessHistoricData.create(hs.getProcessInstanceId(), protocolId, hs.getId(), true);
        history.setTaskDefKey(taskDefKey);
        history.setName(hs.getName());
        history.setDescription(hs.getDescription());
        history.setStartTime(hs.getStartTime());
        history.setEndTime(hs.getEndTime());
        String assignee = hs.getAssignee();
        history.setCreatedBy(assignee);
        fillAdminNote(history, hs.getId());
        //
        if (IacucStatus.SUBMIT.isDefKey(taskDefKey)) {
            fillSnapshotId(history, hs.getProcessInstanceId(), false);
            history.setName(IacucStatus.SUBMIT.statusName());
        } else {
            fillSnapshotId(history, hs.getId(), true);
        }
        // get correspondence info
        Map<String, Object> procMap = hs.getProcessVariables();
        Map<String, IacucCorrespondence> fetchedMap = getCorrespondenceMap(procMap, hs);
        if (fetchedMap != null) {
            fillCorrespondenceList(history, fetchedMap);
        }
        //
        if (procMap.get(STARTED_BY).toString() != null) {
            history.setPiUni(procMap.get(STARTED_BY).toString());
        }
        //
        if (IacucStatus.ASSIGNEEREVIEW.isDefKey(taskDefKey)) {
            Object assigneeObj = procMap.get(assignee + "_" + hs.getId());
            if (assigneeObj != null && assigneeObj instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> read = (Map<String, Object>) assigneeObj;
                IacucDesignatedUserReview review = new IacucDesignatedUserReview();
                review.mapToFields(read);
                history.setDesignatedUserReview(review);
            }
        }
        //
        if (IacucStatus.DISTRIBUTE.isDefKey(taskDefKey)) {
            fillMeetingDate(history, procMap, hs.getId());
        }

        return history;
    }

    IacucProcessHistoricData getHistoryByTaskIdForPdfComparison(String taskId) {
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
        IacucProcessHistoricData history = IacucProcessHistoricData.create(hs.getProcessInstanceId(), protocolId, hs.getId(), true);
        history.setTaskDefKey(taskDefKey);

        if (IacucStatus.SUBMIT.isDefKey(taskDefKey)) {
            fillSnapshotId(history, hs.getProcessInstanceId(), false);
        } else {
            fillSnapshotId(history, hs.getId(), true);
        }

        return history;
    }


    @Transactional
    boolean deleteProcessInstanceByProtocolId(String protocolId, String deleteReason) {
        ProcessInstance instance = getProcessInstanceByBizKey(protocolId);
        if (instance == null) {
            log.error("no process defined by bizKey=" + protocolId);
            return false;
        }
        runtimeService.deleteProcessInstance(instance.getId(), deleteReason);
        return true;
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
        if (isProcessStarted(protocolId)) {
            log.error("cannot terminate this protocol because it is process, protocolId=" + protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.TERMINATE.startGatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY, protocolId, processInput);
        //log.info("protocolId=" + protocolId + ",activityId=" + instance.getActivityId() + ",processId=" + instance.getId());
        return true;
    }

    @Transactional
    String completeTerminationTask(String protocolId, String userId, String adminNote) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.TERMINATE.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.TERMINATE.taskDefKey() + " for protocolId=" + protocolId);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        completeTask(task, userId);
        return taskId;
    }


    @Transactional
    boolean suspendProtocol(String protocolId, String userId) {
        if (isProcessStarted(protocolId)) {
            log.error("cannot suspend this protocol because it is process, protocolId=" + protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.SUSPEND.startGatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY, protocolId, processInput);
        //log.info("protocolId=" + protocolId + ",activityId=" + instance.getActivityId() + ",processId=" + instance.getId());
        return true;
    }

    @Transactional
    String completeSuspensionTask(String protocolId, String userId, String adminNote) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.SUSPEND.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.SUSPEND.taskDefKey() + " for protocolId=" + protocolId);
            return null;
        }
        final String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        completeTask(task, userId);
        return taskId;
    }

    @Transactional
    boolean reinstateProtocol(String protocolId, String userId) {
        if (isProcessStarted(protocolId)) {
            log.error("cannot reinstate this protocol because it is in process, protocolId=" + protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.REINSTATE.startGatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY, protocolId, processInput);
        //log.info("protocolId=" + protocolId + ",activityId=" + instance.getActivityId() + ",processId=" + instance.getId());
        return true;
    }

    @Transactional
    String completeReinstatementTask(String protocolId, String userId, String adminNote) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.REINSTATE.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.REINSTATE.taskDefKey() + " for protocolId=" + protocolId);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        if (!StringUtils.isBlank(adminNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, adminNote);
        }
        completeTask(task, userId);
        return taskId;
    }

    @Transactional
    boolean withdrawProtocol(String protocolId, String userId) {
        if (isProcessStarted(protocolId)) {
            log.error("cannot withdraw this protocol because it is process, protocolId=" + protocolId);
            return false;
        }
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.WITHDRAW.startGatewayValue());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY, protocolId, processInput);
        //log.info("protocolId=" + protocolId + ",activityId=" + instance.getActivityId() + ",processId=" + instance.getId());
        return true;
    }

    @Transactional
    String completeWithdrawalTask(String protocolId, String userId) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.WITHDRAW.taskDefKey());
        if (task == null) {
            log.error("no task=" + IacucStatus.WITHDRAW.taskDefKey() + " for protocolId=" + protocolId+",userId="+userId);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        completeTask(task, userId);
        return taskId;
    }

    @Transactional
    String importKaputStatus(String protocolId, String userId, String kaputName) {
        /*
        if (isProcessStarted(protocolId)) {
            log.error("cannot store kaput status: protocolId="+protocolId+",userId="+userId+",kaputName="+kaputName);
            return null;
        }
        */
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put(PROTOCOL_ID, protocolId);
        processInput.put(STARTED_BY, userId);
        processInput.put(START_GATEWAY, IacucStatus.KAPUT.startGatewayValue());
        processInput.put("KAPUT_NAME", kaputName);
        try {
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY, protocolId, processInput);
            if(instance!=null) {
                return instance.getProcessInstanceId();
            }else {
                return null;
            }
            //log.info("protocolId=" + protocolId + ",activityId=" + instance.getActivityId() + ",processId=" + instance.getId());
        }catch (Exception e) {
            log.error("protocolId="+protocolId+",userId="+userId+",kaputName="+kaputName, e);
            return null;
        }
    }

    @Transactional
    String completeKaputTask(String protocolId, String userId, String kaputNote) {
        Task task = iacucProcessQueryService.getTaskByBizKeyAndTaskDefKey(protocolId, IacucStatus.KAPUT.taskDefKey());
        if (task == null) {
            log.error("failed to completeKaputTask for protocolId=" + protocolId+",userId="+userId);
            return null;
        }
        String taskId=task.getId();
        task.setOwner(IACUC_ADMIN);
        task.setAssignee(userId);
        if (!StringUtils.isBlank(kaputNote)) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), ADMIN_NOTE, kaputNote);
        }
        completeTask(task, userId);
        return taskId;
    }



    private List<IacucProcessHistoricData> getIacucHistoricData(List<HistoricTaskInstance> htiList, String protocolId) {

        List<IacucProcessHistoricData> list = new ArrayList<IacucProcessHistoricData>();
        int distCount = 0;
        for (HistoricTaskInstance hsTaskInstance : htiList) {

            // typically this task was deleted by Activity when time expired
            if("deleted".equals(hsTaskInstance.getDeleteReason())) {
                log.info("deleted by activity at "+hsTaskInstance.getEndTime());
                continue;
            }

            String taskDefKey = hsTaskInstance.getTaskDefinitionKey();
            String assignee = hsTaskInstance.getAssignee();
            Map<String, Object> procMap = hsTaskInstance.getProcessVariables();

            IacucProcessHistoricData history = IacucProcessHistoricData.create(hsTaskInstance.getProcessInstanceId(), protocolId, hsTaskInstance.getId(), true);
            history.setTaskDefKey(taskDefKey);
            history.setCreatedBy(assignee);
            // PI
            Object startedByObj = procMap.get(STARTED_BY);
            if (startedByObj != null) {
                history.setPiUni(startedByObj.toString());
            }
            history.setName(hsTaskInstance.getName());
            if (IacucStatus.SUBMIT.isDefKey(taskDefKey)) {
                history.setCreatedBy(history.getPiUni());
                history.setName(IacucStatus.SUBMIT.statusName());
                fillSnapshotId(history, hsTaskInstance.getProcessInstanceId(), false);
            } else {
                fillSnapshotId(history, hsTaskInstance.getId(), true);
            }

            if (IacucStatus.DISTRIBUTE.isDefKey(taskDefKey)) {
                distCount += 1;
                history.setDistributionCount(distCount);
                Object reviewerListObj = procMap.get(REVIEWER_LIST);
                if (reviewerListObj != null && reviewerListObj instanceof List<?>) {
                    List<String> reviewers = new ArrayList<String>();
                    List<?> foo = (List<?>) reviewerListObj;
                    for (Object o : foo) {
                        reviewers.add(o.toString());
                    }
                    history.setReviewerList(reviewers);
                }
                fillMeetingDate(history, procMap, hsTaskInstance.getId());
                if (procMap.get(REVIEW_TYPE) != null) {
                    history.setReviewType(procMap.get(REVIEW_TYPE).toString());
                }
            }

            history.setDescription(hsTaskInstance.getDescription());
            history.setStartTime(hsTaskInstance.getStartTime());
            history.setEndTime(hsTaskInstance.getEndTime());

            fillAdminNote(history, hsTaskInstance.getId());

            // get designated review info if any
            if (IacucStatus.ASSIGNEEREVIEW.isDefKey(taskDefKey)) {
                String assigneeDataKey = assignee + "_" + hsTaskInstance.getId();
                Object designatedUserReviewObj = procMap.get(assigneeDataKey);
                if (designatedUserReviewObj != null && designatedUserReviewObj instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> detail = (Map<String, Object>) designatedUserReviewObj;
                    IacucDesignatedUserReview review = new IacucDesignatedUserReview();
                    review.mapToFields(detail);
                    // show piece
                    //String name = "Designated Reviewer Action " + detail.getAction();
                    //history.setName(name);
                    history.setDesignatedUserReview(review);
                }
                // if it was cancelled by administrator
                String cancelTaskInfoKey = TASK_CANCELLED_BY + hsTaskInstance.getId();
                if (procMap.get(cancelTaskInfoKey) != null) {
                    String msg = procMap.get(cancelTaskInfoKey).toString();
                    history.setName(msg);
                    history.setTaskCancelledBy(msg);
                }
            }

            // get correspondence info
            Map<String, IacucCorrespondence> corrMap = getCorrespondenceMap(procMap, hsTaskInstance);
            if (corrMap != null) {
                List<IacucCorrespondence> hiList = new ArrayList<IacucCorrespondence>();
                hiList.addAll(corrMap.values());
                history.setCorrespondence(hiList);
            }

            list.add(history);
        }

        return list;
    }

    Set<String> getBizKeysWithoutDesignatedReviewerTask() {
        Set<String> set = new TreeSet<String>();
        List<Task> taskList;
        try {
            taskList = iacucProcessQueryService.getAllTasks();
        } catch (Exception e) {
            // typically caught: "Couldn't deserialize object in variable 'rscl1099'
            log.error("caught error:", e);
            return set;
        }

        if (taskList == null) return set;
        for (Task task : taskList) {
            if (IacucStatus.ASSIGNEEREVIEW.isDefKey(task.getTaskDefinitionKey())) {
                continue;
            }
            Map<String, Object> map = task.getProcessVariables();
            if (map.get(PROTOCOL_ID) != null) {
                set.add(map.get(PROTOCOL_ID).toString());
            }
        }
        return set;
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


    Map<String, Set<String>> getProtocolIdAndReviewerListFromCurrentProcess() {
        Map<String, Set<String>> map = new TreeMap<String, Set<String>>();
        List<Task> taskList = iacucProcessQueryService.getDesignatedReviewerTasks();
        for (Task task : taskList) {
            Map<String, Object> varmap = task.getProcessVariables();
            // never be true, just sanity check
            if (varmap.get(PROTOCOL_ID) == null) {
                log.error("cannot get protocol header OID");
                continue;
            }
            String protocolId = varmap.get(PROTOCOL_ID).toString();
            String assignee = task.getAssignee();
            // never be true, just sanity check
            if (StringUtils.isBlank(assignee)) {
                log.error("cannot get assignee");
                continue;
            }
            if (map.containsKey(protocolId)) {
                Set<String> tset = map.get(protocolId);
                tset.add(assignee);
                map.put(protocolId, tset);
            } else {
                TreeSet<String> tset = new TreeSet<String>();
                tset.add(assignee);
                map.put(protocolId, tset);
            }
        }
        return map;
    }
}
