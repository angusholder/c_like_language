package org.example.token;

import org.example.CompilerCtx;
import org.junit.Test;

public class TreeInterpreterTest {
    @Test
    public void test() {
        CompilerCtx.interpret(
        """
            func println(a: i32) {}
            func printlnBool(a: bool) {}

            func main() {
                let a: i32 = 0;
                let b: i32 = 1;
                while (a < 1000) {
                    println(a);
                    let temp: i32 = a + b;
                    a = b;
                    b = temp;
                };
            }
        """);
    }
}
