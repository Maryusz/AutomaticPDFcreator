package com.mariuszbilda;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PDFManager {
    private PDDocument doc = null;
    private Logger logger;
    private int pageNumber;

    public PDFManager() {
        doc = new PDDocument();
        logger = Logger.getLogger("PDF MANAGER");
        pageNumber = 0;
    }

    /**
     * https://gist.github.com/mmdemirbas/209b4bdb66b788e785266f97204b8631
     * This method take a path of one image and add it to the PDF document.
     * @param file image to add
     */

    public void addPage(File file) {

        pageNumber++;
        try {
            PDImageXObject pdImageXObject = PDImageXObject.createFromFileByContent(file, doc);
            PDRectangle pageSize = PDRectangle.A4;

            int originalWidth = pdImageXObject.getWidth();
            int originalHeight = pdImageXObject.getHeight();

            logger.log(Level.INFO, String.format("Original sizes: w: %d h: %d", originalWidth, originalHeight));

            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();

            logger.log(Level.INFO, String.format("Page sizes: w: %.2f h: %.2f", pageWidth, pageHeight));

            float ratio = Math.min(pageWidth / originalWidth, pageHeight / originalHeight);

            logger.log(Level.INFO, String.format("RATIO: %.2f", ratio));

            float scaledWidth = originalWidth * ratio; // (pageWidth / originalWidth)
            float scaledHeight = originalHeight  * ratio; //(pageHeight / originalHeight)

            logger.log(Level.INFO, String.format("Scaled: w: %.2f h: %.2f", scaledWidth, scaledHeight));

            float x = (pageWidth - scaledWidth) / 2;
            float y = (pageHeight - scaledHeight) / 2;

            logger.log(Level.INFO, String.format("X: %.2f Y: %.2f", x, y));

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {

                contentStream.drawImage(pdImageXObject, x,y, scaledWidth, scaledHeight);
                /**
                 * This part "signs" the pdf and adds the pagination
                 */
                contentStream.beginText();
                contentStream.newLineAtOffset(scaledWidth / 2, 2);
                contentStream.setFont(PDType1Font.HELVETICA, 6);
                contentStream.showText("APDFC 0.6.1 - Page:   " + pageNumber);
                contentStream.endText();

            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savePDF(String destinationPath){
        try {
            logger.log(Level.INFO, "Saving document into path: " + destinationPath);
            doc.save(destinationPath + String.format("/%s.pdf", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss"))));
            doc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
