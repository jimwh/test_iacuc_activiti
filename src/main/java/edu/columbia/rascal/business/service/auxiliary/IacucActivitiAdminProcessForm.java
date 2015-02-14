package edu.columbia.rascal.business.service.auxiliary;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.joda.time.DateTime;

public class IacucActivitiAdminProcessForm {

    private String reviewType;
    private String adminNote;
    private Date meetingDate;
    private Set<String> reviewerList = new TreeSet<String>();
    private List<IacucDesignatedUserReview> reviewDetails = new ArrayList<IacucDesignatedUserReview>();
    private IacucCorrespondence correspondence;
    private List<String> withoutActionReviewerList = new ArrayList<String>();
    			
	// for final approval
    private Date approvalDate;
    private Date effectiveDate;
    private Date endDate;

    // possible final approval type:
    // IacucApprovalType.APPROVE.typeCode()
    // IacucApprovalType.SUSPEND.typeCode()
    // IacucApprovalType.TERMINATE.typeCode()
    private String finalApprovalType;

    // for administrator review
    // the possible outcome:
    // IacucApprovalType.APPROVE.typeCode()
    // IacucApprovalType.RETURNTOPI.typeCode()
    // IacucApprovalType.REDISTRIBUTE.typeCode()
    // IacucApprovalType.FULLBOARD.typeCode()
    private String adminReviewOutcome;

    // true - approval false - disapproval
    private boolean adverseEventApproval;

    // true - approval, false - disapproval
    private boolean terminationApproval;

    // true - approval, false - disapproval
    private boolean suspensionApproval;

    // for show purpose
    private String previousAdminNote;
    public String getPreviousAdminNote() {
    	return this.previousAdminNote;
    }
    public void setPreviousAdminNote(String note) {
    	this.previousAdminNote=note;
    }
    
    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
    	this.reviewType = reviewType;
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
    public String getMeetingDateString() {
    	if(meetingDate!=null) {
    		DateTime dt = new DateTime(meetingDate);
    		return dt.toString("MM/dd/YYYY");
    	}
    	return "";
    }
    public void setMeetingDate(Date meetingDate) {
    	this.meetingDate = meetingDate;
    }

    public Date getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(Date approvalDate) {
        this.approvalDate = approvalDate;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setCorrespondence(IacucCorrespondence correspondence) {
        this.correspondence = correspondence;
    }

    public List<IacucDesignatedUserReview> getReviewDetail() {
        return reviewDetails;
    }

    public void setReviewDetail(List<IacucDesignatedUserReview> detail) {
        this.reviewDetails = detail;
    }

    public void addReviewDetail(IacucDesignatedUserReview review) {
        if (review != null)
            this.reviewDetails.add(review);
    }

   public List<String> getReviewerList() {
	   if( reviewerList==null ) {
		   return new ArrayList<String>();
	   } else {
		   return new ArrayList<String>(reviewerList);
	   }
    }

    public void setReviewerList(List<String> rvList) {
    	if(rvList == null) return;
    	if( this.reviewerList == null) {
    		this.reviewerList=new TreeSet<String>();
    	}else {
    		this.reviewerList.clear();
    	}
    	this.reviewerList.addAll(rvList);
    }

    public void setReviewerList(Set<String> set) {
    	this.reviewerList = set;
    }
    
    public IacucCorrespondence getCorrespondence() {
        return correspondence;
    }

    public boolean getAdverseEventApproval() {
        return this.adverseEventApproval;
    }

    // true - approved; false - return to PI
    public void setAdverseEventApproval(boolean bool) {
        this.adverseEventApproval = bool;
    }

    public String getAdminReviewOutcome() {
        return adminReviewOutcome;
    }

    public void setAdminReviewOutcome(String outcome) {
        this.adminReviewOutcome = outcome;
    }

    public String getFinalApprovalType() {
        return finalApprovalType;
    }

    public void setFinalApprovalType(String finalApprovalType) {
        this.finalApprovalType = finalApprovalType;
    }

    public boolean getTerminationApproval() {
        return this.terminationApproval;
    }

    public void setTerminationApproval(boolean bool) {
        this.terminationApproval = bool;
    }

    public boolean getSuspensionApproval() {
        return this.suspensionApproval;
    }

    public void setSuspensionApproval(boolean bool) {
        this.suspensionApproval = bool;
    }
    
	public List<String> getWithoutActionReviewerList() {
		return withoutActionReviewerList;
	}

	public void setWithoutActionReviewerList(List<String> partReviewerList) {
		this.withoutActionReviewerList = partReviewerList;
	}

	private boolean validateEndDate=false;
	public void setValidateEndDate(boolean bool) {
		this.validateEndDate=bool;
	}
	public boolean validateEndDate() {
		return this.validateEndDate;
	}
	
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("fm[reviewType=").append(this.reviewType)
                .append(",adminNote=").append(this.adminNote)
                .append(",meetingDate=").append(this.meetingDate)
                .append(",reviewerList=").append(this.reviewerList)
                .append("]");
        return sb.toString();
    }
}
