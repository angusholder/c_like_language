package org.example.wasm;

import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.*;

import java.util.List;

public class WasmHello {
    public static void wasmHelloExample() {
        try (Store<Void> store = Store.withoutData()) {
            // Compile the wasm binary into an in-memory instance of a `Module`.
            System.err.println("Compiling module...");
            try (Engine engine = store.engine();
                 Module module = Module.fromFile(engine, "./hello.wat")) {
                // Here we handle the imports of the module, which in this case is our
                // `HelloCallback` type and its associated implementation of `Callback.
                System.err.println("Creating callback...");
                try (Func helloFunc = WasmFunctions.wrap(store, () -> {
                    System.err.println("CB!! Calling back...");
                    System.err.println("CB!! > Hello World!");
                })) {
                    // Once we've got that all set up we can then move to the instantiation
                    // phase, pairing together a compiled module as well as a set of imports.
                    // Note that this is where the wasm `start` function, if any, would run.
                    System.err.println("Instantiating module...");
                    List<Extern> imports = List.of(Extern.fromFunc(helloFunc));
                    try (Instance instance = new Instance(store, module, imports)) {
                        // Next we poke around a bit to extract the `run` function from the module.
                        System.err.println("Extracting export...");
                        try (Func f = instance.getFunc(store, "run").get()) {
                            WasmFunctions.Consumer0 fn = WasmFunctions.consumer(store, f);

                            // And last but not least we can call it!
                            System.err.println("Calling export...");
                            fn.accept();

                            System.err.println("Done.");
                        }
                    }
                }
            }
        }
    }
}
