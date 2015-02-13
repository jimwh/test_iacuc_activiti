package edu.columbia.rascal.business.service.auxiliary;

public enum IacucReviewType {

	FullIacucCommittee("Full IACUC Committee") {
		@Override
		public boolean isType(String str) {
			return typeCode().equals(str);
		}
		@Override
		public int getGatewayValue() {
			return 1;
		}
	},
	
	SubIacucCommitte("Sub-Committee") {
		@Override
		public boolean isType(String str) {
			return typeCode().equals(str);
		}
		@Override
		public int getGatewayValue() {
			return 2;
		}
	},

	DesignatedReviewers("Designated Reviewers") {
		@Override
		public boolean isType(String str) {
			return typeCode().equals(str);
		}
		@Override
		public int getGatewayValue() {
			return 3;
		}
	};

	private String type;

	private IacucReviewType(String type) {
		this.type = type;
	}

	public String typeCode() {
		return this.type;
	}

	public abstract boolean isType(String type);
	public abstract int getGatewayValue();
}
