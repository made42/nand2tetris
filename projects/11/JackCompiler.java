import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This module drives the compilation process. It operates on either a file name of the form
 * <i>Xxx</i><code>.jack</code> or on a folder name containing one or more such files. For each source
 * <i>Xxx</i><code>.jack</code> file, the program
 * <ol>
 *     <li>creates a {@link JackTokenizer} from the <i>Xxx</i><code>.jack</code> input file;</li>
 *     <li>creates an output file named <i>Xxx</i><code>.vm</code>; and</li>
 *     <li>uses a {@link CompilationEngine}, a {@link SymbolTable}, and a {@link VMWriter} for parsing the input file
 *     and emitting the translated VM code into the output file.</li>
 * </ol>
 *
 * @author Maarten Derks
 */
class JackCompiler {

    public static void main(String[] args) throws Exception {
        List<File> files = new ArrayList<>();

        File in = new File(args[0]);
        if (in.isFile()) {
            files.add(in.getAbsoluteFile());
        } else {
            FilenameFilter filter = (dir, name) -> name.matches(".*.jack");
            files.addAll(Arrays.asList(Objects.requireNonNull(in.listFiles(filter))));
        }

        for (File f : files) {
            CompilationEngine ce = new CompilationEngine(f, new File(f.getAbsolutePath().replaceFirst("[.][^.]+$", ".vm")));
            ce.compileClass();
        }
    }
}
