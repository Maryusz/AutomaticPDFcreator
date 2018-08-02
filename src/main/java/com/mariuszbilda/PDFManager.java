package com.mariuszbilda;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PDFManager {
    private FloatProperty compressionFactor;
    private PDDocument doc;
    private Logger logger;
    private int pageNumber;
    private File tmpDir;

    public PDFManager() {
        doc = new PDDocument();
        logger = Logger.getLogger("PDF MANAGER");
        pageNumber = 0;
        compressionFactor = new SimpleFloatProperty();
        tmpDir = new File(System.getProperty("java.io.tmpdir"));
    }

    /**
     * https://gist.github.com/mmdemirbas/209b4bdb66b788e785266f97204b8631
     * This method take a path of one image and add it to the PDF document.
     * @param file image to add
     */

    public void addPage(File file) {


        pageNumber++;
        try {
            file = compressImage(file);

            PDImageXObject pdImageXObject = PDImageXObject.createFromFileByContent(file, doc);
            PDRectangle pageSize = PDRectangle.A4;

            int originalWidth = pdImageXObject.getWidth();
            int originalHeight = pdImageXObject.getHeight();
            //TODO: Analizzare questo passaggio per la rotazione e compressione dell'immagine
            /*if (originalWidth > originalHeight) {
                // the image must be rotated to be adapted to an A4 format
                pdImageXObject = LosslessFactory.createFromImage(doc, rotateBufferedImage(pdImageXObject.getImage()));
                originalWidth = pdImageXObject.getWidth();
                originalHeight = pdImageXObject.getHeight();
            }*/

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
                contentStream.showText("APDFC 0.6.6 - Page:   " + pageNumber);
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

    private BufferedImage rotateBufferedImage(BufferedImage bi) {
        logger.log(Level.INFO, "Rotating the image..");
        AffineTransform tx = new AffineTransform();
        tx.rotate(1, bi.getWidth() / 2, bi.getHeight() / 2);
        AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_BILINEAR);
        return op.filter(bi, null);
    }

    /**
     * This method compress the image received as File
     * The compressed image is placed in TMP folder!
     *
     * @param file File of the image received
     * @return path to the compressed image
     */
    private File compressImage(File file) {
        logger.log(Level.INFO, "Starting image compression...");
        File compressedImage = new File(tmpDir.getPath() + "//compressed-tmp.jpg");
        System.out.println(compressedImage);

        try {
            BufferedImage image = ImageIO.read(file);

            OutputStream os = new FileOutputStream(compressedImage);

            Iterator<ImageWriter> writer = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter imageWriter = writer.next();

            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            imageWriter.setOutput(ios);

            ImageWriteParam param = imageWriter.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            logger.log(Level.INFO, String.format("Compression factor: %.2f", getCompressionFactor()));
            param.setCompressionQuality(getCompressionFactor());
            imageWriter.write(null, new IIOImage(image, null, null), param);

            os.close();
            ios.close();
            imageWriter.dispose();

        } catch (IOException e) {
            logger.log(Level.INFO, "Error while compressing the image.");
            e.printStackTrace();

        }
        logger.log(Level.INFO, "Image compressed succesfully!");
        return compressedImage;
    }

    public float getCompressionFactor() {
        return compressionFactor.get();
    }

    public void setCompressionFactor(float compressionFactor) {
        this.compressionFactor.set(compressionFactor);
    }

    public FloatProperty compressionFactorProperty() {
        return compressionFactor;
    }
}
