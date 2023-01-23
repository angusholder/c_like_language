package org.example.codegen;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.lang.foreign.ValueLayout.*;

public class Codegen {

    public static void main(String[] args) throws Throwable {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            System.err.println("This example only works on Windows");
            System.exit(1);
        }

        try (MemorySession memory = MemorySession.openConfined()) {
            MethodHandle return42 = createFunction(memory, ASM_RETURN_42, FunctionDescriptor.of(JAVA_INT));
            int b = (int) return42.invokeExact();
            System.out.println("return42: " + b);

            MethodHandle readRegs = createFunction(memory, ASM_READ_REGS, FunctionDescriptor.ofVoid(ADDRESS));
            MemorySegment regs = memory.allocateArray(JAVA_LONG, 16);
            readRegs.invokeExact((Addressable)regs);
            for (int i = 0; i < 16; i++) {
                String name = REGS[i];
                long value = regs.getAtIndex(JAVA_LONG, i);
                System.out.printf("%3s: %016X\n", name, value);
            }
        }
    }

    public static MethodHandle createFunction(MemorySession memorySession, String asmSource, FunctionDescriptor descriptor) {
        return createFunction(memorySession, parseHexAsm(asmSource), descriptor);
    }

    public static MethodHandle createFunction(MemorySession memorySession, byte[] code, FunctionDescriptor descriptor) {
        MemorySegment codePage = memorySession.allocate(4096, 4096);
        codePage.asByteBuffer().put(code);
        MemoryProtection.virtualProtect(codePage, PAGE_EXECUTE_READ);
        return Linker.nativeLinker().downcallHandle(codePage, descriptor);
    }

    public static byte[] parseHexAsm(String source) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        for (String line : source.lines().toArray(String[]::new)) {
            int semiIdx = line.indexOf(';');
            if (semiIdx != -1) {
                line = line.substring(0, semiIdx);
            }
            line = line.strip().trim();
            if (line.isEmpty()) {
                continue;
            }
            for (String hex : line.split("\\s+")) {
                buf.put((byte) Integer.parseInt(hex, 16));
            }
        }

        return Arrays.copyOf(buf.array(), buf.position());
    }

    public static final String ASM_RETURN_42 = """
        b8 2a 00 00 00 ; mov eax, 42
        c3 ; ret
    """;

    public static final String ASM_READ_REGS = """
        48 89 41 00 ; mov qword ptr [rcx + 0x00], rax
        48 89 49 08 ; mov qword ptr [rcx + 0x00], rcx
        48 89 51 10 ; mov qword ptr [rcx + 0x10], rdx
        48 89 59 18 ; mov qword ptr [rcx + 0x18], rbx
        48 89 61 20 ; mov qword ptr [rcx + 0x20], rsp
        48 89 69 28 ; mov qword ptr [rcx + 0x28], rbp
        48 89 71 30 ; mov qword ptr [rcx + 0x30], rsi
        48 89 79 38 ; mov qword ptr [rcx + 0x38], rcx
        4c 89 41 40 ; mov qword ptr [rcx + 0x40], r8
        4c 89 49 48 ; mov qword ptr [rcx + 0x48], r9
        4c 89 51 50 ; mov qword ptr [rcx + 0x50], r10
        4c 89 59 58 ; mov qword ptr [rcx + 0x58], r11
        4c 89 61 60 ; mov qword ptr [rcx + 0x60], r12
        4c 89 69 68 ; mov qword ptr [rcx + 0x68], r13
        4c 89 71 70 ; mov qword ptr [rcx + 0x70], r14
        4c 89 79 78 ; mov qword ptr [rcx + 0x78], r15

        c3 ; ret
    """;

    public static final String[] REGS = new String[] {
            "rax", "rcx", "rdx", "rbx", "rsp", "rbp", "rsi", "rdi",
            "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15",
    };

    public static class MemoryProtection {
        static {
            SymbolLookup kernel32 = SymbolLookup.libraryLookup("Kernel32", MemorySession.global());
            MemorySegment virtualProtectFunc = kernel32.lookup("VirtualProtect").orElseThrow();

            FunctionDescriptor virtualProtectSig = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT, ADDRESS);
            virtualProtect = Linker.nativeLinker().downcallHandle(virtualProtectFunc, virtualProtectSig);
        }

        /** Sets the memory protection of the given memory page(s). */
        // https://learn.microsoft.com/en-gb/windows/win32/api/memoryapi/nf-memoryapi-virtualprotect
        public static void virtualProtect(MemorySegment segment, int newProtect) {
            int success;
            try {
                success = (int)virtualProtect.invoke(segment, segment.byteSize(), newProtect, oldProtect);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            if (success == 0) {
                throw new RuntimeException("VirtualProtect failed");
            }
            System.out.println("VirtualProtect oldProtect: " + oldProtect.get(JAVA_LONG, 0));
        }

        @SuppressWarnings("resource")
        private static final MemorySegment oldProtect = MemorySession.global().allocate(JAVA_LONG);
        private static final MethodHandle virtualProtect;
    }

    // https://learn.microsoft.com/en-us/windows/win32/Memory/memory-protection-constants
    public static final int PAGE_READWRITE = 0x04;
    public static final int PAGE_EXECUTE_READ = 0x20;
}
