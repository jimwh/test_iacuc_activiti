package edu.columbia.rascal.business.service.auxiliary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

public class IacucProcessHistoricData {

    private String processId;
    private String id;
    private String protocolId;
    private String taskDefKey;
    private String name;
    private String description;
    private String reviewType;
    private String snapshotId;
    private Date startTime;
    private Date endTime;
    private String createdBy;
    private String adminNoteId;
    private String adminNote;
    private Date meetingDate;
    private IacucDesignatedUserReview designatedUserReview;
    private final List<IacucCorrespondence> correspondence = new ArrayList<IacucCorrespondence>();
    private boolean piNotified;
    private String piUni;
    private String taskCancelledBy;

    // for show business: in case there are multiple distributions   
    private int distributionCount;

    private List<String> reviewerList;

    // for adverse event history 
    private String adverseEventId;
    private boolean approvalStatus;

    public List<String> getReviewerList() {
        return this.reviewerList;
    }

    public void setReviewerList(List<String> list) {
        this.reviewerList = list;
    }

    public static IacucProcessHistoricData create(String processId, String bizKey, String taskId, boolean isProtocol) {
        return new IacucProcessHistoricData(processId, bizKey, taskId, isProtocol);
    }

    private IacucProcessHistoricData(String processId, String bizKey, String taskId, boolean isProtocol) {
        this.processId = processId;
        this.id = taskId;
        if (isProtocol)
            this.protocolId = bizKey;
        else {
            this.adverseEventId = bizKey;
        }
    }

    public String getProcessId() {
        return processId;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public String getTaskDefKey() {
        return taskDefKey;
    }

    public void setTaskDefKey(String taskDefKey) {
        this.taskDefKey = taskDefKey;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public boolean hasSnapshot() {
        return this.snapshotId != null;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public Date getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(Date meetingDate) {
        this.meetingDate = meetingDate;
    }

    public List<IacucCorrespondence> getCorrespondence() {
        return correspondence;
    }

    // we keep internal list itself immutable but the elements of list could be updated
    public void setCorrespondence(List<IacucCorrespondence> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        this.correspondence.clear();
        this.correspondence.addAll(list);
        if (list.size() > 1) {
            Collections.reverse(this.correspondence);
        }
    }

    public boolean hasCorrespondence() {
        return this.correspondence.size() != 0;
    }

    public void clearCorr() {
        this.correspondence.clear();
    }

    public IacucDesignatedUserReview getDesignatedUserReview() {
        return this.designatedUserReview;
    }

    public void setDesignatedUserReview(IacucDesignatedUserReview detail) {
        this.designatedUserReview = detail;
    }


    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getAdminNoteId() {
        return adminNoteId;
    }

    public void setAdminNoteId(String adminNoteId) {
        this.adminNoteId = adminNoteId;
    }

    public String getId() {
        return id;
    }

    public boolean getShowStatus() {
        return !IacucStatus.ASSIGNEEREVIEW.isDefKey(this.taskDefKey);
    }

    public String getStartTimeString() {
        if (this.startTime == null) return "";
        return getDateFmt(this.startTime);
    }

    public String getEndTimeString() {
        if (this.endTime == null) return "";
        return getDateFmt(this.endTime);
    }

    private String getDateFmt(Date date) {
        DateTime dateTime = new DateTime(date);
        return dateTime.toString("MM/dd/yyyy HH:mm:ss");
    }

    public boolean isPiNotified() {
        return piNotified;
    }

    public void setPiNotified(boolean piNotified) {
        this.piNotified = piNotified;
    }

    // for front show purpose
    public String getDisplayStatus() {
        if (IacucStatus.SUBMIT.isDefKey(taskDefKey)) {
            return "Submit";
        } else if (IacucStatus.DISTRIBUTE.isDefKey(taskDefKey)) {
            return "Distribute";
        } else if (IacucStatus.RETURNTOPI.isDefKey(taskDefKey)) {
            return "Return to PI";
        } else if (IacucStatus.SUBCOMITTEEREVIEW.isDefKey(taskDefKey)) {
            return "Sub-Committee Review";
        }
        //else if (IacucStatus.FULLBOARDREVIEW.isDefKey(taskDefKey)) {
        //    return "Full Committee Review";
        //}
        else if (IacucStatus.ASSIGNEEREVIEW.isDefKey(taskDefKey)) {
            String action = getReviewerDisplayAction();
            return action == null ? "Designated User Review" : action;
        } else if (IacucStatus.FINALAPPROVAL.isDefKey(taskDefKey)) {
            return "Approve";
        } else if (IacucStatus.EMAILONLY.isDefKey(taskDefKey)) {
            return "Email Task";
        } else if (IacucStatus.ADMINREVIEW.isDefKey(taskDefKey)) {
            return "Administrator Review";
        } else if (IacucStatus.REINSTATE.isDefKey(taskDefKey)) {
            return "Reinstate";
        } else if (IacucStatus.TERMINATE.isDefKey(taskDefKey)) {
            return "Terminate";
        } else if (IacucStatus.SUSPEND.isDefKey(taskDefKey)) {
            return "Suspend";
        } else if (IacucStatus.WITHDRAW.isDefKey(taskDefKey)) {
            return "Withdraw";
        }
        return "";
    }

    private String getReviewerDisplayAction() {
        if (designatedUserReview != null) {
            return designatedUserReview.getDisplayAction();
        }
        return "Designated User No Action";
    }

    public boolean getReviewerHasAction() {
        return designatedUserReview != null;
    }

    public String getAdverseEventId() {
        return adverseEventId;
    }

    public void setAdverseEventId(String adverseEventId) {
        this.adverseEventId = adverseEventId;
    }

    public boolean getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(boolean approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getPiUni() {
        return piUni;
    }

    public void setPiUni(String piUni) {
        this.piUni = piUni;
    }

    public void updatePiNotified() {
        for (IacucCorrespondence corr : correspondence) {
            if (corr.recipientContains(piUni)) {
                this.piNotified = true;
                return;
            }
        }
    }

    public String getTaskCancelledBy() {
        return taskCancelledBy;
    }

    public void setTaskCancelledBy(String taskCancelledBy) {
        this.taskCancelledBy = taskCancelledBy;
    }

    public int getDistributionCount() {
        return distributionCount;
    }

    public void setDistributionCount(int distributionCount) {
        this.distributionCount = distributionCount;
    }

    // for front view purpose 
    private boolean showNormalUser = true;

    public boolean getShowNormalUser() {
        return this.showNormalUser;
    }

    public void setShowNormalUser(boolean bool) {
        this.showNormalUser = bool;
    }

    public boolean getIsDistribution() {
        return IacucStatus.DISTRIBUTE.isDefKey(this.taskDefKey);
    }

    public boolean getIsDesignatedReview() {
        return IacucStatus.ASSIGNEEREVIEW.isDefKey(this.taskDefKey);
    }

    public boolean getIsAdminReview() {
        return IacucStatus.ADMINREVIEW.isDefKey(this.taskDefKey);
    }

    public boolean getIsSubCommitteeReview() {
        return IacucStatus.SUBCOMITTEEREVIEW.isDefKey(this.taskDefKey);
    }

    public boolean getIsEmailTask() {
        return IacucStatus.EMAILONLY.isDefKey(this.taskDefKey);
    }

    public boolean getIsSubmission() {
        return IacucStatus.SUBMIT.isDefKey(this.taskDefKey);
    }

    public String getDistributionDisplay() {
        if (!IacucStatus.DISTRIBUTE.isDefKey(this.taskDefKey)) {
            return null;
        }

        if (IacucReviewType.SubIacucCommitte.isType(reviewType)) {
            return "Distribute to Sub-Committee";
        } else if (IacucReviewType.DesignatedReviewers.isType(reviewType)) {
            if (reviewerList == null || reviewerList.isEmpty()) return null;
            String str = reviewerList.toString();
            if (!StringUtils.isBlank(str)) {
                str = str.replaceAll("\\[|\\]", "");
            }
            return "Distribute to Designated User(s): " + str;
        } else
            return null;
    }

    public void append(List<IacucCorrespondence> corr) {
        correspondence.addAll(corr);
    }

    // for show business comparison in moving around position
    public long getEndTimeValue() {
    	return this.endTime==null ? 0 : this.endTime.getTime();
    }

    // for show business comparison in moving around position
    public long getFirstCorrTimeValue() {
    	if( this.correspondence.isEmpty() ) {
    		return 0;
    	}
    	IacucCorrespondence corr=this.correspondence.get(0);
    	Date d = corr.getCreationDate();
    	return d==null? 0 : d.getTime();
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[id=").append(id)
                .append(",protocolId=").append(protocolId)
                .append(",taskDefKey=").append(taskDefKey)
                .append(",name=").append(name)
                .append(",description=").append(description)
                .append(",reviewType=").append(reviewType)
                .append(",startTime=").append(startTime)
                .append(",endTime=").append(endTime)
                .append(",createdBy=").append(createdBy)
                .append(",snapshotId=").append(snapshotId)
                .append(",meetingDate=").append(meetingDate)
                .append(",adminNoteId=").append(adminNoteId)
                .append(",adminNote=").append(adminNote)
                .append(",correspondence=").append(correspondence)
                .append(",designatedUserReview=").append(designatedUserReview)
                .append("]");
        return sb.toString();
    }

    public boolean getIsTaskCancelled() {
        return this.taskCancelledBy != null;
    }
}
