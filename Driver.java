// Import required packages and classes
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

// Main class of the application
public class Driver {
    public static void main(String[] args) throws Exception {
        // Initialize input stream from System.in
        ANTLRInputStream input = new ANTLRInputStream(System.in);
        // Initialize the lexer with the input stream
        LittleLexer lexer = new LittleLexer(input);
        // Create a token stream from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        // Initialize the parser with the token stream
        LittleParser parser = new LittleParser(tokens);
        // Parse the input and create a parse tree
        ParseTree tree = parser.program();

        // Create a custom listener for the parse tree
        LittleBaseListenerExtended listener = new LittleBaseListenerExtended();
        // Initialize the global scope
        listener.enterScope("GLOBAL");
        // Create a parse tree walker
        ParseTreeWalker walker = new ParseTreeWalker();
        // Walk the parse tree using the custom listener
        walker.walk(listener, tree);

        // Print the symbol table AFTER walking the tree
        listener.printSymbolTable();
    }
}

// Symbol class representing an individual symbol in the symbol table
class Symbol {
    String name;
    String type;
    String value;

    // Constructor for symbol without value
    Symbol(String name, String type) {
        this.name = name;
        this.type = type;
    }

    // Constructor for symbol with value
    Symbol(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    // Custom toString method for printing symbol details
    @Override
    public String toString() {
        String result = "name " + name + " type " + type;
        if (value != null) {
            result += " value " + value;
        }
        return result;
    }
}

// SymbolTable class to store symbols
class SymbolTable {
    // Map to store symbols with their name as key
    private Map<String, Symbol> symbols = new HashMap<>();

    // Method to add a symbol to the symbol table
    public void addSymbol(String name, String type, String value) {
        symbols.put(name, new Symbol(name, type, value));
    }

    // Method to get a symbol from the symbol table by name
    public Symbol getSymbol(String name) {
        return symbols.get(name);
    }

    // Method to get all symbols in the symbol table
    public Collection<Symbol> getAllSymbols() {
        return symbols.values();
    }
}

// Custom listener class that extends the base listener generated by ANTLR
class LittleBaseListenerExtended extends LittleBaseListener {
    // Variables for handling scopes, symbol tables, and other state
    private int scopeLevel = 0;
    private Stack<Integer> blockCounterStack = new Stack<>();
    private int blockCounter = 1;
    private String currentScope;
    private ArrayList<Map.Entry<String, ArrayList<Symbol>>> symbolTable = new ArrayList<>();

    // Helper method to define a symbol in the current scope
    private void defineSymbol(Symbol symbol) {
        // Find the current scope entry in the symbol table
        Optional<Map.Entry<String, ArrayList<Symbol>>> currentEntry = symbolTable.stream()
                .filter(entry -> entry.getKey().equals(currentScope))
                .findFirst();

        // If the scope entry is found
        if (currentEntry.isPresent()) {
            ArrayList<Symbol> currentSymbols = currentEntry.get().getValue();
            // Check if the symbol already exists in the current scope
            Optional<Symbol> existingSymbol = currentSymbols.stream()
                    .filter(s -> s.name.equals(symbol.name))
                    .findFirst();

            // If the symbol already exists, print an error message and exit the program
            if (existingSymbol.isPresent()) {
                System.out.println("DECLARATION ERROR " + symbol.name);
                System.exit(1);
            } else {
                // If the symbol doesn't exist, add it to the current scope's symbol list
                currentSymbols.add(symbol);
            }
        } else {
            // If the scope entry is not found, create a new scope with the symbol
            ArrayList<Symbol> newSymbols = new ArrayList<>();
            newSymbols.add(symbol);
            symbolTable.add(new SimpleEntry<>(currentScope, newSymbols));
        }
    }

    // called to enter a new scope with the given scope name
    public void enterScope(String scopeName) {
        currentScope = scopeName;
        // Initialize an empty list of symbols for the new scope and add it to the symbol table
        symbolTable.add(new SimpleEntry<>(currentScope, new ArrayList<>()));
    }

    // called to exit the current scope and set the scope back to global
    private void exitScope() {
        currentScope = "GLOBAL";
    }

    // called to print the symbol table
    public void printSymbolTable() {
        // StringBuilder to store the output of the symbol table
        StringBuilder output = new StringBuilder();

        // Create a list to store and sort the scope names
        ArrayList<String> sortedScopeNames = new ArrayList<>();
        for (Entry<String, ArrayList<Symbol>> entry : symbolTable) {
            sortedScopeNames.add(entry.getKey());
        }
        // Sort the scope names alphabetically
        Collections.sort(sortedScopeNames);

        // Iterate through the sorted scope names
        for (String scopeName : sortedScopeNames) {
            // If the scope name starts with "BLOCK", process it
            if (scopeName.startsWith("BLOCK")) {
                // Append the scope name to the output
                output.append("Symbol table ").append(scopeName).append("\n");

                // Retrieve the symbols for the current scope name
                ArrayList<Symbol> symbols = null;
                for (Entry<String, ArrayList<Symbol>> entry : symbolTable) {
                    if (entry.getKey().equals(scopeName)) {
                        symbols = entry.getValue();
                        break;
                    }
                }

                // Append each symbol in the scope to the output
                for (Symbol symbol : symbols) {
                    output.append(symbol).append("\n");
                }
                // Add a newline after each scope's symbols
                output.append("\n");
            }
        }

        // Check if there is a "main" scope and process it
        if (symbolTable.stream().anyMatch(entry -> entry.getKey().equals("main"))) {
            // Append the "main" scope name to the output
            output.append("Symbol table main\n");

            // Retrieve the symbols for the "main" scope
            ArrayList<Symbol> mainSymbols = null;
            for (Entry<String, ArrayList<Symbol>> entry : symbolTable) {
                if (entry.getKey().equals("main")) {
                    mainSymbols = entry.getValue();
                    break;
                }
            }

            // Append each symbol in the "main" scope to the output
            for (Symbol symbol : mainSymbols) {
                output.append(symbol).append("\n");
            }
            // Add a newline after the "main" scope's symbols
            output.append("\n");
        }
    }

// called when the parser enters a variable declaration rule in the grammar.
        @Override
        public void enterVar_decl(LittleParser.Var_declContext ctx) {
            // Get the variable type (e.g., INT or FLOAT) from the first child of the rule.
            String type = ctx.getChild(0).getText();
            // Get the list of variables from the second child of the rule, and split them using a comma.
            String[] vars = ctx.getChild(1).getText().split(",");

            // For each variable in the list of variables, create a new Symbol with its name and type, and define it in the symbol table.
            for (String var : vars) {
                defineSymbol(new Symbol(var, type));
            }
        }

// called when the parser enters a string declaration rule in the grammar.
        @Override
        public void enterString_decl(LittleParser.String_declContext ctx) {
            // Get the string name and value from the rule's children.
            String name = ctx.getChild(1).getText();
            String value = ctx.getChild(3).getText();
            // Define a new Symbol for the string with its name, type "STRING", and value, in the symbol table.
            defineSymbol(new Symbol(name, "STRING", value));
        }

// called when the parser enters a function declaration rule in the grammar.
        @Override
        public void enterFunc_decl(LittleParser.Func_declContext ctx) {
            // Get the function name from the rule.
            String functionName = ctx.id().getText();

            // If the function is not the "main" function, enter a new scope with the function's name; otherwise, enter the "main" scope.
            if (!functionName.equals("main")) {
                enterScope(functionName);
            } else {
                enterScope("main");
            }
            // Increment the scope level counter.
            scopeLevel++;
        }

// called when the parser exits a function body rule in the grammar.
        @Override
        public void exitFunc_body(LittleParser.Func_bodyContext ctx) {
            // Decrement the scope level counter.
            scopeLevel--;
            // Exit the current scope.
            exitScope();
        }

// called when the parser enters a parameter declaration list rule in the grammar.
        @Override
        public void enterParam_decl_list(LittleParser.Param_decl_listContext ctx) {
            // If there are children in the parameter list, process them.
            if (ctx.children != null) {
                // Iterate through all children in the list.
                for (ParseTree child : ctx.children) {
                    // If the child is a parameter declaration rule, process it.
                    if (child instanceof LittleParser.Param_declContext) {
                        LittleParser.Param_declContext param = (LittleParser.Param_declContext) child;
                        // Get the parameter's type and name.
                        String type = param.getChild(0).getText();
                        String name = param.getChild(1).getText();
                        // Define a new Symbol for the parameter with its name and type, in the symbol table.
                        defineSymbol(new Symbol(name, type));
                    }
                }
            }
        }

// keeps track of nested statement lists.
        private int nestedStmtListCounter = 0;

// checks if a statement list is nested within a control structure (e.g., if, else, or while).
        private boolean isNestedInControlStructure(LittleParser.Stmt_listContext ctx) {
            // Start with the parent of the statement list.
            ParseTree current = ctx.getParent();
            // Continue traversing the parent nodes until there are no more parents.
            while (current != null) {
                // If the current parent is an if, else, or while statement, the statement list is nested in a control structure.
                if (current instanceof LittleParser.If_stmtContext ||
                        current instanceof LittleParser.Else_stmtContext ||
                        current instanceof LittleParser.While_stmtContext) {
                    return true;
                }
                // Move to the next parent.
                current = current.getParent();
            }
            // If no control structure was found during the traversal, the statement list is not nested in a control structure.
            return false;
        }

// called when the parser enters a statement list rule in the grammar.
        @Override
        public void enterStmt_list(LittleParser.Stmt_listContext ctx) {
            // Check if the statement list is a direct child of a control structure or a function body.
            boolean isDirectChildOfControlStructure = ctx.getParent() instanceof LittleParser.Func_bodyContext ||
                    ctx.getParent() instanceof LittleParser.If_stmtContext ||
                    ctx.getParent() instanceof LittleParser.Else_stmtContext ||
                    ctx.getParent() instanceof LittleParser.While_stmtContext;
            // Check if the statement list is nested in a control structure.
            boolean isNestedInControlStructure = isNestedInControlStructure(ctx);

            // If the statement list is a direct child of a control structure or a function body and not nested in a control structure, enter a new scope.
            if (isDirectChildOfControlStructure && !isNestedInControlStructure) {
                enterScope("BLOCK " + blockCounter);
                blockCounterStack.push(blockCounter);
                blockCounter++;

                if (currentScope.equals("main")) {
                    mainBlockCounters.add(blockCounter - 1);
                } else {
                    blockCounters.add(blockCounter - 1);
                }

                scopeLevel++;
            } else if (!isDirectChildOfControlStructure && !isNestedInControlStructure) {
                // If the statement list is not a direct child of a control structure or a function body and not nested in a control structure, enter a new scope.
                enterScope("BLOCK " + blockCounter);
                blockCounterStack.push(blockCounter);
                blockCounter++;

                if (currentScope.equals("main")) {
                    mainBlockCounters.add(blockCounter - 1);
                } else {
                    blockCounters.add(blockCounter - 1);
                }

                scopeLevel++;
            }
        }

// called when the parser exits a statement list rule in the grammar.
        @Override
        public void exitStmt_list(LittleParser.Stmt_listContext ctx) {
            // Check if the statement list is a direct child of a control structure or a function body.
            boolean isDirectChildOfControlStructure = ctx.getParent() instanceof LittleParser.Func_bodyContext ||
                    ctx.getParent() instanceof LittleParser.If_stmtContext ||
                    ctx.getParent() instanceof LittleParser.Else_stmtContext ||
                    ctx.getParent() instanceof LittleParser.While_stmtContext;
            // Check if the statement list is nested in a control structure.
            boolean isNestedInControlStructure = isNestedInControlStructure(ctx);

            // If the statement list is a direct child of a control structure or a function body and not nested in a control structure, exit the current scope.
            if (isDirectChildOfControlStructure && !isNestedInControlStructure) {
                // Decrement the scope level.
                scopeLevel--;

                // Get the last entry in the symbol table.
                Map.Entry<String, ArrayList<Symbol>> entry = symbolTable.get(symbolTable.size() - 1);
                // Get the list of symbols in the entry.
                ArrayList<Symbol> symbols = entry.getValue();
                // If the list of symbols is empty, remove the entry from the symbol table.
                if (symbols.isEmpty()) {
                    symbolTable.remove(symbolTable.size() - 1);
                }

                // Call the exitScope method to perform any necessary cleanup before exiting the current scope.
                exitScope();
                // If the blockCounterStack is not empty, update the blockCounter to the next available value.
                if (!blockCounterStack.isEmpty()) {
                    int lastBlockCounter = blockCounterStack.pop();
                    blockCounter = lastBlockCounter + 1;
                }
            } else if (!isDirectChildOfControlStructure && !isNestedInControlStructure) {
                // If the statement list is not a direct child of a control structure or a function body and not nested in a control structure, exit the current scope.
                scopeLevel--;

                // Get the last entry in the symbol table.
                Map.Entry<String, ArrayList<Symbol>> entry = symbolTable.get(symbolTable.size() - 1);
                // Get the list of symbols in the entry.
                ArrayList<Symbol> symbols = entry.getValue();
                // If the list of symbols is empty, remove the entry from the symbol table.
                if (symbols.isEmpty()) {
                    symbolTable.remove(symbolTable.size() - 1);
                }

                // Call the exitScope method to perform any necessary cleanup before exiting the current scope.
                exitScope();
                // If the blockCounterStack is not empty, update the blockCounter to the next available value.
                if (!blockCounterStack.isEmpty()) {
                    int lastBlockCounter = blockCounterStack.pop();
                    blockCounter = lastBlockCounter + 1;
                }
            }
        }

// called when the parser exits an if statement rule in the grammar.
        @Override
        public void exitIf_stmt(LittleParser.If_stmtContext ctx) {
            // Decrement the scope level.
            scopeLevel--;
            // Call the exitScope method to perform any necessary cleanup before exiting the current scope.
            exitScope();
        }

// called when the parser exits an else statement rule in the grammar.
        @Override
        public void exitElse_stmt(LittleParser.Else_stmtContext ctx) {
            // Decrement the scope level.
            scopeLevel--;
            // Call the exitScope method to perform any necessary cleanup before exiting the current scope.
            exitScope();
        }

// called when the parser exits a while statement rule in the grammar.
        @Override
        public void exitWhile_stmt(LittleParser.While_stmtContext ctx) {
            // Decrement the scope level since we're exiting the while statement's scope.
            scopeLevel--;
            // Call the exitScope method to perform any necessary cleanup before exiting the current scope.
            exitScope();
        }

// called when the parser enters an if statement rule in the grammar.
        @Override
        public void enterIf_stmt(LittleParser.If_stmtContext ctx) {
            // Call the enterScope method to create a new scope with the name "BLOCK" followed by the current block counter value.
            enterScope("BLOCK " + blockCounter++);
            // Increment the scope level since we're entering a new scope for the if statement.
            scopeLevel++;
        }

// called when the parser enters an else statement rule in the grammar.
        @Override
        public void enterElse_stmt(LittleParser.Else_stmtContext ctx) {
            // Call the enterScope method to create a new scope with the name "BLOCK" followed by the current block counter value.
            enterScope("BLOCK " + blockCounter++);
            // Increment the scope level since we're entering a new scope for the else statement.
            scopeLevel++;
        }

// called when the parser enters a while statement rule in the grammar.
        @Override
        public void enterWhile_stmt(LittleParser.While_stmtContext ctx) {
            // Call the enterScope method to create a new scope with the name "BLOCK" followed by the current block counter value.
            enterScope("BLOCK " + blockCounter++);
            // Increment the scope level since we're entering a new scope for the while statement.
            scopeLevel++;
        }

}

