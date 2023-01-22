package org.example.token;

import org.example.CompilerCtx;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static java.lang.System.out;

public class TokenizerTest {
    private CompilerCtx ctx;

    @Before
    public void setup() {
        ctx = new CompilerCtx();
    }

    @Test
    public void test() {
        printLines(tokenize(
 """
        + - * / ;
        ( ) { }
        && ||
        = ==
        func while if else
        """));
    }

    @Test
    public void test2() {
        printLines(tokenize("""
            func main() {
                let a = 1;
                let b = 2;
                let c = a + b;
                println(c);
                println(c == a+b);
            }
        """));
    }

    @Test
    public void test3() {
        printLines(tokenize(""));
        printLines(tokenize(" "));
        printLines(tokenize("  "));
    }

    private List<Token> tokenize(String source) {
        return ctx.createTokenizer(ctx.addInMemoryFile("anon-file", source)).tokenizeAll();
    }

    private void printLines(List<Token> tokens) {
        for (Token token : tokens) {
            out.println(token.format(ctx));
        }
    }
}
