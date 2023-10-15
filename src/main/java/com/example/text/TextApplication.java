package com.example.text;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.FileSystemGeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.nativex.FileNativeConfigurationWriter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.javapoet.ClassName;
import org.springframework.util.Assert;
import org.springframework.util.SystemPropertyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@SpringBootApplication
@ImportRuntimeHints(TextApplication.ITextRuntimeHintsRegistrar.class)
public class TextApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(TextApplication.class, args);
    }

    static void write(RuntimeHints hints) {
        try {
            var resourcePatternResolver = new PathMatchingResourcePatternResolver();
            var memberCategories = MemberCategory.values();
            for (var c : new String[]{"com.itextpdf.text.pdf.PdfName",
                    "com.itextpdf.license.LicenseKey", "com.itextpdf.licensekey.LicenseKey"}) {
                hints.reflection().registerType(TypeReference.of(c), memberCategories);
            }

            var fontResources = resourcePatternResolver
                    .getResources("com/itextpdf/text/pdf/fonts/*");
            for (var r : fontResources) {
                if (r.exists()) {
                    hints.resources().registerResource(r);
                }
            }

            for (var p : "en,nl,pt".split(",")) {
                hints.resources().registerResource(
                        new ClassPathResource("com/itextpdf/text/l10n/error/" + p + ".lng"));
            }
        }//
        catch (Throwable t) {
            throw new RuntimeException("oops", t);
        }
    }

    protected FileSystemGeneratedFiles createFileSystemGeneratedFiles() {
        return new FileSystemGeneratedFiles(this::getRoot);
    }

    private static final File ROOT = new File(SystemPropertyUtils
            .resolvePlaceholders("${HOME}/Desktop/json/"));

    private Path getRoot(GeneratedFiles.Kind kind) {

        var file = new File(ROOT, switch (kind) {
            case SOURCE -> "source";
            case RESOURCE -> "resources";
            case CLASS -> "classes";
        });
        return (file).toPath();
    }

    protected static void writeHints(RuntimeHints hints) {
        var writer = new FileNativeConfigurationWriter(ROOT.toPath(), "bootiful", "itext");
        writer.write(hints);
    }

    private static File ensureExists(File file) {
        Assert.state(file.exists() || file.mkdirs(), "the folder [" + file.getAbsolutePath() + "] does not exist");
        return file;
    }

    // @PostConstruct
    public void runtime() {

        var dgc = new DefaultGenerationContext(
                new ClassNameGenerator(ClassName.get(ITextRuntimeHintsRegistrar.class)),
                createFileSystemGeneratedFiles());
        var hints = dgc.getRuntimeHints();
        write(hints);
        dgc.writeGeneratedContent();
        writeHints(hints);
    }

    static class ITextRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            write(hints);
        }
    }

    private static File pdf(String name) {
        var f = new File(SystemPropertyUtils.resolvePlaceholders("${HOME}/Desktop/itext-native/" + name + ".pdf"));
        var pf = f.getParentFile();
        ensureExists(pf);
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

    static void simple4() throws Exception {

        var written = pdf("qrcode");
        try (var in = new FileInputStream(written)) {
            var pdfReader = new PdfReader(in);
            var page = pdfReader.getPageN(1);
            Assert.notNull(page, "the page should not be null");
            var arrayForBoundingBox = page.getAsArray(PdfName.MEDIABOX);
            arrayForBoundingBox.forEach(pdfObject -> System.out.println(pdfObject.toString()));
        }
    }

    @Bean
    ApplicationRunner test() {
        return args -> {
            simple3();
            simple2();
            simple1();
            simple4();
        };
    }
}
