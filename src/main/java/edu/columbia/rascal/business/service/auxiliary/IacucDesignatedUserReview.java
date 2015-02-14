package edu.columbia.rascal.business.service.auxiliary;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class IacucDesignatedUserReview implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String USERID = "USERID";
    private static final String ACTION = "ACTION";
    private static final String ACTION_NOTE = "ACTION_NOTE";
    private static final String ACTION_DATE = "ACTION_DATE";

    private String userId;
    private String firstNameLastNameUni;

    // action string can only be
    // IacucApprovalType.APPROVE.typeCode() or
    // IacucApprovalType.HOLD.typeCode() or
    // IacucApprovalType.FULLBOARD.typeCode()
    //
    private String action;
    private Date actionDate;
    private String actionNote;

    public String getActionNote() {
        return this.actionNote;
    }

    public void setActionNote(String actionNote) {
        this.actionNote = actionNote;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String uni) {
        this.userId = uni;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean hasAction() {
    	return this.action != null;
    }
    
    // The task name:
    // Designated Reviewer Approval
    // Designated Reviewer Request Full Review
    // Designated Reviewer Hold
    // it will be called when complete task 
    public String getDisplayAction() {
        if (IacucApprovalType.APPROVE.isType(action)) {
            return "Designated Reviewer Approval";
        } else if (IacucApprovalType.HOLD.isType(action)) {
            return "Designated Reviewer Hold";
        } else if (IacucApprovalType.FULLCOMMITTEE.isType(action)) {
            return "Designated Reviewer Request Full Review";
        } else {
            return "Designated Reviewr No Action";
        }
    }
    public Date getActionDate() {
        return actionDate;
    }

    public void applyAction() {
        this.actionDate = new Date();
    }


    public String getFirstNameLastNameUni() {
        return firstNameLastNameUni;
    }

    public void setFirstNameLastNameUni(String nameUni) {
        this.firstNameLastNameUni = nameUni;
    }

    /**
     * @return Map<String,Object> if it is an empty map, at least one file is not been set
     */
    public Map<String, Object> fieldsToMap() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        if (!StringUtils.isBlank(this.userId)) {
            map.put(USERID, this.userId);
        } else {
            return map;
        }
        if (!StringUtils.isBlank(this.action)) {
            map.put(ACTION, this.action);
        } else {
            map.clear();
            return map;
        }
        // action note is optional
        if ( !StringUtils.isBlank(this.actionNote) ) {
            map.put(ACTION_NOTE, this.actionNote);
        } 

        if (this.actionDate != null) {
            map.put(ACTION_DATE, this.actionDate);
        } else {
            map.clear();
        }
        return map;
    }

    public boolean mapToFields(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return false;

        if (map.get(USERID) != null) {
            this.userId = map.get(USERID).toString();
        } else {
            return false;
        }

        if (map.get(ACTION) != null) {
            this.action = map.get(ACTION).toString();
        } else {
            return false;
        }
        if (map.get(ACTION_NOTE) != null) {
            this.actionNote = map.get(ACTION_NOTE).toString();
        } else {
            return false;
        }

        Object objDate = map.get(ACTION_DATE);
        if (objDate != null && objDate instanceof Date) {
            this.actionDate = (Date) map.get(ACTION_DATE);
        } else {
            return false;
        }

        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[userId=").append(userId)
                .append(",action=").append(action)
                .append(",actionNote=").append(actionNote)
                .append(",actionDate=").append(actionDate).append("]");
        return sb.toString();
    }

}
