package cl.education.enrollment.service;

import cl.education.enrollment.model.DispatchGuide;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dispatch-guides")
public class DispatchGuidePdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final PDType1Font TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    public byte[] generate(DispatchGuide guide) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.newLineAtOffset(50, 742);
                content.setLeading(18);
                content.setFont(TITLE_FONT, 16);
                content.showText("Guia de despacho #" + guide.getId());
                content.newLine();
                content.newLine();
                content.setFont(BODY_FONT, 11);
                writeLine(content, "Numero de pedido: " + guide.getOrderNumber());
                writeLine(content, "Fecha de despacho: " + DATE_FORMAT.format(guide.getDispatchDate()));
                writeLine(content, "Transportista: " + guide.getCarrierName());
                writeLine(content, "RUT transportista: " + guide.getCarrierRut());
                writeLine(content, "Destinatario: " + guide.getRecipientName());
                writeLine(content, "Origen: " + guide.getOriginAddress());
                writeLine(content, "Destino: " + guide.getDestinationAddress());
                content.newLine();
                content.setFont(TITLE_FONT, 12);
                writeLine(content, "Detalle de carga");
                content.setFont(BODY_FONT, 11);
                writeLine(content, guide.getPackageDescription());
                content.newLine();
                writeLine(content, "Codigo de acceso para descarga: " + guide.getAccessCode());
                content.endText();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible generar la guia en PDF.", exception);
        }
    }

    private void writeLine(PDPageContentStream content, String text) throws IOException {
        content.showText(clean(text));
        content.newLine();
    }

    private String clean(String text) {
        return text == null ? "" : text.replaceAll("[\\r\\n\\t]+", " ");
    }
}
