package org.example.token;

import org.example.CompilerCtx;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TokenizerTest {

    @Test
    public void test() {
        tokenize(
 """
        + - * / ;
        ( ) { }
        && ||
        = ==
        func while if else true false
        my-variable as-many-dashes-as-I-want
        has-more-chars? finished? listEmpty?
        """);
    }

    @Test
    public void test2() {
        tokenize("""
            func main() {
                let a = 1;
                let b = 2;
                let c = a + b;
                println(c);
                println(c == a+b);
            }
        """);
    }

    @Test
    public void testComments() {
        assertTokens("");
        assertTokens("//");
        assertTokens("1//", TokenType.NUMBER);
        assertTokens("1//\n+2", TokenType.NUMBER, TokenType.PLUS, TokenType.NUMBER);
        assertTokens("abc//\n+2", TokenType.IDENTIFIER, TokenType.PLUS, TokenType.NUMBER);
        assertTokens("abc //\n +2", TokenType.IDENTIFIER, TokenType.PLUS, TokenType.NUMBER);
    }

    @Test
    public void test3() {
        tokenize("");
        tokenize(" ");
        tokenize("  ");
    }

    private void tokenize(String source) {
        CompilerCtx.printTokens(source);
    }

    private static void assertTokens(String source, TokenType... expected) {
        List<TokenType> tokens = CompilerCtx.tokenize(source);
        assertEquals(Arrays.asList(expected), tokens);
    }
}
