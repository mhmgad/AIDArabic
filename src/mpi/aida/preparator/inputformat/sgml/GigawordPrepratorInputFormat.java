package mpi.aida.preparator.inputformat.sgml;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GigawordPrepratorInputFormat extends SgmlPreparatoryInputFormat {
		// Gigaword SGML tags
		public static final String DOC_END_TAG  = "</DOC>"; 
		
		// Gigaword element regex - DTD
		
		// 0. Tags
		public static final String HEADLINE_OPEN_TAG  = "<HEADLINE>"  ;
		public static final String HEADLINE_CLOSE_TAG = "</HEADLINE>" ;
		public static final String DATELINE_OPEN_TAG  = "<DATELINE>"  ;
		public static final String DATELINE_CLOSE_TAG = "</DATELINE>" ;
		public static final String TEXT_OPEN_TAG      = "<TEXT>"      ;
		public static final String TEXT_CLOSE_TAG     = "</TEXT>"     ;
		public static final String P_OPEN_REGEX       = "<P>"         ;
		public static final String P_CLOSE_REGEX      = "</P>"        ;
		
		// 1. DOC_LINE related regex
		public static final String DOC_LINE_REGEX = "<DOC(.*?)>"         ;
		public static final String DOC_ID_REGEX   = "id=\"(.*?)\""       ;
		public static final String DOC_TYPE_REGEX = "type=\"(.*?)\""     ;
		public static final String PUB_DATE_REGEX = "^*[_]{1,2}(.*?)\\." ;
		// 2. TEXT
		public static final String TEXT_REGEX     = TEXT_OPEN_TAG + "(.*?)" + TEXT_CLOSE_TAG;
		// 3. HEADLINE
		public static final String HEADLINE_REGEX = HEADLINE_OPEN_TAG + "(.*?)" + HEADLINE_CLOSE_TAG;
		// 4. DATELINE
		public static final String DATELINE_REGEX = DATELINE_OPEN_TAG + "(.*?)" + DATELINE_CLOSE_TAG;	
		
		
		@Override
		public SgmlDoc parseSgml(String sgmlText) {

			SgmlDoc            tempDoc         = null;
			String             tempString      = null;
			String             headline        = null;
			String             dateline        = null;
			String             text            = null;

			// For matching regex
			Pattern pattern = null;
			Matcher matcher = null;

			tempDoc =  new SgmlDoc();

			// 1. Get the DOC_LINE 
			pattern = Pattern.compile(DOC_LINE_REGEX);
			matcher = pattern.matcher(sgmlText);
			if (matcher.find()){
				tempString = matcher.group(1);
				// 1.1 Get the DOC_ID from DOC_LINE
				pattern = Pattern.compile(DOC_ID_REGEX);
				matcher = pattern.matcher(tempString);
				if(matcher.find()){tempDoc.setDocId(matcher.group(1));}
				// 1.2 Get the DOC_TYPE from DOC_LINE
				pattern = Pattern.compile(DOC_TYPE_REGEX);
				matcher = pattern.matcher(tempString);
				if(matcher.find()){tempDoc.setDocType(matcher.group(1));}
			}  

			// 1.3 Set the pub date from the docId
			pattern = Pattern.compile(PUB_DATE_REGEX);
			if(tempDoc.getDocId()!=null){
				matcher = pattern.matcher(tempDoc.getDocId());
				if(matcher.find()){
					tempString = matcher.group(1).split("_")[1];
					tempString =   tempString.substring(0, 4) 
							+ "-" 
							+ tempString.substring(4,6) 
							+ "-" 
							+ tempString.substring(6,8);
					tempDoc.setPubDate(tempString);
				}}else{
					tempDoc.setPubDate(null);
				}

			// 2. Get the TEXT
			text = new String();
			// 2.1 Get the headline
			pattern = Pattern.compile(HEADLINE_REGEX, Pattern.DOTALL);
			matcher = pattern.matcher(sgmlText);
			while (matcher.find()){
				headline   = matcher.group(1);

				// Clean the text
				headline   = headline.replaceAll(P_OPEN_REGEX, " ");
				headline   = headline.replaceAll(P_CLOSE_REGEX, " ");

				text       = text.concat(headline);
			}


			// 2.2 Get the dateline
			pattern = Pattern.compile(DATELINE_REGEX, Pattern.DOTALL);
			matcher = pattern.matcher(sgmlText);
			while (matcher.find()){
				dateline   = matcher.group(1);
				text       = text.concat(dateline);
			}

			// 2.3 Get the text
			pattern = Pattern.compile(TEXT_REGEX, Pattern.DOTALL);
			matcher = pattern.matcher(sgmlText);
			while (matcher.find()){
				tempString = matcher.group(1);
				// Clean the text
				tempString = tempString.replaceAll(P_OPEN_REGEX  , " ");
				tempString = tempString.replaceAll(P_CLOSE_REGEX , " ");

				text       = text.concat(tempString);
			}

			tempDoc.setText(text);

			return tempDoc;
		}

}
