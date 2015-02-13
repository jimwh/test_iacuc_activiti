package edu.columbia.rascal.business.service.auxiliary;

import java.util.HashSet;
import java.util.Set;

public enum IacucStatus {

    SUBMIT("Submit") {
        @Override
        public String taskDefKey() {
            return "preliminaryReview";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            return 1;
        }
    },

    DISTRIBUTE("Distribute") {
        @Override
        public String taskDefKey() {
            return "distribution";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            // unused
            return 11;
        }
    },

    SUBCOMITTEEREVIEW("SubCommittee") {
        @Override
        public String taskDefKey() {
            return "subCommittee";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            // Unused
            return 22;
        }
    },

    ASSIGNEEREVIEW("AssigneeReview") {
        @Override
        public String taskDefKey() {
            return "assigneeReview";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            // unused
            return 33;
        }
    },

    ADMINREVIEW("AdminReview") {
        @Override
        public String taskDefKey() {
            return "adminReview";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            // unused
            return 44;
        }
    },


    RETURNTOPI("ReturnToPI") {
        @Override
        public String taskDefKey() {
            return "returnToPI";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            // unused
            return 55;
        }
    },

    FINALAPPROVAL("Approve") {
        @Override
        public String taskDefKey() {
            return "finalApproval";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            // unused
            return 66;
        }
    },


    ADVERSEEVENT("AdverseEvent") {
        @Override
        public String taskDefKey() {
            return "adverseEvent";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            return 2;
        }
    },

    TERMINATE("Terminate") {
        @Override
        public String taskDefKey() {
            return "termination";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            return 3;
        }
    },

    SUSPEND("Suspend") {
        @Override
        public String taskDefKey() {
            return "suspension";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            return 4;
        }
    },

    WITHDRAW("Withdraw") {
        @Override
        public String taskDefKey() {
            return "withdrawal";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            return 5;
        }
    },

    EMAILONLY("EmailOnly") {
        @Override
        public String taskDefKey() {
            return "emailOnly";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            return 6;
        }
    },

    REINSTATE("Reinstate") {
        @Override
        public String taskDefKey() {
            return "reinstatement";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return statusName().equalsIgnoreCase(status);
        }

        @Override
        public int startGatewayValue() {
            return 7;
        }
    },

    KAPUT("Kaput") {
        @Override
        public String taskDefKey() {
            return "kaput";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return  statusName().equals(status) ? true : isKaput(status);
        }

        @Override
        public int startGatewayValue() {
            return 8;
        }
    },

    UndoReturnToPI("UndoReturnToPI") {
        @Override
        public String taskDefKey() {
            return "undoReturnToPI";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return  statusName().equals(status);
        }

        @Override
        public int startGatewayValue() {
            return 9;
        }
    },

    ReleaseReturnToPI("ReleaseReturnToPI") {
        @Override
        public String taskDefKey() {
            return "releaseReturnToPI";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return  statusName().equals(status);
        }

        @Override
        public int startGatewayValue() {
            return 10;
        }
    },

    UndoApproval("UndoApproval") {
        @Override
        public String taskDefKey() {
            return "undoApproval";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return  statusName().equals(status);
        }

        @Override
        public int startGatewayValue() {
            return 11;
        }
    },

    ReleaseApproval("ReleaseApproval") {
        @Override
        public String taskDefKey() {
            return "releaseApproval";
        }

        @Override
        public boolean isDefKey(String def) {
            return this.taskDefKey().equals(def);
        }

        @Override
        public boolean isStatus(String status) {
            return  statusName().equals(status);
        }

        @Override
        public int startGatewayValue() {
            return 12;
        }
    };

    private String codeName;

    private IacucStatus(String s) {
        this.codeName = s;
    }

    private static final Set<String> KaputSet = new HashSet<String>();
    static {
        KaputSet.add("Release");
        KaputSet.add("UnRelease");
        KaputSet.add("Reject");
        KaputSet.add("Notify");
        //
        KaputSet.add("PreApprove");
        KaputSet.add("FullReviewReq");
        //
        KaputSet.add("ChgApprovalDate");
        KaputSet.add("ChgEffectivDate");
        KaputSet.add("ChgEndDate");
        KaputSet.add("ChgMeetingDate");
        //
        KaputSet.add("FullReviewReq");
        //
        KaputSet.add("HazardsApprove");
        //
        KaputSet.add("SOPreApproveA");
        KaputSet.add("SOPreApproveB");
        KaputSet.add("SOPreApproveC");
        KaputSet.add("SOPreApproveD");
        KaputSet.add("SOPreApproveE");
        KaputSet.add("SOPreApproveF");
        KaputSet.add("SOPreApproveG");
        KaputSet.add("SOPreApproveI");
        KaputSet.add("SOHoldA");
        KaputSet.add("SOHoldB");
        KaputSet.add("SOHoldC");
        KaputSet.add("SOHoldD");
        KaputSet.add("SOHoldE");
        KaputSet.add("SOHoldF");
        KaputSet.add("SOHoldG");
        KaputSet.add("SOHoldI");
        //
        KaputSet.add("VetPreApproveB");
        KaputSet.add("VetPreApproveC");
        KaputSet.add("VetPreApproveE");
        KaputSet.add("VetPreApproveF");
        KaputSet.add("VetHoldB");
        KaputSet.add("VetHoldC");
        KaputSet.add("VetHoldE");
        KaputSet.add("VetHoldF");
    }

    public String statusName() {
        return codeName;
    }

    public boolean isKaput(String name) {
        return KaputSet.contains(name);
    }

    public abstract String taskDefKey();

    public abstract boolean isDefKey(String def);

    public abstract boolean isStatus(String status);

    public abstract int startGatewayValue();
}
