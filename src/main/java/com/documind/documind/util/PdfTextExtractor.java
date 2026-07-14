package com.documind.documind.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;


public class PdfTextExtractor {

    public static Map<Integer, String> extractPageWiseText(String filePath) throws Exception {
        Map<Integer, String> pageTextMap = new LinkedHashMap<>();

        try (PDDocument pdf = Loader.loadPDF(new File(filePath))) {
            int totalPages = pdf.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();

            System.out.println("Total pages in PDF: " + totalPages);

            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(pdf);

                if (pageText == null || pageText.trim().isEmpty()) {
                    continue;
                }


                pageText = pageText.replace("\u2022", "\n\u2022");

                pageTextMap.put(pageNum, pageText);

                if (pageNum % 50 == 0) {
                    System.out.println("Extracted " + pageNum + "/" + totalPages + " pages...");
                }
            }
        }

        return pageTextMap;
    }
}
