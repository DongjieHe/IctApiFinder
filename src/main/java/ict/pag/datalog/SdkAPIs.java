package ict.pag.datalog;

import java.util.Set;

public class SdkAPIs {
	private int mApiLevel;
	private Set<String> mTypeSigs;
	private Set<String> mMethodSigs;
	private Set<String> mFieldSigs;

	public SdkAPIs(LBWorkspaceConnector conn) {
		this.mApiLevel = conn.getSdkVersion();
		this.mTypeSigs = FactsLoader.getApplicationTypes(conn);
		this.mMethodSigs = FactsLoader.getApplicationMethod(conn);
		this.mFieldSigs = FactsLoader.getApplicationFields(conn);
	}

	public boolean containType(String sig) {
		return this.mTypeSigs.contains(sig);
	}

	public boolean containMethod(String sig) {
		return this.mMethodSigs.contains(sig);
	}

	public boolean containField(String sig) {
		return this.mFieldSigs.contains(sig);
	}

	public boolean containAPI(String apiSig, int apiType) {
		if (apiType == 0) {
			return containType(apiSig);
		} else if (apiType == 1) {
			return containMethod(apiSig);
		} else if (apiType == 2) {
			return containField(apiSig);
		} else {
			assert false;
			return false;
		}
	}

	public Set<String> getMethodSigs() {
		return mMethodSigs;
	}

	public Set<String> getFieldSigs() {
		return mFieldSigs;
	}

	public int getmApiLevel() {
		return mApiLevel;
	}

	public void dump() {
		System.out.println("API LEVEL: " + this.mApiLevel);
		System.out.println("TYPE SIZE: " + this.mTypeSigs.size());
		System.out.println("METHOD SIZE: " + this.mMethodSigs.size());
		System.out.println("FIELD SIZE: " + this.mFieldSigs.size());
	}
}
