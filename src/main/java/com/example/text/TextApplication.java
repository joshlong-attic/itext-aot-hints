package com.example.text;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BarcodeEAN;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.SystemPropertyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

@SpringBootApplication
@ImportRuntimeHints(TextApplication.Hints.class)
public class TextApplication {

    public static void main(String[] args) {
        SpringApplication.run(TextApplication.class, args);
    }

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {


            var memberCategories = MemberCategory.values();
            for (var c : new String[]{"com.itextpdf.text.pdf.PdfName",
                    "com.itextpdf.license.LicenseKey", "com.itextpdf.licensekey.LicenseKey"}) {
                hints.reflection().registerType(TypeReference.of(c), memberCategories);
            }

            for (var p : new String[]{
                    "com/itextpdf/text/pdf/fonts/glyphlist.txt",
                    "com/itextpdf/text/pdf/fonts/Helvetica.afm",
                    "com/itextpdf/text/pdf/fonts/Helvetica-Bold.afm"}) {
                hints.resources().registerResource(new ClassPathResource(p));
            }

            for (var p : "en,nl,pt".split(",")) {
                hints.resources().registerResource(new ClassPathResource("com/itextpdf/text/l10n/error/" + p + ".lng"));
            }

        }
    }

    private static File pdf(String name) {
        var f = new File(SystemPropertyUtils.resolvePlaceholders("${HOME}/Desktop/itext-native/" + name + ".pdf"));
        var pf = f.getParentFile();
        Assert.state(pf.exists() || pf.mkdirs(),
                "the directory [" + pf.getAbsolutePath() + "] does not exist");
        return f;
    }

    private static void simple1() throws Exception {
        try (var out = new FileOutputStream(pdf("simple-1"))) {
            var document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            var p = new Paragraph();
            p.add("This is my paragraph 1");
            p.setAlignment(Element.ALIGN_CENTER);
            document.add(p);
            var p2 = new Paragraph();
            p2.add("This is my paragraph 2"); //no alignment
            document.add(p2);
            var f = new Font();
            f.setStyle(Font.BOLD);
            f.setSize(8);
            document.add(new Paragraph("This is my paragraph 3", f));
            document.close();
            System.out.println("Done");
        }
    }

    static void simple2() throws Exception {
        var code = "123456789";
        try (var out = new FileOutputStream(pdf("barcode"))) {
            var document = new Document();
            var pdfWriter = PdfWriter.getInstance(document, out);
            document.open();
            var barcode = new BarcodeEAN();
            barcode.setCodeType(BarcodeEAN.EAN8);
            barcode.setCode(code);
            var img = (barcode.createImageWithBarcode(new PdfContentByte(pdfWriter), BaseColor.BLACK, BaseColor.WHITE));
            document.add(img);
            document.close();
        }
    }

    static void simple3() throws Exception {
        var code = "123456789";
        try (var out = new FileOutputStream(pdf("qrcode"))) {
            var document = new Document();
            var pdfWriter = PdfWriter.getInstance(document, out);
            document.open();
            var barcode = new BarcodeQRCode(code, 100, 100, Map.of());
            var img = (barcode.getImage());
            document.add(img);
            document.close();
        }
    }

    @Bean
    ApplicationRunner test() {
        return args -> {
            simple3();
            simple2();
            simple1();
        };
    }
}
