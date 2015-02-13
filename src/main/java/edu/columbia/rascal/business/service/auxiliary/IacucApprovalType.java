package edu.columbia.rascal.business.service.auxiliary;

public enum IacucApprovalType {

	// used by designated reviewer as well as ADMIN review
	APPROVE("APPROVE") {
		@Override
		public boolean isType(String type) {
			return typeCode().equals(type);
		}

		@Override
		public int getGatewayValue() {
			return 1;
		}
	},
	
	// used by ADMIN review
	RETURNTOPI("ReturnToPI") {
		@Override
		public boolean isType(String type) {
			return typeCode().equals(type);
		}

		@Override
		public int getGatewayValue() {
			return 2;
		}
	},

	// used by ADMIN review
	REDISTRIBUTE("Redistribute") {
		@Override
		public boolean isType(String type) {
			return typeCode().equals(type);
		}

		@Override
		public int getGatewayValue() {
			return 3;
		}
	},

	// used by designated reviewer and ADMIN review
	FULLCOMMITTEE("Full Committee Review") {
		@Override
		public boolean isType(String type) {
			return typeCode().equals(type);
		}

		@Override
		public int getGatewayValue() {
			return 4;
		}
	},

	// used by designated reviewer
	HOLD("HOLD") {
		@Override
		public boolean isType(String type) {
			return typeCode().equals(type);
		}

		@Override
		public int getGatewayValue() {
			// undefined currently in diagram
			return 0;
		}
	},


	TERMINATE("Terminate") {
		@Override
		public boolean isType(String type) {
			return typeCode().equals(type);
		}

		@Override
		public int getGatewayValue() {
			// currently undefined in diagram
			return 0;
		}
	},

	SUSPEND("Suspend") {
		@Override
		public boolean isType(String type) {
			return typeCode().equals(type);
		}

		@Override
		public int getGatewayValue() {
			// currently undefined
			return 0;
		}
	},

	WITHDRAW("Withdraw") {
		@Override
		public boolean isType(String type) {
			return typeCode().equals(type);
		}

		@Override
		public int getGatewayValue() {
			// currently undefined
			return 0;
		}
	};

	private String type;
	private IacucApprovalType(String type) {
		this.type=type;
	}
	
	public String typeCode() {
		return this.type;
	}
	
	public abstract boolean isType(String type);
	public abstract int getGatewayValue();
}
