import java.io.IOException;

import de.prauscher.cli.CLI;
import de.prauscher.cli.Command;
import de.prauscher.cli.CommandArgument;

public class Example1 extends CLI {
    @Command(command="add", help="Add two numbers")
    public void add(@CommandArgument(help="summand 1") int a, @CommandArgument(help="summand 2") int b) {
        System.out.println("Result: " + (a + b));
    }

    @Command(command="hello", help="Greets individually")
    public void greet(@CommandArgument(help="The name to greet") String name) {
        System.out.println("Hello " + name);
    }

    @Command(command="hello", help="Prints out simple hello world")
    public void printHello() {
        System.out.println("Hello World!");
    }

    public static void main(String[] args) throws IOException {
        Example1 cli = new Example1();
        cli.loop();
    }
}
