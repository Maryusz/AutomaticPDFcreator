import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PDFManager {
    private PDDocument doc = null;

    public PDFManager() {
        doc = new PDDocument();
    }

    /**
     * https://gist.github.com/mmdemirbas/209b4bdb66b788e785266f97204b8631
     * This method take a path of one image and add it to the PDF document.
     * @param imagePath path of the image
     */
    public void addPage(String imagePath) {


        try {
            PDImageXObject pdImageXObject = PDImageXObject.createFromFileByContent(new File(imagePath), doc);
            PDRectangle pageSize = PDRectangle.A4;

            int originalWidth = pdImageXObject.getWidth();
            int originalHeight = pdImageXObject.getHeight();

            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            float ratio = Math.min(pageWidth / originalWidth, pageHeight / originalHeight);
            float scaledWidth = originalWidth * ratio;
            float scaledHeight = originalHeight * ratio;

            float x = (pageWidth - scaledWidth) / 2;
            float y = (pageHeight - scaledHeight) / 2;

            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                contentStream.drawImage(pdImageXObject, x, y, scaledWidth, scaledHeight);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savePDF(String destinationPath){
        try {
            doc.save(destinationPath + String.format("/%s.pdf", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss"))));
            doc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
