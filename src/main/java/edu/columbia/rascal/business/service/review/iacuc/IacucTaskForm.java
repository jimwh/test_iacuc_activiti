package edu.columbia.rascal.business.service.review.iacuc;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

// ATTEN: "Method 'xyz()' is never used" is used by jsp page.
public class IacucTaskForm implements IacucTaskFormBase, Comparable<IacucTaskForm> {

    private static final Logger log = LoggerFactory.getLogger(IacucTaskForm.class);

    // all these lookup is for show business
    private static final Set<String>ReviewerTaskDefKeySet=new HashSet<String>();
    static {
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv1Approval.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv1Hold.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv1ReqFullReview.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv2Approval.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv2Hold.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv2ReqFullReview.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv3Approval.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv3Hold.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv3ReqFullReview.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv4Approval.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv4Hold.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv4ReqFullReview.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv5Approval.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv5Hold.taskDefKey());
    	ReviewerTaskDefKeySet.add(IacucStatus.Rv5ReqFullReview.taskDefKey());
    }

    private static final Set<String>AppendixTaskDefKeySet=new HashSet<String>();
    static {
    	AppendixTaskDefKeySet.add(IacucStatus.SOPreApproveA.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOPreApproveB.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOPreApproveC.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOPreApproveD.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOPreApproveE.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOPreApproveF.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOPreApproveG.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOPreApproveI.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOHoldA.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOHoldB.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOHoldC.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOHoldD.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOHoldE.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOHoldF.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOHoldG.taskDefKey());
    	AppendixTaskDefKeySet.add(IacucStatus.SOHoldI.taskDefKey());
    }
    
    private String bizKey;
    private String author;
    private String taskId;
    private String taskDefKey;
    private String taskName;
    private String comment;
    private String commentId;
    private String snapshotId;
    private Date endTime;
    private IacucCorrespondence iacucCorrespondence;
    private List<String> reviewerList = new ArrayList<String>();
    private Date date;

    public List<String> getReviewerList() {
        return reviewerList;
    }

    public void setReviewerList(List<String> list) {
        reviewerList = list;
    }

    public String getBizKey() {
        return bizKey;
    }

    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    public void setEndTime(Date date) {
        this.endTime = date;
    }
    public Date getEndTime() { return this.endTime; }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskDefKey() {
        return taskDefKey;
    }

    public void setTaskDefKey(String taskDefKey) {
        this.taskDefKey = taskDefKey;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public void setCorrespondence(IacucCorrespondence corr) {
        this.iacucCorrespondence = corr;
    }

    public IacucCorrespondence getCorrespondence() {
        return this.iacucCorrespondence;
    }


    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getCommentId() { return this.commentId; }
    public void setCommentId(String cid) { this.commentId=cid; }

    public String getSuspensionDateString() {
        if (date == null) {
            // old data doesn't have the suspension date when submitting the suspension
            // take the action date as the suspension date
            if(endTime != null) {
                return new DateTime(endTime).toString("MM/dd/yyyy");
            }else { return null; }
        }
        return new DateTime(date).toString("MM/dd/yyyy");
    }

    public String getMeetingDateString() {
        return (date == null) ? null :
            new DateTime(date).toString("MM/dd/yyyy");
    }

    public String getEndTimeString() {
        return (endTime == null) ? null :
            new DateTime(endTime).toString("MM/dd/yyyy HH:mm:ss");
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> map = new HashMap<String, String>();

        map.put("bizKey", bizKey);

        map.put("snapshotId", snapshotId);
        map.put("commentId", commentId);

        map.put("author", author);
        map.put("taskName", taskName);
        map.put("taskDefKey", taskDefKey);

        if (date != null) {
            DateTime dateTime = new DateTime(this.date);
            map.put("date", dateTime.toString());
        }
        if(!reviewerList.isEmpty()) {
            for(int i=1; i<reviewerList.size()+1; i++) {
                map.put("rv"+i, reviewerList.get(i-1));
            }
        }
        //...
        return map;
    }

    @Override
    public void setProperties(Map<String, String> map) {
        if (map == null || map.isEmpty()) return;

        taskName = map.get("taskName");
        taskDefKey = map.get("taskDefKey");
        // this.comment = map.get("comment");
        snapshotId = map.get("snapshotId");
        commentId = map.get("commentId");
        author = map.get("author");
        bizKey = map.get("bizKey");
        if (map.get("date") != null) {
            String ms = map.get("date");
            DateTime dateTime = new DateTime(ms);
            this.date = dateTime.toDate();
        }

        for(int i=1; i<6; i++) {
            String rv=map.get("rv" + i);
            if(rv!=null) {
                reviewerList.add(rv);
            }
        }
    }

    @Override
    public Map<String, Object> getTaskVariables() {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[taskName=").append(taskName)
                .append(",taskDefKey=").append(taskDefKey).append(",snapshotId=").append(snapshotId).append(",bizKey=").append(bizKey)
                .append(",author=").append(author);
        
        if (comment != null) {
            sb.append(",comment=").append(comment);
        }
        if (date != null) {
            sb.append(",date=").append(getMeetingDateString());
        }
        if (!reviewerList.isEmpty()) {
            sb.append(",reviewerList").append(reviewerList.toString());
        }

        sb.append(",endTime=").append(getEndTimeString()).append("]");
        if (iacucCorrespondence==null) {
            return sb.toString();
        }
        sb.append("\n").append(iacucCorrespondence.toString()).append("\n");
        return sb.toString();
    }

    @Override
    public int compareTo(IacucTaskForm itf) {
        if( this.endTime==null && itf.getEndTime()!=null) return -1;
        if( this.endTime!=null && itf.getEndTime()==null) return 1;

        return this.endTime.compareTo(itf.getEndTime());
    }
    
    private boolean showNormalUser;
    public boolean getShowNormalUser() { return showNormalUser; }
    public void setShowNormalUser(boolean bool) { showNormalUser=bool; }
    
    public boolean getIsDesignatedReview() {
    	return ReviewerTaskDefKeySet.contains(taskDefKey);
    }
    
    public boolean getIsAddCorrespondence() {
    	return IacucStatus.AddCorrespondence.isDefKey(taskDefKey);
    }
    
    public boolean getIsSubmission() {
        return IacucStatus.Submit.isDefKey(this.taskDefKey);
    }

    public boolean getIsSuspension() {
        return IacucStatus.Suspend.isDefKey(this.taskDefKey);
    }

    public boolean getIsDistributeReivewer() {
    	return IacucStatus.DistributeReviewer.isDefKey(taskDefKey);
    }

    public boolean getIsDistributeSubcommittee() {
    	return IacucStatus.DistributeSubcommittee.isDefKey(taskDefKey);
    }

    public boolean getIsSubcommitteeReview() {
        return getIsDistributeSubcommittee();
    }

    public boolean isAddNote() {
        return IacucStatus.AddNote.isDefKey(taskDefKey);
    }

    public boolean isExpediteReview() {
        return IacucStatus.ExpediteReview.isDefKey(taskDefKey);
    }

    public String getReviewerListAsString() {
    	return (reviewerList==null || reviewerList.isEmpty())? "" :
    	reviewerList.toString().replaceAll("\\[|\\]","");
    }

    public boolean getShowResearcher() {
        if (isAddNote()) return false;
        else if (getIsDesignatedReview()) return false;
        else if (getIsAddCorrespondence()) return false;
        else if (isExpediteReview()) return false;
        return true;
    }
    
    // all for show business
    public String getAppendixTypeAuthoritySuffix() {

    	if( IacucStatus.SOPreApproveA.isDefKey(taskDefKey)) {
    		return "APPROVE_A";
    	}
    	else if( IacucStatus.SOPreApproveB.isDefKey(taskDefKey)) {
    		return "APPROVE_B";
    	}

    	else if( IacucStatus.SOPreApproveC.isDefKey(taskDefKey)) {
    		return "APPROVE_C";
    	}
    	else if( IacucStatus.SOPreApproveD.isDefKey(taskDefKey)) {
    		return "APPROVE_D";
    	}
    	else if( IacucStatus.SOPreApproveE.isDefKey(taskDefKey)) {
    		return "APPROVE_E";
    	}
    	else if( IacucStatus.SOPreApproveF.isDefKey(taskDefKey)) {
    		return "APPROVE_F";
    	}
    	else if( IacucStatus.SOPreApproveG.isDefKey(taskDefKey)) {
    		return "APPROVE_G";
    	}
    	else if( IacucStatus.SOPreApproveI.isDefKey(taskDefKey)) {
    		return "APPROVE_I";
    	}

    	else if( IacucStatus.SOHoldA.isDefKey(taskDefKey)) {
    		return "HOLD_A";
    	}
    	else if( IacucStatus.SOHoldB.isDefKey(taskDefKey)) {
    		return "HOLD_B";
    	}
    	else if( IacucStatus.SOHoldC.isDefKey(taskDefKey)) {
    		return "HOLD_C";
    	}
    	else if( IacucStatus.SOHoldD.isDefKey(taskDefKey)) {
    		return "HOLD_D";
    	}
    	else if( IacucStatus.SOHoldE.isDefKey(taskDefKey)) {
    		return "HOLD_E";
    	}
    	else if( IacucStatus.SOHoldF.isDefKey(taskDefKey)) {
    		return "HOLD_F";
    	}
    	else if( IacucStatus.SOHoldG.isDefKey(taskDefKey)) {
    		return "HOLD_G";
    	}
    	else if( IacucStatus.SOHoldI.isDefKey(taskDefKey)) {
    		return "HOLD_I";
    	}
    	else return null;
    }
    
    public boolean getIsAppendixTask() {
    	return AppendixTaskDefKeySet.contains(taskDefKey);
    }

    private String processId;
    public String getProcessId() { return processId; }
    public void setProcessId(String id) { processId=id; }
}
