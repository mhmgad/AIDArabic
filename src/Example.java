import mpi.aida.Disambiguator;
import mpi.aida.access.DataAccess;
import mpi.aida.config.settings.DisambiguationSettings;
import mpi.aida.config.settings.PreparationSettings;
import mpi.aida.config.settings.disambiguation.CocktailPartyDisambiguationSettings;
import mpi.aida.config.settings.preparation.ManualPreparationSettings;
import mpi.aida.data.*;
import mpi.aida.preparator.Preparator;
import mpi.tokenizer.data.Token;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by gadelrab on 9/14/16.
 */
public class Example {


    public static void main(String[] args) {
        // Define the input.
        String inputText = "وُلد [[ألبرت أينشتاين]] في مدينة [[أولم]] الألمانية في 14 مارس 1879 لأبوين يهوديين وأمضى سِن يفاعته في [[ميونخ]]. كان أبوه [[هيرمان أينشتاين]] يعمل في بيع الرّيش المستخدم في صناعة الوسائد";
        //Albert Einstein was born in Ulm, in the Kingdom of Württemberg in the German Empire on 14 March 1879.His father was Hermann Einstein, a salesman and engineer. His mother was Pauline Einstein (née Koch).";
        //String inputText ="يقع [[معهد ماكس بلانك]] في [[ألمانيا]]";
//            String inputText = "ولد [[لتلجهن]] في [[الولايات المتحدة]] لأب"; //newly discovered
        //String inputText ="ولد [[لتلجهن]] في [[إسكتلندا]] ";
        //String inputText= "ولد [[أبو جبل]] في [[قطاع غزة]] "; // Qetaa Gazza was not recognized before

        //String inputText ="في الرابع من يوليو - تموز من عام 1776حصل ممثلو مقاطعات [[الكونغرس]] على وثيقة استقلال [[الولايات المتحدة الأمربكيّة]]. أعلنت المقاطعات في هذه الوثيقة عن استقلالها عن [[بريطانيا]]. أقر الممثلون في هذه الوثيقة القواعد الّتي ما زالت تحدّد طابع [[الولايات المتحدة]] حتى يومنا هذا. كُتب في الوثيقة أنّ [[الولايات المتحدة]] تكون مأسّسة على الإيمان بأن \"جميع بني البشر ولدوا متساوين\" ووظيفة الحكومة الحفاظ على حقوق مواطنيها. في هذا الكونغرس انتُخب [[جورج واشنطن]] قائد أعلى [[لجيش الولايات المتحدة]] وبعدها انتُخب كأول رئيس لهذه الدولة. يوم قبول وثيقة الإستقلال أعلِن [[كيوم استقلال الولايات المتحدة]] وما زال يحتَفل [[بوم الإستقلال]] في هذا التاريخ حتى يومنا هذا. استمرّت [[حرب الإستقلال الأمريكيّة]] حتى عام 1781 واعترفت [[بريطانيا]] عامين بعد ذلك [[بالولايات المتحدة]] كدولة مستقلة.";
        //String inputText ="في الرابع من يوليو - تموز من عام 1776حصل ممثلو مقاطعات [[الكونغرس]] على وثيقة استقلال [[الولايات المتحدة الأمربكيّة]]. أعلنت المقاطعات في هذه الوثيقة عن استقلالها عن [[بريطانيا]]. أقر الممثلون في هذه الوثيقة القواعد الّتي ما زالت تحدّد طابع [[الولايات المتحدة]] حتى يومنا هذا. كُتب في الوثيقة أنّ [[الولايات المتحدة]] تكون مأسّسة على الإيمان بأن \"جميع بني البشر ولدوا متساوين\" ووظيفة الحكومة الحفاظ على حقوق مواطنيها. في هذا الكونغرس انتُخب [[جورج واشنطن]] قائد أعلى [[لجيش الولايات المتحدة]] وبعدها انتُخب كأول رئيس لهذه الدولة. يوم قبول وثيقة الإستقلال أعلِن [[كيوم استقلال الولايات المتحدة]] وما زال يحتَفل [[بوم الإستقلال]] في هذا التاريخ حتى يومنا هذا. استمرّت [[حرب الإستقلال الأمريكيّة]] حتى عام 1781 واعترفت [[بريطانيا]] عامين بعد ذلك ب+ [[الولايات المتحدة]] ك+ دولة مستقلة.";

        //String inputText ="[[ستيفن]] هوكينج ولد في [[أكسفورد]]، [[إنجلترا]]. درس في [[جامعة أكسفورد]] وحصل منها على درجة الشرف الأولى في الفيزياء، أكمل دراسته في [[جامعة كامبريدج]]. [[هوكينغ]] كان قد أصيب بمرض عصبي نادر في الـ 21 من عمره، يسمى [[مرض تصلب العضلات الجانبي الضموري]] ALS، وهو مرض مميت لا علاج له، وأعلن الأطباء آنذاك أنه لن يعيش أكثر من سنتين، ومع ذلك قاوم المرض حتى تجاوز عمره الـ 70 عاماً، وهو أمد أطول بكثير مما توقعه الأطباء. ومنذ 3 سنوات، طلب [[هوكينغ]] من شركة [[إنتل]] المساعدة. في تلك المرحلة، كانت سرعة كتابته انخفضت إلى كلمة واحدة فقط في الدقيقة، مما جعل الأمر أكثر صعوبة على التواصل من أي وقت مضى.";

//		String inputText="تحسنت عائدات فريق [[ريال مدريد]] المادية بعد تسجيل لاعبه كريستيانو [[رونالدو]] أربعة أهداف في المباراة الأخيرة ضد فريق [[شالكه]] الألماني";
        //String inputText="Albert Einstein appears in  all our examples";
        //String inputText="كان [[هيلموت كول]] مستشار [[ألمانيا الإتحادية]] (1982ـ1998) واحد اهم الشخصيات الالمانية والاوروبية ما بعد [[الحرب العالمية]] الثانية . في عهده تحققت الوحدة الالمانية عام 1990 ولذلك يلقب بمستشار الوحدة. ولد [[كول]] عام 1930 في مدينة [[لودفيغسهافن]] ويحمل شهادة الدكتورا في التاريخ والقانون. وهو ينتمي [[الحزب المسيحي الديمقراطي]] الذي ظل رئيسا له من 1976 حتى هزيمته في الانتخابات الالمانية العامة امام [[غيرهارد شرودر]] 1998.";
        // Prepare the input for disambiguation. The Stanford NER will be run
        // to identify names. Strings marked with [[ ]] will also be treated as
        // names.

//
        //String inputText = "وقع الرئيس [[الروسي]] [[فلاديمير بوتين]] الاثنين مرسوما يلغي حظر تسليم [[إيران]] صواريخ أس300 الذي كان الرئيس السابق ديمتري [[مدفيدف]] أصدره في 2010";
        //String inputText = "[[Albert Einstein]] was born in [[Ulm]]";
        PreparationSettings prepratorSettings = new ManualPreparationSettings();

        Preparator p = new Preparator();
        PreparedInput input;
        try {
            input = p.prepare(inputText, prepratorSettings);

//		// Disambiguate the input with the graph coherence algorithm.
            DisambiguationSettings disSettings;

            disSettings = new CocktailPartyDisambiguationSettings();

            Disambiguator d = new Disambiguator(input, disSettings);
            DisambiguationResults results;

            results = d.disambiguate();


            // Print the disambiguation results.
            for (ResultMention rm : results.getResultMentions()) {
                ResultEntity re = results.getBestEntity(rm);
                System.out.println(rm.getMention() + " -> " + re);
            }

            Set<KBIdentifiedEntity> entities = new HashSet<KBIdentifiedEntity>();
            for (ResultMention rm : results.getResultMentions()) {
                entities.add(results.getBestEntity(rm).getKbEntity());
            }

            Map<KBIdentifiedEntity, EntityMetaData> entitiesMetaData =
                    DataAccess.getEntitiesMetaData(entities);

            for (ResultMention rm : results.getResultMentions()) {
                KBIdentifiedEntity entity = results.getBestEntity(rm).getKbEntity();
                EntityMetaData entityMetaData = entitiesMetaData.get(entity);

                if (Entities.isOokbEntity(entity)) {
                    System.out.println("\t" + rm + "\t NO MATCHING ENTITY");
                } else {
                    System.out.println("\t" + rm + "\t" + entityMetaData.getId() + "\t"
                            + entity + "\t" + entityMetaData.getHumanReadableRepresentation()
                            + "\t" + entityMetaData.getUrl());
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
