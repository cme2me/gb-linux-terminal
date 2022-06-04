import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerService {
    private ServerSocketChannel server;
    private Selector selector;
    private ByteBuffer buffer;
    private Path files;
    private Path path = Path.of(System.getProperty("user.home"));
    public ServerService() throws IOException {
        server = ServerSocketChannel.open();
        selector = Selector.open();
        server.bind(new InetSocketAddress(8085));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        files = Path.of(System.getProperty("user.home"));
    }

    public void startServer() throws IOException {
        while (server.isOpen()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAcc();
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    private void handleAcc() throws IOException {
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        channel.write(ByteBuffer.wrap("Greetings, traveler!\n$".getBytes(StandardCharsets.UTF_8)));
    }

    private void handleRead(SelectionKey key) throws IOException {
        buffer = ByteBuffer.allocate(2048);
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();
        while (channel.isOpen()) {
            int read = channel.read(buffer);
            if (read < 0) {
                channel.close();
                return;
            }
            if (read == 0) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                sb.append((char) buffer.get());
            }
            buffer.clear();

        }
        String command = sb.toString().trim();
        if ("ls".equals(command)) {
            channel.write(ByteBuffer.wrap(showAllFiles(files).getBytes(StandardCharsets.UTF_8)));
        }
        if ("cat".equals(command)) {
            List<String> list = Files.readAllLines(path);
            for (String str : list) {
                channel.write(ByteBuffer.wrap((str + "\n").getBytes(StandardCharsets.UTF_8)));
            }
        }
        if (command.startsWith("cd")) {
            String[] strings = command.split(" +");
            if (strings.length == 2) {
                String directory = strings[1];
                Path goTo = path.resolve(directory);
                if (Files.exists(goTo)) {
                    if (Files.isDirectory(goTo)) {
                        path = goTo;
                        String ent = "$";
                        channel.write(ByteBuffer.wrap(ent.getBytes(StandardCharsets.UTF_8)));
                    }
                    else {
                        String ent = "Директории не существует";
                        channel.write(ByteBuffer.wrap(ent.getBytes(StandardCharsets.UTF_8)));
                    }
                }
            }
        }
        sb.append("$");
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        channel.write(ByteBuffer.wrap(bytes));
    }
    private String showAllFiles(Path path) throws IOException {
        return Files.list(path).map(f -> f.getFileName().toString()).collect(Collectors.joining("\n"));
    }
}
