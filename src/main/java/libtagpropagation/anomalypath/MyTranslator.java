package libtagpropagation.anomalypath;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.modality.nlp.DefaultVocabulary;
import ai.djl.modality.nlp.Vocabulary;
import ai.djl.modality.nlp.bert.BertToken;
import ai.djl.modality.nlp.bert.BertTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class MyTranslator implements Translator<String, Float> {
    private List<String> tokens;
    private Vocabulary vocabulary;
    private HuggingFaceTokenizer tokenizer;

    @Override
    public void prepare(TranslatorContext ctx) throws IOException {
        Path path = Paths.get("src/main/resources/bert-base-cased-vocab.txt");
        vocabulary = DefaultVocabulary.builder()
                .optMinFrequency(1)
                .addFromTextFile(path)
                .optUnknownToken("[UNK]")
                .build();
        tokenizer = HuggingFaceTokenizer.newInstance("sentence-transformers/msmarco-distilbert-dot-v5");
    }

    @Override
    public NDList processInput(TranslatorContext ctx, String input) throws IOException {
        Encoding token = tokenizer.encode(input.toLowerCase());

        // get the encoded tokens that would be used in processOutput
        tokens = Arrays.asList(token.getTokens());
        NDManager manager = ctx.getNDManager();
        // map the tokens(String) to indices(long)
        long[] indices = token.getIds();
        long[] attentionMask = token.getAttentionMask();
//        long[] indices = tokens.stream().mapToLong(vocabulary::getIndex).toArray();
//        long[] attentionMask = token.getAttentionMask().stream().mapToLong(i -> i).toArray();
//        long[] tokenType = token.getTokenTypes().stream().mapToLong(i -> i).toArray();
        NDArray indicesArray = manager.create(indices);
        NDArray attentionMaskArray =
                manager.create(attentionMask);
//        NDArray tokenTypeArray = manager.create(tokenType);
//        // The order matters
//        return new NDList(indicesArray, attentionMaskArray, tokenTypeArray);
        return new NDList(indicesArray, attentionMaskArray);
    }

    @Override
    public Float processOutput(TranslatorContext ctx, NDList list) {
        NDArray result = list.get(0);
        float[] result1 = result.toFloatArray();
//        return tokenizer.tokenToString(tokens.subList(startIdx, endIdx + 1));
        return result1[0];
    }

    @Override
    public Batchifier getBatchifier() {
        return Batchifier.STACK;
    }
}
