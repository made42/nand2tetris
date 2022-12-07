import java.util.HashMap;

/**
 * This module provides services for building, populating, and using symbol tables that keep track of the symbol
 * properties <i>name</i>, <i>type</i>, <i>kind</i>, and a running <i>index</i> for each kind.
 *
 * @author Maarten Derks
 */
class SymbolTable {

    private final HashMap<String, Object[]> ht;
    private int staticIndex, fieldIndex, argIndex, varIndex;

    /**
     * Creates a new symbol table.
     */
    SymbolTable() {
        ht = new HashMap<>();
        staticIndex = fieldIndex = argIndex = varIndex = 0;
    }

    /**
     * Empties the symbol table, and resets the four indexes to 0. Should be called when starting to compile a
     * subroutine declaration.
     */
    void reset() {
        ht.clear();
        staticIndex = fieldIndex = argIndex = varIndex = 0;
    }

    /**
     * Defines (adds to the table) a new variable of the given <code>name</code>, <code>type</code>, and
     * <code>kind</code>. Assigns to it the index value of that <code>kind</code>, and adds 1 to the index.
     *
     * @param name the <code>name</code> of the variable
     * @param type the <code>type</code> of the variable
     * @param kind the <code>kind</code> of the variable
     */
    void define(String name, String type, Kind kind) {
        switch (kind) {
            case STATIC:
                ht.put(name, new Object[]{type, kind, staticIndex++});
                break;
            case FIELD:
                ht.put(name, new Object[]{type, kind, fieldIndex++});
                break;
            case ARG:
                ht.put(name, new Object[]{type, kind, argIndex++});
                break;
            case VAR:
                ht.put(name, new Object[]{type, kind, varIndex++});
                break;
        }
    }

    /**
     * Returns the number of variables of the given <code>kind</code> already defined in the table.
     *
     * @param  kind <code>kind</code> to return the number of
     * @return      the number of variables of the given <code>kind</code>
     */
    int varCount(Kind kind) {
        int numberOfVariables = 0;
        for (String key : ht.keySet()) {
            if (kindOf(key).equals(kind)) {
                numberOfVariables++;
            }
        }
        return numberOfVariables;
    }

    /**
     * Returns the <code>kind</code> of the named identifier. If the identifier is not found, returns
     * {@link Kind#NONE NONE}.
     *
     * @param  name variable to return the kind of
     * @return      the <code>kind</code> of the named variable
     */
    Kind kindOf(String name) {
        return ht.get(name) == null ? Kind.NONE : (Kind) ht.get(name)[1];
    }

    /**
     * Returns the <code>type</code> of the named variable.
     *
     * @param  name variable to return the type of
     * @return      the <code>type</code> of the named variable
     */
    String typeOf(String name) {
        return (String) ht.get(name)[0];
    }

    /**
     * Returns the <code>index</code> of the named variable.
     *
     * @param  name variable to return the index of
     * @return      the <code>index</code> of the named variable
     */
    int indexOf(String name) {
        return (int) ht.get(name)[2];
    }
}
