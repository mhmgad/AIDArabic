package mpi.ner.taggers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import mpi.aida.config.AidaConfig;
import mpi.ner.NER;
import mpi.ner.NERUtil;
import mpi.ner.Name;
import mpi.ner.PosToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class StanfordPOS implements NER {
	
	private static final Logger logger = LoggerFactory
			.getLogger(StanfordPOS.class);

	private StanfordCoreNLP stanfordCoreNLP = null;

  private final String GERMAN_POS_HGC =
			"edu/stanford/nlp/models/pos-tagger/german/german-hgc.tagger";
  
	public StanfordPOS() {
		logger.info("Initilaizing Stanford POS");
		Properties props = new Properties();
		
		switch(AidaConfig.getLanguage()) {
		  case en:
		    props.put("annotators", "tokenize, ssplit, pos, lemma");
		    break;
		  case de:
        props.put("annotators", "tokenize, ssplit, pos");
        props.put("pos.model", GERMAN_POS_HGC);
		    break;
		  default:
		      break;
		}
		
		stanfordCoreNLP = new StanfordCoreNLP(props, true);
	}

	@Override
	public List<Name> findNames(String docId, String text) {
		if (text.trim().length() == 0) {
			return new LinkedList<Name>();
		}

		List<PosToken> posTokens = new ArrayList<>();
		Annotation document = new Annotation(text);
		stanfordCoreNLP.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int start = 0, end = 0;
		PosToken previousToken = null;
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String pos = token.get(PartOfSpeechAnnotation.class);
				start = token.get(CharacterOffsetBeginAnnotation.class);
				end = token.get(CharacterOffsetEndAnnotation.class);

				PosToken posToken = new PosToken(text.substring(start, end));
				posToken.setStart(start);
				posToken.setPosTag(pos);
				posToken.setLength(end - start);
				posToken.setPosAnnotatorId(getId());
				
				posTokens.add(posToken);

				if (previousToken != null) {
					int spacesStartIndex = previousToken.getStart() + previousToken.getLength();
					String spaces = text.substring(spacesStartIndex, start);
					posToken.setOriginalEnd(spaces);
				}
				previousToken = posToken;
			}
		}
		
		return NERUtil.findNamesFromPOS(posTokens);
	}

	@Override
	public String getId() {
		return "StanfordPOS";
	}

}
