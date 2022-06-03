import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CommandsService {
    public void filesList(Files file) {
        Path path = Path.of(String.valueOf(file));
        List<Path> files = new ArrayList<>();

    }
}
