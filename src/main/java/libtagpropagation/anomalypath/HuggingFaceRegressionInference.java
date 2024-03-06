package libtagpropagation.anomalypath;

import ai.djl.ModelException;
import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.qa.QAInput;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;

import java.io.IOException;
import java.nio.file.Paths;

public class HuggingFaceRegressionInference {
    public static HuggingFaceTokenizer tokenizer;

    public static MyTranslator translator;
    public static Criteria<ai.djl.ndarray.NDList, ai.djl.ndarray.NDList> criteria;

    public static ZooModel model;

    public HuggingFaceRegressionInference() throws IOException, ModelException {
        tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("./src/main/resources/my-event-tokenizer/tokenizer.json"));

        translator = new MyTranslator();
        criteria = Criteria.builder()
                .setTypes(ai.djl.ndarray.NDList.class, ai.djl.ndarray.NDList.class)
                .optModelPath(Paths.get("src/main/resources/e1.pt"))
                .optProgress(new ProgressBar()).build();

        model = criteria.loadModel();
    }

//    public static void main(String[] args) throws IOException, TranslateException, ModelException {
////        String question = "When did BBC Japan start broadcasting?";
//        String question = "bash read /etc/passwd";
//        float freq_predict = HuggingFaceRegressionInference.freq_predict(question);
//        System.out.println("The result is: \n" + freq_predict);
//    }

    //    public static float freq_predict(String input) throws IOException, TranslateException, ModelException {
//        Encoding token = tokenizer.encode(input.toLowerCase());
//
////        ZooModel<String, Float> model = criteria.loadModel();
//        try (Predictor<String, Float> predictor = model.newPredictor(translator)) {
//            return predictor.predict(input);
//        }
//    }
    public float freq_predict(String input) throws IOException, TranslateException, ModelException {
        Encoding token = tokenizer.encode(input.toLowerCase());

//        ZooModel<String, Float> model = criteria.loadModel();
        try (Predictor<String, Float> predictor = model.newPredictor(translator)) {
            return predictor.predict(input);
        }
    }
}
