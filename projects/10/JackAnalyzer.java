import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class JackAnalyzer {

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
            CompilationEngine ce = new CompilationEngine(f, new File(f.getAbsolutePath().replaceFirst("[.][^.]+$", ".xml")));
            ce.compileClass();
        }
    }
}
