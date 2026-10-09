package com.study.assistant.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmbeddingService {
    public static final int DIM = 384;

    public float[] embed(String text) {
        float[] vector = new float[DIM];
        Map<String, Integer> tf = new LinkedHashMap<>();
        for (String token : tokenize(text)) {
            tf.merge(token, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : tf.entrySet()) {
            int index = Math.floorMod(entry.getKey().hashCode(), DIM);
            vector[index] += (float) (1.0 + Math.log(entry.getValue()));
        }
        float norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    public float cosine(float[] left, float[] right) {
        int n = Math.min(left.length, right.length);
        float dot = 0;
        for (int i = 0; i < n; i++) {
            dot += left[i] * right[i];
        }
        return dot;
    }

    public List<String> importantTokens(String text) {
        return tokenize(text).stream().filter(token -> token.length() >= 2).distinct().toList();
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        StringBuilder latin = new StringBuilder();
        StringBuilder cjk = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isCjk(ch)) {
                flushLatin(latin, tokens);
                cjk.append(ch);
            } else if (Character.isLetterOrDigit(ch)) {
                flushCjk(cjk, tokens);
                latin.append(ch);
            } else {
                flushLatin(latin, tokens);
                flushCjk(cjk, tokens);
            }
        }
        flushLatin(latin, tokens);
        flushCjk(cjk, tokens);
        return tokens;
    }

    private void flushLatin(StringBuilder latin, List<String> tokens) {
        if (latin.length() >= 2) {
            tokens.add(latin.toString().toLowerCase(Locale.ROOT));
        }
        latin.setLength(0);
    }

    private void flushCjk(StringBuilder cjk, List<String> tokens) {
        String value = cjk.toString();
        for (int i = 0; i < value.length(); i++) {
            tokens.add(String.valueOf(value.charAt(i)));
            if (i + 1 < value.length()) {
                tokens.add(value.substring(i, i + 2));
            }
        }
        cjk.setLength(0);
    }

    private boolean isCjk(char ch) {
        return Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN;
    }
}
