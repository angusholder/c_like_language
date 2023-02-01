package org.example.token;

import org.example.CompilerCtx;
import org.junit.Test;

import static org.example.CompilerCtx.readResource;

public class TreeInterpreterTest {
    @Test
    public void fibonacci() {
        CompilerCtx.interpret(readResource("/lang_samples/fibonacci.txt"));
    }

    @Test
    public void factorial() {
        CompilerCtx.interpret(readResource("/lang_samples/factorial.txt"));
    }

    @Test
    public void helloWorld() {
        CompilerCtx.interpret(readResource("/lang_samples/hello_world.txt"));
    }
}
