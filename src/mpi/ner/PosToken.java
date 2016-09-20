package mpi.ner;

import mpi.aida.util.nlp.ProperNounManager;

public class PosToken {
	private String token;
	private String originalEnd;
	private int start;
	private int length;
	private String posTag;
	private String posAnnotatorId;
	private double score;

	public PosToken(String token) {
		super();
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getOriginalEnd() {
		return originalEnd;
	}

	public void setOriginalEnd(String originalEnd) {
		this.originalEnd = originalEnd;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getPosTag() {
		return posTag;
	}

	public void setPosTag(String posTag) {
		this.posTag = posTag;
	}

	public String getPosAnnotatorId() {
		return posAnnotatorId;
	}

	public void setPosAnnotatorId(String posAnnotatorId) {
		this.posAnnotatorId = posAnnotatorId;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public boolean isProperNoun() {
		return ProperNounManager.singleton().isProperNounTag(getPosTag());
	}
	
	@Override
	public String toString() {
		return token + ":" + posTag + ":" + score; 
	}

}
