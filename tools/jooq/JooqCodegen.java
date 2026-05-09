import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Property;
import org.jooq.meta.jaxb.Target;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Bazel codegen tool: reads Flyway-style SQL DDL files, runs JOOQ DDLDatabase,
 * and writes a .srcjar of the generated Java sources.
 *
 * Args: <output.srcjar> <java.package.name> <file1.sql> [file2.sql ...]
 */
public class JooqCodegen {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("usage: JooqCodegen <out.srcjar> <package> <file.sql>...");
            System.exit(1);
        }

        // Suppress JOOQ banner and tips during code generation
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");

        String outputSrcjar = args[0];
        String packageName  = args[1];
        String[] sqlInputs  = Arrays.copyOfRange(args, 2, args.length);

        Path workDir = Files.createTempDirectory("jooq_");
        Path sqlDir  = workDir.resolve("sql");
        Path genDir  = workDir.resolve("gen");
        Files.createDirectories(sqlDir);
        Files.createDirectories(genDir);

        try {
            // Collect SQL files into one directory so DDLDatabase can glob them
            for (String sqlFile : sqlInputs) {
                Path src = Paths.get(sqlFile);
                Files.copy(src, sqlDir.resolve(src.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }

            GenerationTool.generate(new Configuration()
                .withGenerator(new Generator()
                    .withDatabase(new Database()
                        .withName("org.jooq.meta.extensions.ddl.DDLDatabase")
                        .withProperties(
                            new Property().withKey("scripts").withValue(sqlDir + "/*.sql"),
                            // flyway sort handles V1__/V2__/... ordering correctly (not lexicographic)
                            new Property().withKey("sort").withValue("flyway"),
                            // lowercase identifiers match PostgreSQL's default behaviour
                            new Property().withKey("defaultNameCase").withValue("lower")
                        ))
                    .withTarget(new Target()
                        .withPackageName(packageName)
                        .withDirectory(genDir.toString()))));

            // Pack generated sources into a srcjar (zip of .java files, Bazel-consumable)
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputSrcjar))) {
                Files.walk(genDir)
                    .filter(Files::isRegularFile)
                    .sorted()
                    .forEach(f -> {
                        try {
                            zos.putNextEntry(new ZipEntry(genDir.relativize(f).toString()));
                            Files.copy(f, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            }
        } finally {
            Files.walk(workDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(java.io.File::delete);
        }
    }
}
