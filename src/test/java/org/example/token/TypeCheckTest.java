package org.example.token;

import org.example.CompilerCtx;
import org.junit.Test;

import static org.example.CompilerCtx.readResource;
import static org.junit.Assert.assertThrows;

public class TypeCheckTest {
    @Test
    public void test() {
        checkTypes(readResource("/lang_samples/typechecking.txt"));
    }

    @Test
    public void test2() {
        assertThrows(RuntimeException.class, () -> {
            checkTypes("func foo() -> i32 { return true; }");
        });
    }

    @Test
    public void canReturnReturn() {
        checkTypes("func foo() -> void { return return; }");
    }

    public static void checkTypes(String source) {
        CompilerCtx.checkTypes(source);
    }
}
