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
                fibonacci(10000);
            }

            func fibonacci(limit: i32) {
                let a: i32 = 0;
                let b: i32 = 1;
                while (a < limit) {
                    println(a);
                    let temp: i32 = a + b;
                    a = b;
                    b = temp;
                };
            }
        """);
    }
}
