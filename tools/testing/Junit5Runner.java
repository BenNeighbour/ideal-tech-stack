import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

/**
 * Bazel-compatible JUnit 5 runner.
 *
 * Invoked by junit5_test() macro with args[0] = fully-qualified test class name.
 * Writes JUnit XML to XML_OUTPUT_FILE when set so Bazel can report per-test results.
 */
public class Junit5Runner {

    public static void main(String[] args) throws Exception {
        // Suppress JOOQ banners if JOOQ happens to be on the test classpath
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");

        String testClass = args[0];
        String xmlOutputFile = System.getenv("XML_OUTPUT_FILE");

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(testClass))
            .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener summary = new SummaryGeneratingListener();

        if (xmlOutputFile != null) {
            // Write XML to a temp dir then move to the path Bazel expects
            Path xmlDir = Files.createTempDirectory("junit5_");
            try {
                launcher.execute(request, summary,
                    new LegacyXmlReportGeneratingListener(xmlDir, new PrintWriter(System.out, true)));
                Path generated = xmlDir.resolve("TEST-" + testClass + ".xml");
                if (Files.exists(generated)) {
                    Files.move(generated, Paths.get(xmlOutputFile), StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.walk(xmlDir).sorted(Comparator.reverseOrder())
                    .map(Path::toFile).forEach(java.io.File::delete);
            }
        } else {
            launcher.execute(request, summary);
        }

        TestExecutionSummary s = summary.getSummary();
        s.printFailuresTo(new PrintWriter(System.err, true));
        System.exit(s.getTestsFailedCount() > 0 ? 1 : 0);
    }
}
