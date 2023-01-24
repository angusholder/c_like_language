Currently only supports two types: `i32`, and `bool`.

Example source:

```
func println(a: i32) {}

func printlnBool(a: bool) {}

func main() -> i32 {
    let a: i32 = 1;
    let b: i32 = 2;
    let c: i32 = a + b;
    println(c);
    printlnBool(a+b == c);
    printlnBool(true);
    printlnBool(false);
    return 5;
}

func ifExpression() -> i32 {
    return if (1 == 3) {
        1;
    } else if (1 == 2) {
        2;
    } else {
        3;
    };
}
```

# Useful resources:

- [X64 instruction reference](https://www.felixcloutier.com/x86/)
- [Pratt parser](http://journal.stuffwithstuff.com/2011/03/19/pratt-parsers-expression-parsing-made-easy/)
