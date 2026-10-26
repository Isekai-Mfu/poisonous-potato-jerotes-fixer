import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class PatchPoisonousPotato {
    private static final String EXPECTED_SHA256 = "74f04831df568e06ddb6e2de14362e8d32d2d9e67e422cc6b61cbadb6b55295a";
    private static final String OUTPUT_NAME = "reimaginingpotatoes-1.20.1-1.0.1-jerotes-fix.jar";
    private static final String CLASS_NAME = "com/nine/reimaginingpotatoes/common/entity/PoisonousPotatoZombie";
    private static final String CLASS_ENTRY = CLASS_NAME + ".class";
    private static final String PLAYER = "net/minecraft/world/entity/player/Player";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java -jar PoisonousPotatoJerotesFixer.jar <official-1.0.1.jar>");
            System.exit(2);
        }
        Path input = args[0] == null ? null : Path.of(args[0]).toAbsolutePath().normalize();
        if (input == null || !Files.isRegularFile(input)) {
            throw new IOException("Input JAR not found: " + input);
        }
        byte[] inputBytes = Files.readAllBytes(input);
        String actualHash = sha256(inputBytes);
        if (!EXPECTED_SHA256.equals(actualHash)) {
            throw new IOException("Unsupported or modified input JAR.\nExpected SHA-256: " + EXPECTED_SHA256 + "\nActual SHA-256:   " + actualHash);
        }

        Path output = input.resolveSibling(OUTPUT_NAME);
        Path backup = input.resolveSibling(input.getFileName() + ".original-backup");
        if (Files.exists(output)) {
            throw new IOException("Output already exists; refusing to overwrite: " + output);
        }
        if (Files.exists(backup)) {
            if (!EXPECTED_SHA256.equals(sha256(Files.readAllBytes(backup)))) {
                throw new IOException("Backup exists but is not the expected original; refusing to overwrite: " + backup);
            }
        } else {
            Files.copy(input, backup);
            System.out.println("Original backup: " + backup);
        }

        Path temp = Files.createTempFile(output.getParent(), OUTPUT_NAME, ".tmp");
        try {
            writePatchedJar(input, temp);
            verifyJar(temp);
            Files.move(temp, output);
        } finally {
            Files.deleteIfExists(temp);
        }
        System.out.println("Fixed JAR: " + output);
        System.out.println("Success. Replace the original mod with the fixed JAR; do not install both.");
    }

    private static void writePatchedJar(Path source, Path target) throws IOException {
        try (
            ZipFile input = new ZipFile(source.toFile());
            ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(target))
        ) {
            Enumeration<? extends ZipEntry> entries = input.entries();
            while (entries.hasMoreElements()) {
                ZipEntry oldEntry = entries.nextElement();
                byte[] data;
                try (InputStream stream = input.getInputStream(oldEntry)) {
                    data = stream.readAllBytes();
                }
                switch (oldEntry.getName()) {
                    case CLASS_ENTRY -> data = patchClass(data);
                    case "META-INF/mods.toml" -> data = patchModsToml(data);
                    case "META-INF/MANIFEST.MF" -> data = patchManifest(data);
                    default -> { }
                }
                ZipEntry newEntry = new ZipEntry(oldEntry);
                newEntry.setSize(data.length);
                newEntry.setCompressedSize(-1);
                if (newEntry.getMethod() == ZipEntry.STORED) {
                    CRC32 crc = new CRC32();
                    crc.update(data);
                    newEntry.setCompressedSize(data.length);
                    newEntry.setCrc(crc.getValue());
                }
                output.putNextEntry(newEntry);
                output.write(data);
                output.closeEntry();
            }
        }
    }

    private static byte[] patchModsToml(byte[] data) {
        String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        text = replaceRequired(text, "version=\"0.2.05\"", "version=\"1.0.1-jerotes-fix-1\"");
        text = replaceRequired(text, "displayName=\"Poisonous Potato Update\"", "displayName=\"Poisonous Potato Update (Jerotes Compatibility Fix)\"");
        return text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] patchManifest(byte[] data) {
        String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        text = replaceRequired(text, "Implementation-Version: 1.20.1-1.0.1-forge", "Implementation-Version: 1.0.1-jerotes-fix-1");
        return text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String replaceRequired(String text, String oldValue, String newValue) {
        int first = text.indexOf(oldValue);
        if (first < 0 || text.indexOf(oldValue, first + oldValue.length()) >= 0) {
            throw new IllegalStateException("Expected exactly one metadata value: " + oldValue);
        }
        return text.replace(oldValue, newValue);
    }

    private static byte[] patchClass(byte[] originalClass) {
        ClassReader reader = new ClassReader(originalClass);
        ClassWriter writer = new ClassWriter(reader, 0);
        boolean[] patched = {false};
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals("lambda$registerGoals$1") && descriptor.equals("(Lnet/minecraft/world/entity/LivingEntity;)Z")) {
                    patched[0] = true;
                    MethodVisitor method = writer.visitMethod(access, name, descriptor, signature, exceptions);
                    emitPatchedMethod(method);
                    return null;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
        reader.accept(visitor, 0);
        if (!patched[0]) {
            throw new IllegalStateException("Target method not found");
        }
        return writer.toByteArray();
    }

    private static void emitPatchedMethod(MethodVisitor method) {
        Label start = new Label();
        Label notPlayer = new Label();
        Label wearingHat = new Label();
        Label result = new Label();
        Label end = new Label();
        method.visitCode();
        method.visitLabel(start);
        method.visitLineNumber(39, start);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitTypeInsn(Opcodes.INSTANCEOF, PLAYER);
        method.visitJumpInsn(Opcodes.IFEQ, notPlayer);
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitTypeInsn(Opcodes.CHECKCAST, PLAYER);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CLASS_NAME, "isUsingZombieHat", "(L" + PLAYER + ";)Z", false);
        method.visitJumpInsn(Opcodes.IFNE, wearingHat);
        method.visitLabel(notPlayer);
        method.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        method.visitInsn(Opcodes.ICONST_1);
        method.visitJumpInsn(Opcodes.GOTO, result);
        method.visitLabel(wearingHat);
        method.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        method.visitInsn(Opcodes.ICONST_0);
        method.visitLabel(result);
        method.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
        method.visitInsn(Opcodes.IRETURN);
        method.visitLabel(end);
        method.visitLocalVariable("this", "L" + CLASS_NAME + ";", null, start, end, 0);
        method.visitLocalVariable("livingEntity", "Lnet/minecraft/world/entity/LivingEntity;", null, start, end, 1);
        method.visitMaxs(2, 2);
        method.visitEnd();
    }

    private static void verifyJar(Path jar) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            if (zip.getEntry(CLASS_ENTRY) == null || zip.getEntry("META-INF/mods.toml") == null) {
                throw new IOException("Patched JAR verification failed");
            }
        }
    }

    private static String sha256(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }
}
