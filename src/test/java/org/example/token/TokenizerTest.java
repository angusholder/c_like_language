package org.example.token;

import org.example.CompilerCtx;
import org.junit.Test;

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
    public void test3() {
        tokenize("");
        tokenize(" ");
        tokenize("  ");
    }

    private void tokenize(String source) {
        CompilerCtx.printTokens(source);
    }
}
