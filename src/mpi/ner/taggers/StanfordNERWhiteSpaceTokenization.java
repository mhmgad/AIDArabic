package mpi.ner.taggers;

import java.util.Properties;

public class StanfordNERWhiteSpaceTokenization extends StanfordNER {
	
	@Override
	protected Properties buildProperties() {

		Properties props = super.buildProperties();
        // separates words only when whitespace is encountered.
        props.put("tokenize.whitespace", "true");
        // split sentences on newlines. Works well in conjunction with 
        // "-tokenize.whitespace true", in which case StanfordCoreNLP will treat 
        // the input as one sentence per line, only separating words on whitespace.
        props.put("ssplit.eolonly", "true");
        
        return props;
	}

}
