package mpi.aida.preparator.inputformat.sgml;

import java.util.ArrayList;

import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.data.ExternalEntitiesContext;
import mpi.aida.data.PreparedInput;
import mpi.aida.preparator.Preparator;
import mpi.aida.preparator.inputformat.PreparatorInputFormatInterface;


/**
 * @author Dhruv Gupta (dhgupta@mpi-inf.mpg.de)
 * 
 * The following SGML extractor will preapre the input for AIDA for disambiguation
 * 
 */
public abstract class SgmlPreparatoryInputFormat implements PreparatorInputFormatInterface {


	/**
	 * 
	 * @author Dhruv Gupta (dhgupta@mpi-inf.mpg.de)
	 * 
	 * Encodes the different elements of a SGML document
	 *
	 */
	public class SgmlDoc{
		private String docId  ;
		private String text   ;
		private String docType;
		private String pubDate;

		/* Getter and setters */
		public String getDocId() {
			return docId;

		}
		public void setDocId(String docId) {
			this.docId = docId;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;	    
		}
		public String getDocType() {
			return docType;
		}
		public void setDocType(String docType) {
			this.docType = docType;
		}
		public String getPubDate() {
			return pubDate;
		}
		public void setPubDate(String pubDate) {
			this.pubDate = pubDate;
		}

		// Debug code to print the doc
		public void printDoc(){
			System.out.println("DocId    : " + this.getDocId()  );
			System.out.println("Doc Type : " + this.getDocType());
			System.out.println("Pub Date : " + this.getPubDate());
			System.out.println("Text     : " + this.getText()   );
		}

		@Override
		public String toString() {
			return(this.getDocId()   + "\\n" +
					this.getDocType() + "\\n" + 
					this.getPubDate() + "\\n" +
					this.getText());
		}
	}



	/*
	 * (non-Javadoc)
	 * @see mpi.aida.preparator.inputformat.PreparatorInputFormatInterface#prepare(java.lang.String, java.lang.String, mpi.aida.config.settings.PreparationSettings, mpi.aida.data.ExternalEntitiesContext)
	 */
	@Override
	public PreparedInput prepare(String docId, 
								 String text,
								 PreparationSettings prepSettings,
								 ExternalEntitiesContext externalContext) throws Exception {

		SgmlDoc sgmlDoc = parseSgml(text);

		PreparedInput pInp = Preparator.prepareInputData( sgmlDoc.getText()
				,sgmlDoc.getDocId()
				,externalContext
				,prepSettings);
		return pInp;
	}

	/*
	 * Extract the different elements from the 
	 * SGML text
	 */
	public abstract SgmlDoc parseSgml(String sgmlText);
}
