package edu.columbia.rascal.business.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import edu.columbia.rascal.business.service.review.iacuc.IacucStatus;
import edu.columbia.rascal.business.service.review.iacuc.IacucTaskForm;

@Service
public class IacucProtocolHeaderService {

    private static final Logger log = LoggerFactory.getLogger(IacucProtocolHeaderService.class);
    public static final String ANIMAL_CARE_OWNER = "IacucProtocolHeader";

    @Resource
    private IacucProcessService processService;

    // to speed up administrator queue looping
    //@Resource
    //private JdbcTemplate jdbcTemplate;

    // for sidebar thing
    private static final Set<String> ReviewerAuthSet = new HashSet<String>();

    static {
        ReviewerAuthSet.add("IACUC_CAN_REVIEW_APPROVE_PROTOCOL");
        ReviewerAuthSet.add("IACUC_CAN_REVIEW_FULL_BOARD_PROTOCOL");
        ReviewerAuthSet.add("IACUC_CAN_REVIEW_HOLD_PROTOCOL");
    }

    public boolean isSuspended(String headerId) {
        return processService.isSuspended(headerId);
    }
    public void suspendProtocolByHeaderId(String headerId) {
        processService.suspendProtocol(headerId);
    }

    public void activateProtocolByHeaderId(String headerId) {
        processService.activateProtocol(headerId);
    }

    public void attachSnapshot(String headerId, String taskDefKey){}


    public boolean submitProtocol(String protocolId) {

        if (!processService.isProtocolProcessStarted(protocolId)) {
            return startProcess(protocolId);
        } else if (processService.hasTaskByTaskDefKey(protocolId, IacucStatus.Submit.taskDefKey())) {
            return completeSubmit(protocolId);
        } else {
            log.error("protocolId={} is already in process", protocolId);
            return false;
        }
    }

    @Transactional
    private boolean completeSubmit(String headerId) {
        String userId = "tester";
        if (completeSubmit(headerId, userId) != null) {
            submissionUpdate(headerId, userId);
            return true;
        }
        return false;
    }

    private boolean startProcess(String headerId) {
        Map<String, Object> processInputMap = getAppendixInputMap(headerId);
        // default as false
        processInputMap.put(IacucProcessService.EXPEDITE_REVIEW, false);
        if( processInputMap.size()>1 ) {
            processInputMap.put(IacucProcessService.HAS_APPENDIX, true);
            if (processService.hasExpediteReviewCondition(headerId)) {
                processInputMap.put(IacucProcessService.EXPEDITE_REVIEW, true);
            }
        }
        // if there is an appendix attached to this protocol and it hasn't approved by safety officer yet,
        // the map contains something like ("hasAppendixA", true),("hasAppendixB", true)...
        // otherwise it is just an newly created empty map
        for (Map.Entry<String, Object> e : processInputMap.entrySet()) {
            log.info("appendix: k={},v={}", e.getKey(), e.getValue());
        }
        return startProcess(headerId, processInputMap);
    }

    private Map<String, Object> getAppendixInputMap(String one) {
        Map<String, Object> processInputMap = new HashMap<String, Object>();
        return processInputMap;
    }

    @Transactional
    private boolean startProcess(String protocolId, Map<String, Object> map) {
        String userId = "tester";

        if (!processService.startProtocolProcess(protocolId, userId, map)) {
            return false;
        }
        if (completeSubmit(protocolId, userId) != null) {
            submissionUpdate(protocolId, userId);
            return true;
        } else
            return false;
    }

    private String completeSubmit(String protocolId, String userId) {
        IacucTaskForm iacucTaskForm = new IacucTaskForm();
        iacucTaskForm.setAuthor(userId);
        iacucTaskForm.setBizKey(protocolId);
        iacucTaskForm.setTaskName(IacucStatus.Submit.statusName());
        iacucTaskForm.setTaskDefKey(IacucStatus.Submit.taskDefKey());
        return processService.completeTaskByTaskForm(IacucProcessService.ProtocolProcessDefKey, iacucTaskForm);
    }

    private void submissionUpdate(String headerId, String userId) {
        /*
        protocol.setSubmitDate(new Date());
        protocol.setApprovalDate(null);
        protocol.setCurrentStatus(IacucStatus.Submit.statusName());
        protocol.setLastModificationDate(new Date());
        protocol.setLastModifiedBy(rascalUserService.getRidByUserId(userId));
        IacucProtocolHeader header = save(protocol);
        removeStaffAccessPermission(header);
        addAdminReviewerAccess(header);
        addDeptAdminAccess(header);
        resetAppendixAccess(header);
        */
    }

    public boolean hasTask(String protocolId, String taskDefKey) {
        return processService.hasTaskByTaskDefKey(protocolId, taskDefKey);
    }


    public boolean hasReviewerTask(String protocolId) {
        return processService.hasReviewerTask(protocolId);
    }


    public InputStream getSnapshotContent(String snapshotId) {
        return processService.getSnapshotContent(snapshotId);
    }



    private boolean isAllReviewersApproved(String protocolId) {
        return processService.isAllReviewersApproved(protocolId);
    }


    IacucTaskForm getPreviousApprovedData(String protocolId) {
        IacucTaskForm form = processService.getPreviousApprovedData(protocolId);
        return (form != null) ? form : processService.getPreviousApprovedDataFromKaput(protocolId);
    }


    public boolean canAdminRedistribute(String protocolId) {
        return !processService.hasReviewerAction(protocolId);
    }


    public Set<String> getReviewerUserId(String protocolId) {
        return processService.getReviewerUserId(protocolId);
    }



    public Date getMeetingDateByBizKey(String protocolId) {
        return processService.getMeetingDateByBizKey(protocolId);
    }


    @Transactional
    public boolean addCorrespondence(IacucTaskForm iacucTaskForm, String... name) {
        iacucTaskForm.setTaskDefKey(IacucStatus.AddCorrespondence.taskDefKey());

        if (name != null && name.length != 0)
            iacucTaskForm.setTaskName(name[0]);
        else
            iacucTaskForm.setTaskName(IacucStatus.AddCorrespondence.statusName());
        return processService.addCorrespondence(iacucTaskForm);
    }

    @Transactional
    public boolean addNote(IacucTaskForm iacucTaskForm) {
        Assert.notNull(iacucTaskForm);
        iacucTaskForm.setTaskDefKey(IacucStatus.AddNote.taskDefKey());
        iacucTaskForm.setTaskName(IacucStatus.AddNote.statusName());
        return processService.addNote(iacucTaskForm);
    }

    public List<IacucTaskForm> getPreviousNote(String protocolId) {
        return processService.getPreviousNote(protocolId);
    }

    private void insertToGraniteTable(String protocolId) {
   }

    @Transactional
    public void completeAnimalOrder(IacucTaskForm taskForm) {
        Assert.notNull(taskForm);
        Assert.notNull(taskForm.getBizKey());
        Assert.notNull(taskForm.getAuthor());
        taskForm.setTaskDefKey(IacucStatus.AnimalOrder.taskDefKey());
        taskForm.setTaskName(IacucStatus.AnimalOrder.statusName());
        processService.completeTaskByTaskForm(IacucProcessService.ProtocolProcessDefKey, taskForm);
        log.info("place animal order ...");
    }

    private IacucTaskForm getHistoryByTaskId(String taskId) {
        return processService.getHistoryByTaskIdForPdfComparison(taskId);
    }



    @Transactional
    public boolean withdrawProtocol(IacucTaskForm taskForm) {

        Assert.notNull(taskForm);
        String protocolId = taskForm.getBizKey();
        String userId = taskForm.getAuthor();

        String currentStatus = "foo";
        if (!"ReturnToPI".equalsIgnoreCase(currentStatus)) {
            log.error("only ReturnToPI protocol can be withdrawl, protocolId={},userId={}", protocolId, userId);
            return false;
        }
        if (!processService.withdrawProtocol(protocolId, userId)) {
            log.error("unable to start withdrawl protocol process for protocolId={}, userId={}", protocolId, userId);
            return false;
        }

        taskForm.setTaskDefKey(IacucStatus.Withdraw.taskDefKey());
        taskForm.setTaskName(IacucStatus.Withdraw.statusName());
        processService.completeTaskByTaskForm(IacucProcessService.ProtocolProcessDefKey, taskForm);

        return true;
    }


    private String getDateString(Date date) {
        DateTime df = new DateTime(date);
        return df.toString("MM/dd/YYYY");
    }


    public List<Map<String, Object>> getSuspendedProtocolHeaderIdAndDate() {

        Map<String, Date> dataTmpList = processService.getHistoricSuspendedBizKeyAndDate();
        List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
        return dataList;
    }

    private String convertDateToString(Date someDate) {
        if (someDate == null) return "";
        DateTime dt = new DateTime(someDate);
        return dt.toString("MM/dd/YYYY");
    }


    public boolean isResubmitForAppendixApproval(String protocolId) {
        return processService.isResubmitForAppendixApproval(protocolId);
    }

    public void interruptTimerDuration(String headerId) {
        processService.interruptTimerDuration(headerId);
    }


    public List<Map<String, String>> getReviewerForReminder() {
        return processService.getReviewerForReminder();
    }

    public Date findSuspensionDate(String headerId) { return processService.findSuspensionDate(headerId); };
}
