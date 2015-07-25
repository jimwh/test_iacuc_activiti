package iacuc;

import edu.columbia.rascal.business.service.IacucProtocolHeaderService;
import edu.columbia.rascal.business.service.review.iacuc.*;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.engine.test.Deployment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:activiti/springUsageTest-context.xml")
public class MyBusinessProcessTest {

    static final String ProtocolReviewProcDefKey = "IacucApprovalProcess";
    static final String AevtRptReviewProcDefKey = "IacucAdverseEvent";
    private static final Logger log = LoggerFactory.getLogger(MyBusinessProcessTest.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private IacucProtocolHeaderService headerService;

    @Test
    // @Deployment(resources = {"org/activiti/test/IacucApprovalProcess.bpmn20.xml"})
    @Deployment(resources = {"IacucApprovalProcess.bpmn20.xml", "IacucAdverseEvent.bpmn20.xml"})
    public void test() {
        log.info("testing ...");
        testRedistribute();
    }

    private boolean isSuspended(String headerId) {
        return headerService.isSuspended(headerId);
    }
    public void testRedistribute() {
        String bizKey = "fooRedistributeKey";
        String userId = "Robert";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("START_GATEWAY", IacucStatus.Submit.gatewayValue());
        map.put("hasAppendixA", true);
        map.put("expediteReview", false);
        startProcess(bizKey, userId, map, IacucStatus.Submit.name());
        // after starting, it has 5 tasks:
        // returnToPI, subcommittee, DS, SO(pre-approve, hold)
        assertEquals(5, taskCount(bizKey));
        log.info("isSuspended={}", isSuspended(bizKey));
        //
        List<String> rvList = new ArrayList<String>();
        rvList.add("Sam");
        distributeToDesignatedReviewer(bizKey, "admin", rvList);
        // after distributing, it has 7 tasks:
        // returnToPI, redistribute, rv1(approval, hold, req-full-review), SO(pre-approve, hold)
        assertEquals(7, taskCount(bizKey));
        //
        // completeAction(bizKey, "Sam", IacucStatus.Rv1Approval.taskDefKey(), IacucStatus.Rv1Approval.statusName(), "Sam's comment");
        completeAction(bizKey, "SafetyOfficer", IacucStatus.SOPreApproveA.taskDefKey(), IacucStatus.SOPreApproveA.statusName(), "Safety Officer's comment");
        // after safety office completed,
        // it has 5 tasks: returnToPI, redistribute, rv1(approval, hold, req-full-review)
        assertEquals(5, taskCount(bizKey));

        if (canDistribute(bizKey)) {
            completeAction(bizKey, "admin", IacucStatus.Redistribute.taskDefKey(), IacucStatus.Redistribute.statusName(), "admin redistribute protocol");
            // after redistributing: return2pi, Subcommittee, DS
            assertEquals(3, taskCount(bizKey));
            printOpenTasks(bizKey);
        }
    }


    public void testExpediteReview() {
        String bizKey = "foo1";
        String userId = "bob";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("START_GATEWAY", IacucStatus.Submit.gatewayValue());
        map.put("hasAppendixA", true);
        map.put("expediteReview", true);
        startProcess(bizKey, userId, map, ProtocolReviewProcDefKey);
        //
        assertEquals(6, taskCount(bizKey));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.DistributeSubcommittee.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.DistributeReviewer.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.ExpediteReview.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.ReturnToPI.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.SOPreApproveA.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.SOHoldA.taskDefKey()));
        //
        completeExpediteAction(bizKey, "Rom", IacucStatus.ExpediteReview.taskDefKey(),
                IacucStatus.ExpediteReview.statusName(), "Expediter Rom's comment");
        assertEquals(3, taskCount(bizKey));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.SOPreApproveA.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.SOHoldA.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.ReturnToPI.taskDefKey()));
        //
        completeAction(bizKey, "SafetyOfficer", IacucStatus.SOPreApproveA.taskDefKey(), IacucStatus.SOPreApproveA.statusName(), "Safety Officer's comment");
        assertEquals(2, taskCount(bizKey));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.FinalApproval.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.ReturnToPI.taskDefKey()));
        //printOpenTasks(bizKey);
        //
        completeAction(bizKey, "admin", IacucStatus.FinalApproval.taskDefKey(), IacucStatus.FinalApproval.statusName(), "admin comment");
        assertEquals(2, taskCount(bizKey));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.UndoApproval.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.ReturnToPI.taskDefKey()));
        //
        ProcessInstance instance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(bizKey)
                .singleResult();
        // fire timer
        Job timer = managementService
                .createJobQuery()
                .processInstanceId(instance.getId())
                .singleResult();
        managementService.executeJob(timer.getId());
        assertEquals(0, taskCount(bizKey));

        printHistory(bizKey);
    }

    public void removeUndoApproval() {
        String bizKey = "foo1";
        String userId = "bob";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("START_GATEWAY", IacucStatus.Submit.gatewayValue());
        startProcess(bizKey, userId, map, ProtocolReviewProcDefKey);
        //
        List<String> rvList = new ArrayList<String>();
        rvList.add("Sam");
        distributeToDesignatedReviewer(bizKey, "admin", rvList);
        //
        completeAction(bizKey, "Sam", IacucStatus.Rv1Approval.taskDefKey(), IacucStatus.Rv1Approval.statusName(), "Sam's comment");
        completeAction(bizKey, "admin", IacucStatus.FinalApproval.taskDefKey(), IacucStatus.FinalApproval.statusName(), "admin comment");

        // after these two actions, it must have only two tasks: final approval and return to PI
        assertEquals(2, taskCount(bizKey));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.UndoApproval.taskDefKey()));

        ProcessInstance instance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(bizKey)
                .singleResult();

        // fire timer
        Job timer = managementService
                .createJobQuery()
                .processInstanceId(instance.getId())
                .singleResult();
        managementService.executeJob(timer.getId());
        assertEquals(0, taskCount(bizKey));

        printHistory(bizKey);
    }

    public void testSafetyOfficerApprovalFirstThenRvApproval() {
        String bizKey = "foo1";
        String userId = "bob";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("START_GATEWAY", IacucStatus.Submit.gatewayValue());
        map.put("hasAppendixB", true);
        startProcess(bizKey, userId, map, ProtocolReviewProcDefKey);
        //
        List<String> rvList = new ArrayList<String>();
        rvList.add("Sam");
        distributeToDesignatedReviewer(bizKey, "admin", rvList);
        //
        completeAction(bizKey, "Rom", IacucStatus.SOPreApproveB.taskDefKey(), IacucStatus.SOPreApproveB.statusName(), "Rom's comment");
        completeAction(bizKey, "Sam", IacucStatus.Rv1Approval.taskDefKey(), IacucStatus.Rv1Approval.statusName(), "Sam's comment");
        // after these two actions, it must have only two tasks: final approval and return to PI
        assertEquals(2, taskCount(bizKey));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.FinalApproval.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.ReturnToPI.taskDefKey()));
    }

    public void testRvApprovalFirstThenSafeOfficerApproval() {
        String bizKey = "foo2";
        String userId = "Robert";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("START_GATEWAY", IacucStatus.Submit.gatewayValue());
        map.put("hasAppendixB", true);
        startProcess(bizKey, userId, map, ProtocolReviewProcDefKey);
        //
        List<String> rvList = new ArrayList<String>();
        rvList.add("Sam");
        distributeToDesignatedReviewer(bizKey, "admin", rvList);

        completeAction(bizKey, "Sam", IacucStatus.Rv1Approval.taskDefKey(), IacucStatus.Rv1Approval.statusName(), "Sam's comment");
        completeAction(bizKey, "Rom", IacucStatus.SOPreApproveB.taskDefKey(), IacucStatus.SOPreApproveB.statusName(), "Rom's comment");
        // after these two actions, it must have only two tasks: final approval and return to PI
        assertEquals(2, taskCount(bizKey));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.FinalApproval.taskDefKey()));
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.ReturnToPI.taskDefKey()));
    }

    public void testRvApprovalFirstThenSafeOfficerHold() {
        String bizKey = "foo3";
        String userId = "Tom";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("START_GATEWAY", IacucStatus.Submit.gatewayValue());
        map.put("hasAppendixB", true);
        startProcess(bizKey, userId, map, ProtocolReviewProcDefKey);

        List<String> rvList = new ArrayList<String>();
        rvList.add("Sam");
        distributeToDesignatedReviewer(bizKey, "admin", rvList);

        completeAction(bizKey, "Sam", IacucStatus.Rv1Approval.taskDefKey(), IacucStatus.Rv1Approval.statusName(), "Sam's comment");
        completeAction(bizKey, "Rom", IacucStatus.SOHoldB.taskDefKey(), IacucStatus.SOHoldB.statusName(), "Rom HoldB's comment");

        // after above actions, it must have only one task - return to PI
        assertEquals(1, taskCount(bizKey));
        // no approval task
        assertNull(getTaskByTaskDefKey(bizKey, IacucStatus.FinalApproval.taskDefKey()));
        // has return to PI task
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.ReturnToPI.taskDefKey()));

    }

    public void testRvHoldFirstSafeOfficerHold() {
        String bizKey = "foo4";
        String userId = "Ellen";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("START_GATEWAY", IacucStatus.Submit.gatewayValue());
        map.put("hasAppendixB", true);
        startProcess(bizKey, userId, map, ProtocolReviewProcDefKey);

        List<String> rvList = new ArrayList<String>();
        rvList.add("Bob");
        distributeToDesignatedReviewer(bizKey, "admin", rvList);

        completeAction(bizKey, "Bob", IacucStatus.Rv1Hold.taskDefKey(), IacucStatus.Rv1Hold.statusName(), "Bob's comment");
        completeAction(bizKey, "Rom", IacucStatus.SOHoldB.taskDefKey(), IacucStatus.SOHoldB.statusName(), "Rom HoldB's comment");

        // after above actions, it must have only one task - return to PI
        assertEquals(1, taskCount(bizKey));
        // no approval task
        assertNull(getTaskByTaskDefKey(bizKey, IacucStatus.FinalApproval.taskDefKey()));
        // has return to PI task
        assertNotNull(getTaskByTaskDefKey(bizKey, IacucStatus.ReturnToPI.taskDefKey()));

    }

    public void testUndoApprovalReturnToPI() {

        String bizKey = "foo5";
        String userId = "bob";
        startProtocolProcess(bizKey, userId);

        List<String> rvList = new ArrayList<String>();
        rvList.add("Sam");
        rvList.add("Dave");
        distributeToDesignatedReviewer(bizKey, "admin", rvList);

        completeAction(bizKey, "Sam", IacucStatus.Rv1Approval.taskDefKey(), IacucStatus.Rv1Approval.statusName(), "Sam's comment");
        completeAction(bizKey, "Dave", IacucStatus.Rv2Approval.taskDefKey(), IacucStatus.Rv2Approval.statusName(), "Dave's comment");

        if (canDistribute(bizKey)) {
            completeAction(bizKey, "admin", IacucStatus.Redistribute.taskDefKey(), IacucStatus.Redistribute.statusName(), "admin redistribute protocol");
        } else {
            completeAction(bizKey, "admin", IacucStatus.FinalApproval.taskDefKey(), IacucStatus.FinalApproval.statusName(), "final approval");
            completeAction(bizKey, "admin", IacucStatus.UndoApproval.taskDefKey(), IacucStatus.UndoApproval.statusName(), "undo approval");
            completeAction(bizKey, "admin", IacucStatus.ReturnToPI.taskDefKey(), IacucStatus.ReturnToPI.statusName(), "return-2-pi");
            // after above action, it has no more tasks
            assertEquals(0, taskCount(bizKey));

            // only the process ended then you can get the start user id, otherwise null exception
            HistoricProcessInstance hp = historyService
                    .createHistoricProcessInstanceQuery()
                    .processInstanceBusinessKey(bizKey)
                    .finished()
                    .singleResult();
            assertEquals(userId, hp.getStartUserId());
        }
    }

    void completeAction(String bizKey, String userId, String taskDefKey, String taskName, String comment) {
        IacucTaskForm iacucTaskForm = new IacucTaskForm();
        iacucTaskForm.setBizKey(bizKey);
        iacucTaskForm.setAuthor(userId);
        iacucTaskForm.setComment(comment);
        iacucTaskForm.setTaskDefKey(taskDefKey);
        iacucTaskForm.setTaskName(taskName);
        completeTaskByTaskForm(iacucTaskForm);
    }

    void completeExpediteAction(String bizKey, String userId, String taskDefKey, String taskName, String comment) {
        IacucTaskForm iacucTaskForm = new IacucExpediteReviewForm();
        iacucTaskForm.setBizKey(bizKey);
        iacucTaskForm.setAuthor(userId);
        iacucTaskForm.setComment(comment);
        iacucTaskForm.setTaskDefKey(taskDefKey);
        iacucTaskForm.setTaskName(taskName);
        completeTaskByTaskForm(iacucTaskForm);
    }

    Task getAssigneeTaskByTaskDefKey(String bizKey, String defKey, String assignee) {
        return taskService.createTaskQuery()
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(defKey)
                .taskAssignee(assignee)
                .singleResult();
    }

    // protocol approval process
    void startProtocolProcess(String bizKey, String userId) {
        ProcessInstance instance = getProtocolProcessInstance(bizKey);
        Assert.assertNull("dude i am expecting null", instance);
        Map<String, Object> processMap = new HashMap<String, Object>();
        processMap.put("START_GATEWAY", IacucStatus.Submit.gatewayValue());
        ProcessInstance processInstance = startProcess(bizKey, userId, processMap, IacucStatus.Submit.name());
        assertNotNull(processInstance);
    }

    ProcessInstance startProcess(String bizKey, String userId, Map<String, Object> map, String instanceName) {
        identityService.setAuthenticatedUserId(userId);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(ProtocolReviewProcDefKey, bizKey, map);
        runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), instanceName);
        //
        IacucTaskForm form = new IacucTaskForm();
        form.setBizKey(bizKey);
        form.setAuthor(userId);
        form.setTaskDefKey(IacucStatus.Submit.taskDefKey());
        form.setTaskName(IacucStatus.Submit.statusName());
        completeTaskByTaskForm(form);
        return instance;
    }

    ProcessInstance getProtocolProcessInstance(String bizKey) {
        return getProcessInstanceByName(bizKey, IacucStatus.Submit.name());
    }

    void printProcessVar(String bizKey) {
        ProcessInstance instance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(bizKey)
                .includeProcessVariables()
                .singleResult();
        Map<String, Object> map = instance.getProcessVariables();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            log.info("key={}, value={}", e.getKey(), e.getValue());
        }
    }

    ProcessInstance getProcessInstanceByName(String bizKey, String instanceName) {
        return runtimeService
                .createProcessInstanceQuery()
                        // .processDefinitionKey(processDefKey)
                .processInstanceBusinessKey(bizKey)
                .processInstanceName(instanceName)
                .includeProcessVariables()
                .singleResult();
    }

    void distributeToDesignatedReviewer(String bizKey, String user, List<String> reviewerList) {
        IacucDistributeReviewerForm iacucTaskForm = new IacucDistributeReviewerForm();
        iacucTaskForm.setBizKey(bizKey);
        iacucTaskForm.setAuthor(user);
        iacucTaskForm.setComment("distribute to " + reviewerList);
        iacucTaskForm.setTaskName(IacucStatus.DistributeReviewer.statusName());
        iacucTaskForm.setTaskDefKey(IacucStatus.DistributeReviewer.taskDefKey());
        iacucTaskForm.setReviewerList(reviewerList);
        //
        IacucCorrespondence corr = new IacucCorrespondence();
        corr.setFrom("Freemen");
        corr.setRecipient("sam");
        corr.setCarbonCopy("cameron");
        corr.setSubject("notification of distribution");
        corr.setText("complete review asap ...");
        corr.apply();
        iacucTaskForm.setCorrespondence(corr);
        completeTaskByTaskForm(iacucTaskForm);
    }

    void completeTaskByTaskForm(IacucTaskForm iacucTaskForm) {
        assertNotNull(iacucTaskForm);
        Task task = getTaskByTaskDefKey(iacucTaskForm.getBizKey(), iacucTaskForm.getTaskDefKey());
        assertNotNull(task);

        String taskId = task.getId();
        if (task.getAssignee() == null) {
            taskService.claim(taskId, iacucTaskForm.getAuthor());
        }
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

        if (IacucStatus.DistributeSubcommittee.isDefKey(iacucTaskForm.getTaskDefKey())) {
            if (iacucTaskForm instanceof IacucDistributeSubcommitteeForm) {
                log.info("meeting data: {}", iacucTaskForm.getDate());
                taskService.setVariable(taskId, "meetingDate", iacucTaskForm.getDate());

            }
        }
        // attach corr to this task
        IacucCorrespondence corr = iacucTaskForm.getCorrespondence();
        if (corr != null) {
            Map<String, String> corrProperties = corr.getProperties();
            if (!corrProperties.isEmpty()) {
                taskService.setVariableLocal(taskId, "IacucCorrespondence" + taskId, corrProperties);
            }
        }

        // determine the direction
        Map<String, Object> map = iacucTaskForm.getTaskVariables();
        if (map != null && !map.isEmpty())
            taskService.complete(taskId, map); // go left/right/middle or go ...
        else
            taskService.complete(taskId); // go straight
    }

    Task getTaskByTaskDefKey(String bizKey, String defKey) {

        return taskService.createTaskQuery()
                .processInstanceBusinessKey(bizKey)
                .taskDefinitionKey(defKey)
                .singleResult();
    }

    public void testAevt(String bizKey, String userId, boolean return2pi) {
        Map<String, Object> processInput = new HashMap<String, Object>();
        processInput.put("START_GATEWAY", IacucStatus.Submit.gatewayValue());
        runtimeService.startProcessInstanceByKey(AevtRptReviewProcDefKey, bizKey);
        completeAction(bizKey, userId, IacucStatus.Submit.taskDefKey(), IacucStatus.Submit.statusName(), "pi submit adverse event report");
        if (!return2pi) {
            printOpenTasks(bizKey);
        } else {
            completeAction(bizKey, userId, IacucStatus.ReturnToPI.taskDefKey(), IacucStatus.ReturnToPI.statusName(), "return-2-pi comment");
        }
        assertEquals(0, taskCount(bizKey));
        printHistory(bizKey);
    }

    List<Task> getOpenTasks() {
        TaskQuery taskQuery = taskService.createTaskQuery().taskUnassigned();
        return taskQuery.active().list();
    }

    long taskCount(String bizKey) {
        return taskService
                .createTaskQuery()
                .processInstanceBusinessKey(bizKey)
                .active().list().size();
    }

    void printHistory(String bizKey) {
        log.info("history...");
        List<HistoricTaskInstance> list = historyService
                .createHistoricTaskInstanceQuery()
                .processInstanceBusinessKey(bizKey)
                .taskDeleteReason("completed")
                .orderByHistoricTaskInstanceEndTime()
                .finished()
                .desc()
                .list();
        for (HistoricTaskInstance hs : list) {
            log.info("taskDefKey={}, taskName={}, deleteReason={}", hs.getTaskDefinitionKey(), hs.getName(), hs.getDeleteReason());
        }
    }

    void printOpenTasks(String bizKey) {
        log.info("open tasks:");
        List<Task> taskList = taskService
                .createTaskQuery()
                .processDefinitionKey(ProtocolReviewProcDefKey)
                .processInstanceBusinessKey(bizKey)
                .list();
        for (Task task : taskList) {
            log.info("taskDefKey={},taskName={}", task.getTaskDefinitionKey(), task.getName());
        }
    }

    boolean canDistribute(String bizKey) {
        ProcessInstance pai = runtimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(bizKey)
                .singleResult();

        return historyService
                .createHistoricTaskInstanceQuery()
                .processInstanceBusinessKey(bizKey)
                .finished()
                .processInstanceId(pai.getProcessInstanceId())
                .taskDefinitionKeyLike("rv%").list().isEmpty();
    }

}


