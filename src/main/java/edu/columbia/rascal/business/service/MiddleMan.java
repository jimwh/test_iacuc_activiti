package edu.columbia.rascal.business.service;

import edu.columbia.rascal.business.service.auxiliary.*;

import org.activiti.engine.ManagementService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.cmd.AbstractCustomSqlExecution;
import org.activiti.engine.impl.cmd.CustomSqlExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class MiddleMan {

    private static final Logger log = LoggerFactory.getLogger(MiddleMan.class);
    private final JdbcTemplate jdbcTemplate;
    @Resource
    private ManagementService managementService;
    @Resource
    private IacucProcessService processService;
    @Autowired
    public MiddleMan(JdbcTemplate jt) {
        this.jdbcTemplate = jt;
    }

    public void testTimedReleaseApproval() {
        String protocolId="111";
        String userId="tester";
        // abortProcess("111", "abort the process by "+userId);
        if( !submitProtocol(protocolId, userId) ) {
            log.error("submission err");
        }
        if( !completePreliminaryReviewTask(protocolId, userId, false) ){
            log.error("completePreliminaryReviewTask err");
        }
        //
        IacucActivitiAdminProcessForm form=new IacucActivitiAdminProcessForm();
        form.setReviewType(IacucReviewType.DesignatedReviewers.typeCode());
        form.setAdminNote("distribution to designated reviewer");
        List<String>reviewer=new ArrayList<String>();
        reviewer.add("rscl1005");
        reviewer.add("rscl1006");
        form.setReviewerList(reviewer);
        if( processService.completeDistributeTask(protocolId,userId, form)==null ) {
            log.error("failed to completeDistributeTask");
        }
        IacucDesignatedUserReview rv=new IacucDesignatedUserReview();
        rv.setUserId(reviewer.get(0));
        rv.setActionDate(new Date());
        rv.setAction(IacucApprovalType.APPROVE.typeCode());
        if( processService.completeTaskByAssignee(protocolId, reviewer.get(0), rv)==null ) {
            log.error("failed to completeTaskByAssignee");
        }
        rv=new IacucDesignatedUserReview();
        rv.setUserId(reviewer.get(1));
        rv.setActionDate(new Date());
        rv.setAction( IacucApprovalType.APPROVE.typeCode() );
        if( processService.completeTaskByAssignee(protocolId, reviewer.get(1), rv)==null ) {
            log.error("failed to completeTaskByAssignee");
        }
        //
        IacucActivitiAdminProcessForm fm=new IacucActivitiAdminProcessForm();
        fm.setAdminNote("I do paper work");
        fm.setAdminReviewOutcome( IacucApprovalType.APPROVE.typeCode() );
        if( processService.completeAdminReviewTask(protocolId,userId,fm)==null ) {
            log.error("failed to completeAdminReviewTask");
        }
        //
        if( processService.completeFinalApprovalTask(protocolId,userId,"it is approved")==null ) {
            log.error("failed to completeFinalApprovalTask");
        }
        getIacucProtocolHistory(protocolId);
    }

    public void testUndoApprovalBackToSubprocess() {
        String protocolId="111";
        String userId="tester";
        abortProcess("111", "abort the process by "+userId);
        if( !submitProtocol(protocolId, userId) ) {
            log.error("submission err");
        }
        if( !completePreliminaryReviewTask(protocolId, userId, false) ){
            log.error("completePreliminaryReviewTask err");
        }
        //
        IacucActivitiAdminProcessForm form=new IacucActivitiAdminProcessForm();
        form.setReviewType(IacucReviewType.DesignatedReviewers.typeCode());
        form.setAdminNote("distribution to designated reviewer");
        List<String>reviewer=new ArrayList<String>();
        reviewer.add("rscl1005");
        reviewer.add("rscl1006");
        form.setReviewerList(reviewer);
        if( processService.completeDistributeTask(protocolId,userId, form)==null ) {
            log.error("failed to completeDistributeTask");
            return;
        }
        //
        IacucDesignatedUserReview rv=new IacucDesignatedUserReview();
        rv.setUserId(reviewer.get(0));
        rv.setActionDate(new Date());
        rv.setAction( IacucApprovalType.APPROVE.typeCode() );
        if( processService.completeTaskByAssignee(protocolId, reviewer.get(0), rv)==null ) {
            log.error("failed to completeTaskByAssignee");
            return;
        }
        rv=new IacucDesignatedUserReview();
        rv.setUserId(reviewer.get(1));
        rv.setActionDate(new Date());
        rv.setAction( IacucApprovalType.APPROVE.typeCode() );
        if( processService.completeTaskByAssignee(protocolId, reviewer.get(1), rv)==null ) {
            log.error("failed to completeTaskByAssignee");
            return;
        }

        // admin review
        IacucActivitiAdminProcessForm fm=new IacucActivitiAdminProcessForm();
        fm.setAdminNote("I do paper work");
        fm.setAdminReviewOutcome( IacucApprovalType.APPROVE.typeCode() );
        if( processService.completeAdminReviewTask(protocolId,userId,fm)==null ) {
            log.error("failed to completeAdminReviewTask");
            return;
        }
        // final approval
        if( processService.completeFinalApprovalTask(protocolId,userId,"it is approved")==null ) {
            log.error("failed to completeFinalApprovalTask");
            return;
        }

        //
        if( processService.completeUndoApprovalTask(protocolId, userId, "back to designated review")==null) {
            log.error("failed to completeUndoApprovalTaskBackToSubprocess");
            return;
        }
    }

    public void testTimedReleaseReturnToPI() {
        String protocolId="111";
        String userId="tester";
        // abortProcess("111", "abort the process by "+userId);
        if( !submitProtocol(protocolId, userId) ) {
            log.error("submission err");
        }
        if( !completePreliminaryReviewTask(protocolId, userId, true) ){
            log.error("completePreliminaryReviewTask err");
        }
        if( !completeReturnToPI(protocolId, userId) ) {
            log.error("completeReturnToPiTask err");
        }
    }

    public void testUndoReturnToPI() {
        String protocolId="111";
        String userId="tester";
        // abortProcess("111", "abort the process by "+userId);
        if( !submitProtocol(protocolId, userId) ) {
            log.error("submission err");
        }
        if( !completePreliminaryReviewTask(protocolId, userId, true) ){
            log.error("completePreliminaryReviewTask err");
        }
        if( !completeReturnToPI(protocolId, userId) ) {
            log.error("completeReturnToPiTask err");
        }
        log.info("test completeUndoReturnToPiTask...");
        if(processService.completeUndoReturnToPI(protocolId, userId, "undo return-2-pi")==null) {
            log.error("err in processService.completeUndoReturnToPiTask...");
        }
    }

    public void testUndoReturnToPIBackToSubcommittee() {
        String protocolId="111";
        String userId="tester";
        // abortProcess("111", "abort the process by "+userId);
        if( !submitProtocol(protocolId, userId) ) {
            log.error("submission err");
        }
        if( !completePreliminaryReviewTask(protocolId, userId, false) ){
            log.error("failed to completePreliminaryReview");
        }
        //
        IacucActivitiAdminProcessForm form=new IacucActivitiAdminProcessForm();
        form.setReviewType(IacucReviewType.SubIacucCommitte.typeCode());
        form.setAdminNote("distribution to subcommittee");
        form.setMeetingDate(new Date());
        if( processService.completeDistributeTask(protocolId,userId, form)==null ) {
            log.error("failed to completeDistributeTask");
        }
        //
        if( processService.completeSubCommitteeReviewTask(protocolId,userId,false)==null ) {
            log.error("failed to completeSubCommitteeReviewTask");
        }
        //
        if( !completeReturnToPI(protocolId, userId) ) {
            log.error("completeReturnToPiTask err");
        }
        /*
        log.info("test completeUndoReturnToPI...");
        if( processService.completeUndoReturnToPI(protocolId, userId, "undo return-2-pi")==null ) {
            log.error("failed to err completeUndoReturnToPI");
        }*/

    }

    public void testReturnToPiImmediatelyRelease() {
        String protocolId="111";
        String userId="tester";
        // abortProcess("111", "abort the process by "+userId);
        if( !submitProtocol(protocolId, userId) ) {
            log.error("submission err");
        }
        if( !completePreliminaryReviewTask(protocolId, userId, true) ){
            log.error("completePreliminaryReviewTask err");
        }

        if( !completeReturnToPiGoRelease(protocolId, userId) ) {
            log.error("completeReturnToPiTask err");
        }
    }

    public void testReturnToPiGoEnd() {
        String protocolId="111";
        String userId="tester";
        // abortProcess("111", "abort the process by "+userId);
        if( !submitProtocol(protocolId, userId) ) {
            log.error("submission err");
        }
        if( !completePreliminaryReviewTask(protocolId, userId, true) ){
            log.error("completePreliminaryReviewTask err");
        }
        if( !completeReturnToPiGoEnd(protocolId, userId) ) {
            log.error("completeReturnToPiTask err");
        }
    }

    private boolean submitProtocol(String protocolId, String userId) {
        if (processService.isProcessStarted(protocolId)) {
            if( hasTask(protocolId, IacucStatus.UndoReturnToPI.taskDefKey())) {
                // end undo return to PI
                if( !cancelUndoReturnToPI(protocolId, userId, "cancel undoReturnPI") )  {
                    return startProcess(protocolId, userId);
                }else {
                    log.error("unable to cancel undo return to PI, protocolId=" + protocolId);
                    return false;
                }
            }else {
                log.error("process was already started for protocolId=" + protocolId);
                return false;
            }
        } else {
            return startProcess(protocolId, userId);
        }
    }

    private boolean startProcess(String protocolId, String userId) {
        String procId=processService.startProtocolProcess(protocolId, userId);
        return procId != null;
    }

    private boolean cancelUndoReturnToPI(String protocolId, String userId, String reason) {
        String taskId = processService.cancelUndoReturnToPI(protocolId, userId, reason);
        return (taskId != null);
    }

    private boolean completePreliminaryReviewTask(String protocolId, String userId, final boolean returnToPiBool) {
        String taskId = processService.completePreliminaryReviewTask(protocolId, userId, returnToPiBool);
        return taskId != null;
    }

    private boolean completeReturnToPI(String protocolId, String userId) {
        if ( !hasTask(protocolId, IacucStatus.RETURNTOPI.taskDefKey()) ) {
            log.error("no task=" + IacucStatus.RETURNTOPI.taskDefKey());
            return false;
        }
        log.info("return-2-pi go undoReturnToPI");
        String taskId = processService.completeReturnToPI(protocolId, userId, "go undo return to PI");
        return (taskId != null);
    }

    private boolean completeReturnToPiGoRelease(String protocolId, String userId) {
        if ( !hasTask(protocolId, IacucStatus.RETURNTOPI.taskDefKey()) ) {
            log.error("no task=" + IacucStatus.RETURNTOPI.taskDefKey());
            return false;
        }
        log.info("immediately release after return-2-pi");
        String taskId = processService.completeReturnToPiTaskGoRelease(protocolId, userId, "immediately release");
        return (taskId != null);
    }

    private boolean completeReturnToPiGoEnd(String protocolId, String userId) {
        if ( !hasTask(protocolId, IacucStatus.RETURNTOPI.taskDefKey()) ) {
            log.error("no task=" + IacucStatus.RETURNTOPI.taskDefKey());
            return false;
        }
        log.info("immediately end after return-2-pi");
        String taskId = processService.completeReturnToPiTaskGoEnd(protocolId, userId, "immediately end");
        return (taskId != null);
    }


    public String getCurrentTaskDefKey(String protocolId) {
        return processService.getCurrentTaskDefKey(protocolId);
    }

    public boolean hasTask(String protocolId, String taskDefKey) {
        return processService.hasTaskByTaskDefKey(protocolId, taskDefKey);
    }

    public boolean hasTaskForReviewer(String protocolId, String userId) {
        return processService.hasTaskForAssignee(protocolId, userId);
    }


    public boolean abortProcess(String protocolId, String deletedReason) {
        log.info("process abort protocolId=" + protocolId);
        return processService.deleteProcessInstanceByProtocolId(protocolId, deletedReason);
    }
    public void foo() {
        CustomSqlExecution<IacucMybatisMapper, List<Map<String, Object>>>
                sqlExecution =
                new AbstractCustomSqlExecution<IacucMybatisMapper,
                        List<Map<String, Object>>>(IacucMybatisMapper.class) {

                    public List<Map<String, Object>> execute(IacucMybatisMapper
                                                                     customMapper) {
                        return customMapper.selectTasks();
                    }
                };

        List<Map<String, Object>> results =
                managementService.executeCustomSql(sqlExecution);
        for (Map<String, Object> m : results) {
            for (String k : m.keySet()) {
                log.info("key==================" + k);
            }
        }
    }


    public void foo2() {
        CustomSqlExecution<IacucMybatisMapper, List<Map<String, Object>>>
                sqlExecution =
                new AbstractCustomSqlExecution<IacucMybatisMapper,
                        List<Map<String, Object>>>(IacucMybatisMapper.class) {

                    public List<Map<String, Object>> execute(IacucMybatisMapper customMapper) {
                        return customMapper.selectTaskWithSpecificVariable("PROTOCOL_ID");
                    }
                };

        List<Map<String, Object>> results = managementService.executeCustomSql(sqlExecution);
        for (Map<String, Object> m : results) {
            for (String k : m.keySet()) {
                log.info("Foo key=" + k);
            }
        }
    }

    /**
     * @param taskId  final String
     * @param endTime final Date
     * @return boolean
     */
    private boolean updateEndTimeByTaskId(final String taskId, final Date endTime) {
        CustomSqlExecution<IacucMybatisMapper, Boolean>
                sqlExecution =
                new AbstractCustomSqlExecution<IacucMybatisMapper,
                        Boolean>(IacucMybatisMapper.class) {
                    @Override
                    public Boolean execute(IacucMybatisMapper cmapper) {
                        cmapper.updateEndTime(taskId, endTime);
                        return true;
                    }
                };
        return managementService.executeCustomSql(sqlExecution);
    }

    public List<IacucProcessHistoricData> getIacucProtocolHistory(String protocolId) {
        List<IacucProcessHistoricData> list = new ArrayList<IacucProcessHistoricData>();
        try {
            log.info("startTime="+new Date());
            list = processService.getIacucProtocolHistory(protocolId);
            log.info("endTime="+new Date());
        } catch (Exception e) {
            log.error("caught exception:", e);
        }
        for(IacucProcessHistoricData hs: list) {
            log.info("taskDefKey="+hs.getTaskDefKey());
        }
        return list;
    }

    public void releaseReturnToPI(DelegateExecution execution) {
        log.info("release return to PI task: protocolId=bizKey=" + execution.getProcessBusinessKey() );
    }

    public void releaseApproval(DelegateExecution execution) {
        log.info("release approval task: protocolId=bizKey=" + execution.getProcessBusinessKey() );
    }

}
