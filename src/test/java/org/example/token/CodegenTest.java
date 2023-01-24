package org.example.token;

import org.example.CompilerCtx;
import org.junit.Test;

public class CodegenTest {
    @Test
    public void test() {
        CompilerCtx.codeEmitForExpression("1+2");
    }

    @Test
    public void test2() {
        CompilerCtx.codeEmitForExpression("(1+(2*-5) - 10/3) <= 5");
    }
}
