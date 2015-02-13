package edu.columbia.rascal.business.service.auxiliary;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IacucCorrespondence implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String ID = "ID";
    private final String FROM = "FROM";
    private final String RECIPIENT = "RECIPIENT";
    private final String CARBONCOPY = "CARBONCOPY";
    private final String SUBJECT = "SUBJECT";
    private final String BODY = "BODY";
    private final String DATE = "DATE";

    private String id;
    private String fromUni;
    private String recipient;
    private String carbonCopy;
    private String subject;
    private String text;
    private Date creationDate;

    private String fromFirstLastNameUni;

    public void setCreationDate(Date date) {
        this.creationDate=date;
    }
    private static final Logger log=LoggerFactory.getLogger(IacucCorrespondence.class);
    
    public String getId() {
        return id;
    }

    public String getFrom() {
        return fromUni;
    }

    public boolean isValidFrom() {
        return !StringUtils.isBlank(this.fromUni);
    }

    public void setFrom(String fromUni) {
        this.fromUni = fromUni;
    }

    public String getSubject() {
        return subject;
    }

    public boolean isValidSubject() {
        return !StringUtils.isBlank(this.subject);
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void apply() {
    	if( this.creationDate == null) {
    		this.creationDate = new Date();
    		this.id = String.valueOf(this.creationDate.getTime());
    	}else {
            this.id = String.valueOf(this.creationDate.getTime());
        }
    }

    public void setFromFirstLastNameUni(String flu) {
    	this.fromFirstLastNameUni=flu;
    }

    public String getFromFirstLastNameUni() {
    	return this.fromFirstLastNameUni;
    }

    // it is for activity use, not for you
    public boolean isValid() {
    	if( StringUtils.isBlank(this.id) ) return false;
        if ( !isValidFrom() ) return false;
        if ( !isValidRecipient() ) return false;
        if ( !isValidSubject() ) return false;
        return true;
    }

    public String getRecipient() {
        return recipient;
    }

    public boolean isValidRecipient() {
        List<String> list = getRecipientAsList();
        return !list.isEmpty();
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public List<String> getRecipientAsList() {
        List<String> list = new ArrayList<String>();
        if ( StringUtils.isBlank(recipient) ) return list;
        String noSpaces=removeSpaces( recipient );
        list.addAll( Arrays.asList( noSpaces.split(",") ) );
        return list;
    }

    private String removeSpaces(String foo) {
    	return foo.replaceAll("\\s+", "");
    }
    
    public List<String> getCarbonCopyAsList() {
        List<String> list = new ArrayList<String>();
        if ( StringUtils.isBlank(carbonCopy) ) return list;
        String noSpaces=removeSpaces(carbonCopy);
        list.addAll( Arrays.asList(noSpaces.split(",") ) );
        return list;
    }

    public boolean recipientContains(String uni) {
        return this.recipient == null ? false : this.recipient.contains(uni);
    }

    public String getCarbonCopy() {
        return carbonCopy;
    }

    public void setCarbonCopy(String carbonCopy) {
        this.carbonCopy = carbonCopy;
    }

    public boolean carbonCopyContains(String uni) {
        return this.carbonCopy == null ? false : this.carbonCopy.contains(uni);
    }

    // save data to activity table
    public Map<String, Object> fieldToMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        if ( !StringUtils.isBlank(this.id) ) {
            map.put(ID, this.id);
        } 
        else {
            return map;
        }

        if ( !StringUtils.isBlank(this.fromUni) ) {
            map.put(FROM, this.fromUni);
        } else {
            map.clear();
            return map;
        }

        if (!StringUtils.isBlank(this.recipient)) {
            map.put(RECIPIENT, this.recipient);
        } else {
            map.clear();
            return map;
        }

        if (!StringUtils.isBlank(this.subject)) {
            map.put(SUBJECT, this.subject);
        } else {
            map.clear();
            return map;
        }

        if (!StringUtils.isBlank(this.text)) {
            map.put(BODY, this.text);
        } else {
            map.clear();
            return map;
        }

        if (!StringUtils.isBlank(this.carbonCopy)) {
            map.put(CARBONCOPY, this.carbonCopy);
        }

        if (this.creationDate != null && this.creationDate instanceof Date) {
            map.put(DATE, this.creationDate);
        } else {
            map.clear();
            return map;
        }

        return map;
    }

    // retrieve data from activiti table
    public boolean mapToFields(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
        	log.error("empty map");
        	return false;
        }
        boolean bool=true;
        if (map.get(ID) != null) {
            this.id = map.get(ID).toString();
        } else {
        	log.error("cannot get id");
            bool=false;
        }
        if (map.get(FROM) != null) {
            this.fromUni = map.get(FROM).toString();
        } else {
        	log.error("cannot get from");
            bool=false;
        }
        if (map.get(RECIPIENT) != null) {
            this.recipient = map.get(RECIPIENT).toString();
        } else {
        	log.error("cannot get recipient");
            bool=false;
        }
        if (map.get(CARBONCOPY) != null) {
            this.carbonCopy = map.get(CARBONCOPY).toString();
        } 

        if (map.get(SUBJECT) != null) {
            this.subject = map.get(SUBJECT).toString();
        } else {
        	log.error("cannot get subject");
            bool=false;
        }
        if (map.get(BODY) != null) {
            this.text = map.get(BODY).toString();
        } else {
        	log.error("cannot get body");
            bool=false;
        }
        Object objDate = map.get(DATE);
        if (objDate != null && objDate instanceof Date) {
            this.creationDate = (Date) objDate;
        } else {
        	log.error("cannot get date");
            bool=false;
        }

        return bool;
    }

    public String getDateString() {
    	if( this.creationDate==null) return "";
    	DateTime dateTime=new DateTime(this.creationDate);
    	return dateTime.toString("MM/dd/yyyy HH:mm:ss");
    }

    
    // just for front show purpose
    private boolean showCorrToUser=false; 
    
    public boolean getShowCorrToUser() {
    	return this.showCorrToUser;
    }
    
    public void setShowCorrToUser(String userId) {
    	if( !StringUtils.isBlank(fromUni) ) {
    		if( fromUni.contains(userId) ) {
    			this.showCorrToUser=true;
    		}
    	}
    	if( !StringUtils.isBlank(recipient) ) {
    		if( recipient.contains(userId) ) {
    			this.showCorrToUser=true;
    		}
    	}
    	if( !StringUtils.isBlank(carbonCopy) ) {
    		if( carbonCopy.contains(userId) ) {
    			this.showCorrToUser=true;
    		}
    	} 
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[id=").append(id)
                .append(",from=").append(fromUni)
                .append(",to=").append(recipient)
                .append(",cc=").append(carbonCopy)
                .append(",subject=").append(subject)
                .append(",body=").append(text)
                .append(",date=").append(creationDate).append("]");
        return sb.toString();
    }

}
