package com.felixkroemer.analyzer;

import com.felixkroemer.analyzer.ai.OAIService;
import com.felixkroemer.analyzer.result.PDFAnalysisResult;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
public class PDFAnalyzer implements FileAnalyzer<PDFAnalysisResult> {

    private final OAIService oaiService;

    @Inject
    public PDFAnalyzer(OAIService oaiService) {
        this.oaiService = oaiService;
    }

    public PDFAnalysisResult analyze(File f) {
        try (var is = new FileInputStream(f)) {
            PdfReader reader = new PdfReader(is);
            var analyzableContent = getContentForNameAnalysis(reader);
            var analyzedFileName = oaiService.analyzeFileName(analyzableContent);
            log.info("Analyzed file name for file {}: {}", f.getAbsolutePath(), analyzedFileName);
            return new PDFAnalysisResult(f.getAbsolutePath(), f.getName(), f.getTotalSpace(), LocalDateTime.now(), reader.getNumberOfPages(), analyzedFileName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze pdf file: " + f.getAbsolutePath(), e);
        }
    }

    private String getContentForNameAnalysis(PdfReader reader) throws IOException {
        PdfTextExtractor pdfTextExtractor = new PdfTextExtractor(reader);
        return pdfTextExtractor.getTextFromPage(1);
    }
}
