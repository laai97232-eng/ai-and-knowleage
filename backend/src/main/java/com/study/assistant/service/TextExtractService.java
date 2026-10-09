package com.study.assistant.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Slf4j
@Service
public class TextExtractService {

    public String extract(Path path, String fileType) {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        String ext = fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
        try {
            return switch (ext) {
                case "txt", "md", "markdown" -> Files.readString(path, StandardCharsets.UTF_8);
                case "pdf" -> pdf(path);
                case "docx" -> docx(path);
                case "doc" -> doc(path);
                case "pptx" -> pptx(path);
                case "ppt" -> ppt(path);
                default -> "";
            };
        } catch (Exception ex) {
            log.warn("解析资料失败: {}", path, ex);
            return "";
        }
    }

    private String pdf(Path path) throws Exception {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String docx(Path path) throws Exception {
        try (InputStream in = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(in)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append('\n');
            }
            return text.toString();
        }
    }

    private String doc(Path path) throws Exception {
        try (InputStream in = Files.newInputStream(path); HWPFDocument document = new HWPFDocument(in); WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String pptx(Path path) throws Exception {
        try (InputStream in = Files.newInputStream(path); XMLSlideShow show = new XMLSlideShow(in)) {
            StringBuilder text = new StringBuilder();
            show.getSlides().forEach(slide -> {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        text.append(textShape.getText()).append('\n');
                    }
                }
            });
            return text.toString();
        }
    }

    private String ppt(Path path) throws Exception {
        try (InputStream in = Files.newInputStream(path); HSLFSlideShow show = new HSLFSlideShow(in)) {
            StringBuilder text = new StringBuilder();
            show.getSlides().forEach(slide -> {
                for (var shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        text.append(textShape.getText()).append('\n');
                    }
                }
            });
            return text.toString();
        }
    }
}
