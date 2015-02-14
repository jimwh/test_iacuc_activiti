package edu.columbia.rascal.business.service.auxiliary;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class IacucListener implements TaskListener, ExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(IacucListener.class);

    private static final String AllRvs = "allRvs";
    private static final String hasAppendixA = "hasAppendixA";
    private static final String hasAppendixB = "hasAppendixB";
    private static final String hasAppendixC = "hasAppendixC";
    private static final String hasAppendixD = "hasAppendixD";
    private static final String hasAppendixE = "hasAppendixE";
    private static final String hasAppendixF = "hasAppendixF";
    private static final String hasAppendixG = "hasAppendixG";
    private static final String hasAppendixI = "hasAppendixI";

    private static final String AllAppendicesApproved = "allAppendicesApproved";
    private static final String appendixAApproved = "aApproved";
    private static final String appendixBApproved = "bAApproved";
    private static final String appendixCApproved = "cApproved";
    private static final String appendixDApproved = "dApproved";
    private static final String appendixEApproved = "eApproved";
    private static final String appendixFApproved = "fApproved";
    private static final String appendixGApproved = "gApproved";
    private static final String appendixIApproved = "iApproved";

    /**
     * Task listener will be called  by activity
     *
     * @param delegateTask
     */
    @Override
    public void notify(DelegateTask delegateTask) {

        DelegateExecution taskExecution = delegateTask.getExecution();
        String bizKey = taskExecution.getProcessBusinessKey();
        String processId = taskExecution.getProcessInstanceId();
        String taskId = delegateTask.getId();
        String taskDefKey = delegateTask.getTaskDefinitionKey();
        String eventName = delegateTask.getEventName();

        log.info("eventName={}, bizKey={}, taskDefKey={}, taskId={}, processId={}",
                eventName, bizKey, taskDefKey, taskId, processId);

        if (!"complete".equals(eventName)) {
            return;
        }

        if (IacucStatus.DistributeSubcommittee.isDefKey(taskDefKey)) {
            Object obj = delegateTask.getVariableLocal("iacucTaskForm" + taskId);
            if (obj != null) {
                Map<String, Object> map = (Map<String, Object>) obj;
                log.info("meetingDate={}", map.get("date"));
            }
        }

        if (IacucStatus.RETURNTOPI.isDefKey(taskDefKey)) {
            taskExecution.setVariable("undoApproval", false);
        }
        if (IacucStatus.UndoApproval.isDefKey(taskDefKey)) {
            taskExecution.setVariable("undoApproval", true);
        }
        if (IacucStatus.FINALAPPROVAL.isDefKey(taskDefKey)) {
            taskExecution.setVariable("undoApproval", false);
        }


        // for designated reviewers
        if (IacucStatus.Rv1Hold.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllRvs, false);
        } else if (IacucStatus.Rv1ReqFullReview.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllRvs, false);
        } else if (IacucStatus.Rv2Hold.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllRvs, false);
        } else if (IacucStatus.Rv2ReqFullReview.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllRvs, false);
        } else if (IacucStatus.Rv3Hold.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllRvs, false);
        } else if (IacucStatus.Rv3ReqFullReview.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllRvs, false);
        } else if (IacucStatus.Rv4Hold.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllRvs, false);
        } else if (IacucStatus.Rv4ReqFullReview.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllRvs, false);
        } else if (IacucStatus.Rv5Hold.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllRvs, false);
        } else if (IacucStatus.Rv5ReqFullReview.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllRvs, false);
        }

        // for appendices
        if (IacucStatus.SOPreApproveA.isDefKey(taskDefKey)) {
            taskExecution.setVariable(appendixAApproved, true);
            updateAppendixApproveStatus(delegateTask);
        } else if (IacucStatus.SOHoldA.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllAppendicesApproved, false);
        } else if (IacucStatus.SOPreApproveB.isDefKey(taskDefKey)) {
            taskExecution.setVariable(appendixBApproved, true);
            updateAppendixApproveStatus(delegateTask);
        } else if (IacucStatus.SOHoldB.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllAppendicesApproved, false);
        } else if (IacucStatus.SOPreApproveC.isDefKey(taskDefKey)) {
            taskExecution.setVariable(appendixCApproved, true);
            updateAppendixApproveStatus(delegateTask);
        } else if (IacucStatus.SOHoldC.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllAppendicesApproved, false);
        } else if (IacucStatus.SOPreApproveD.isDefKey(taskDefKey)) {
            taskExecution.setVariable(appendixDApproved, true);
            updateAppendixApproveStatus(delegateTask);
        } else if (IacucStatus.SOHoldD.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllAppendicesApproved, false);
        } else if (IacucStatus.SOPreApproveE.isDefKey(taskDefKey)) {
            taskExecution.setVariable(appendixEApproved, true);
            updateAppendixApproveStatus(delegateTask);
        } else if (IacucStatus.SOHoldE.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllAppendicesApproved, false);
        } else if (IacucStatus.SOPreApproveF.isDefKey(taskDefKey)) {
            taskExecution.setVariable(appendixFApproved, true);
            updateAppendixApproveStatus(delegateTask);
        } else if (IacucStatus.SOHoldF.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllAppendicesApproved, false);
        } else if (IacucStatus.SOPreApproveG.isDefKey(taskDefKey)) {
            taskExecution.setVariable(appendixGApproved, true);
            updateAppendixApproveStatus(delegateTask);
        } else if (IacucStatus.SOHoldG.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllAppendicesApproved, false);
        } else if (IacucStatus.SOPreApproveI.isDefKey(taskDefKey)) {
            taskExecution.setVariable(appendixIApproved, true);
            updateAppendixApproveStatus(delegateTask);
        } else if (IacucStatus.SOHoldI.isDefKey(taskDefKey)) {
            taskExecution.setVariable(AllAppendicesApproved, false);
        }

    }


    private void updateAppendixApproveStatus(DelegateTask delegateTask) {

        if (!(Boolean) delegateTask.getVariable(appendixAApproved)) {
            return;
        }
        if (!(Boolean) delegateTask.getVariable(appendixBApproved)) {
            return;
        }
        if (!(Boolean) delegateTask.getVariable(appendixCApproved)) {
            return;
        }
        if (!(Boolean) delegateTask.getVariable(appendixDApproved)) {
            return;
        }
        if (!(Boolean) delegateTask.getVariable(appendixEApproved)) {
            return;
        }
        if (!(Boolean) delegateTask.getVariable(appendixFApproved)) {
            return;
        }
        if (!(Boolean) delegateTask.getVariable(appendixGApproved)) {
            return;
        }
        if (!(Boolean) delegateTask.getVariable(appendixIApproved)) {
            return;
        }

        delegateTask.setVariable(AllAppendicesApproved, true);
    }

    /**
     * Execution listener will be called  by activity
     *
     * @param delegateExecution
     * @throws Exception
     */
    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {

        ExecutionEntity thisEntity = (ExecutionEntity) delegateExecution;
        ExecutionEntity superExecEntity = thisEntity.getSuperExecution();
        String eventName = delegateExecution.getEventName();

        if (superExecEntity == null) {
            setUpAppendixApproveStatus(delegateExecution);

            // get the business key of the main process
            log.info("main process: eventName={}, bizKey={}, procDefId={}", eventName, thisEntity.getBusinessKey(), thisEntity.getProcessDefinitionId());
            // used by designatedReviews output
            thisEntity.setVariable(AllRvs, true);

        } else {
            // in a sub-process so get the BusinessKey variable set by the caller.
            String key = (String) superExecEntity.getVariable("BusinessKey");
            boolean hasAppendix = (Boolean) superExecEntity.getVariable("hasAppendix");

            log.info("sub-process: eventName={}, bizKey={}, procDefId={}, hasAppendix={}",
                    eventName, key, thisEntity.getProcessDefinitionId(), hasAppendix);

            thisEntity.setVariable("BusinessKey", key);

            // for get task by business key
            thisEntity.setBusinessKey(key);

        }
    }

    private void setUpAppendixApproveStatus(DelegateExecution exe) {
        boolean bool = true;

        if (exe.getVariable(hasAppendixA) == null) {
            exe.setVariable(hasAppendixA, false);
            exe.setVariable(appendixAApproved, true);
        } else if ((Boolean) exe.getVariable(hasAppendixA)) {
            exe.setVariable(appendixAApproved, false);
            bool = false;
        } else {
            exe.setVariable(appendixAApproved, true);
        }

        if (exe.getVariable(hasAppendixB) == null) {
            exe.setVariable(hasAppendixB, false);
            exe.setVariable(appendixAApproved, true);
        } else if ((Boolean) exe.getVariable(hasAppendixB)) {
            exe.setVariable(appendixBApproved, false);
            bool = false;
        } else {
            exe.setVariable(appendixBApproved, true);
        }

        if (exe.getVariable(hasAppendixC) == null) {
            exe.setVariable(hasAppendixC, false);
            exe.setVariable(appendixCApproved, true);
        } else if ((Boolean) exe.getVariable(hasAppendixC)) {
            exe.setVariable(appendixCApproved, false);
            bool = false;
        } else {
            exe.setVariable(appendixCApproved, true);
        }

        if (exe.getVariable(hasAppendixD) == null) {
            exe.setVariable(hasAppendixD, false);
            exe.setVariable(appendixDApproved, true);
        } else if ((Boolean) exe.getVariable(hasAppendixD)) {
            exe.setVariable(appendixDApproved, false);
            bool = false;
        } else {
            exe.setVariable(appendixDApproved, true);
        }

        if (exe.getVariable(hasAppendixE) == null) {
            exe.setVariable(hasAppendixE, false);
            exe.setVariable(appendixEApproved, true);
        } else if ((Boolean) exe.getVariable(hasAppendixE)) {
            exe.setVariable(appendixEApproved, false);
            bool = false;
        } else {
            exe.setVariable(appendixEApproved, true);
        }

        if (exe.getVariable(hasAppendixF) == null) {
            exe.setVariable(hasAppendixF, false);
            exe.setVariable(appendixFApproved, true);
        } else if ((Boolean) exe.getVariable(hasAppendixF)) {
            exe.setVariable(appendixFApproved, false);
            bool = false;
        } else {
            exe.setVariable(appendixFApproved, true);
        }

        if (exe.getVariable(hasAppendixG) == null) {
            exe.setVariable(hasAppendixG, false);
            exe.setVariable(appendixGApproved, true);
        } else if ((Boolean) exe.getVariable(hasAppendixG)) {
            exe.setVariable(appendixGApproved, false);
            bool = false;
        } else {
            exe.setVariable(appendixGApproved, true);
        }

        if (exe.getVariable(hasAppendixI) == null) {
            exe.setVariable(hasAppendixI, false);
            exe.setVariable(appendixIApproved, true);
        } else if ((Boolean) exe.getVariable(hasAppendixI)) {
            exe.setVariable(appendixIApproved, false);
            bool = false;
        } else {
            exe.setVariable(appendixIApproved, true);
        }

        exe.setVariable(AllAppendicesApproved, bool);
        exe.setVariable("hasAppendix", !bool);

    }
}
