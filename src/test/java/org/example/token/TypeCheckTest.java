package org.example.token;

import org.example.CompilerCtx;
import org.example.parse.AstFile;
import org.example.parse.Parser;
import org.example.typecheck.TypeChecker;
import org.junit.Test;

public class TypeCheckTest {
    @Test
    public void test() {
        checkTypes("""
        func println(a: i32) {}
        func printlnBool(a: bool) {}
        func main() {
            let a: i32 = 1;
            let b: i32 = 2;
            let c: i32 = a + b;
            println(c);
            printlnBool(a+b == c);
        }
        
        func emptyFunc() {}
        func oneParam(a: i32) {}
        func oneParamCommad(a: i32,) {}
        func twoParams(a: i32, b: i32) {}
        func twoParamsComma(a: i32, b: i32,) {}
        func exprIf() {
            let a: i32 = if (1 == 3) {
                1;
            } else if (1 == 2) {
                2;
            } else {
                3;
            };
        }
        """);
    }

    public static void checkTypes(String source) {
        var ctx = new CompilerCtx();
        Parser parser = ctx.createParser(ctx.addInMemoryFile("anon-file", source));
        AstFile file = parser.parseFile();
        new TypeChecker(ctx).checkFile(file);
    }
}
