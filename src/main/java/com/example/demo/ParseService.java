package com.example.demo;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.PatternMatchUtils;

@Service
public class ParseService {

    private static List<String> words = new ArrayList<>();

    @SneakyThrows
    //@EventListener(ApplicationStartedEvent.class)
    public void parsePdf() {
        ClassPathResource cpr = new ClassPathResource("1.pdf");
        PdfReader reader = new PdfReader(cpr.getInputStream());
        int pages = reader.getNumberOfPages();

        List<String> pageText = new ArrayList<>();
        IntStream.range(1, pages + 1).forEach(i -> {
            try {
                pageText.add(PdfTextExtractor.getTextFromPage(reader, i));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        List<String> words = pageText.stream().flatMap(p -> Arrays.stream(p.split(" ")))
            .peek(System.out::println)
            .toList();
    }

    @SneakyThrows
    @EventListener(ApplicationStartedEvent.class)
    public void parseDocx() {
        for (int i = 1; i <= 7; i++) {
            ClassPathResource cpr = new ClassPathResource(i + ".docx");
            XWPFDocument doc = new XWPFDocument(cpr.getInputStream());
            words.addAll(doc.getParagraphs().stream()
                .flatMap(p -> p.getRuns().stream())
                .map(r -> r.getText(0))
                .filter(s -> s != null && !s.isEmpty() && !s.isBlank())
                .flatMap(p -> Arrays.stream(p.split(" ")))
                .map(w -> w.replaceAll("[^а-яА-Яa-zA-z0-9]", ""))
                .filter(s -> !s.isEmpty() && !s.isBlank())
                .map(String::toLowerCase)
                .toList());
        }
        System.out.println("Слов: " + words.size());
        getResult();
    }

    public List<String> getResult() {
        Pattern pattern = Pattern.compile("[а-я0-9 ]{4}е[а-я0-9 ]{13}");
        List<String> letters = List.of("ч","д");
        int length = 18;
        List<String> wordPairs = new ArrayList<>();
        String temp = "";
        for (String word : words) {
            String concat = word.concat(" ").concat(temp);
            if (concat.length() == length) wordPairs.add(concat);
            temp = word;
        }

        wordPairs.addAll(words);
        System.out.println("Result:");

        List<String> result = wordPairs.stream()
            .distinct()
            .filter(w -> w.length() == length)
            .filter(w -> pattern.matcher(w).matches())
            .filter(w -> {
                boolean containsAll = true;
                for (String letter : letters) {
                    if (!w.contains(letter)) {
                        containsAll = false;
                        break;
                    }
                }
                return containsAll;
            })
            .peek(System.out::println)
            .toList();
        System.out.println("finished");
        return result;
    }
}
