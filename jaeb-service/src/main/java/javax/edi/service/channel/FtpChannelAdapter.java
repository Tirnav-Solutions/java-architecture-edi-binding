package javax.edi.service.channel;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.edi.service.entity.CommunicationChannel;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * FTP/FTPS implementation of ChannelAdapter using Apache Commons Net.
 */
@Component
public class FtpChannelAdapter implements ChannelAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(FtpChannelAdapter.class);

    @Override
    public List<RemoteFile> listFiles(CommunicationChannel channel) {
        List<RemoteFile> result = new ArrayList<>();
        withFtp(channel, ftp -> {
            String dir = channel.getRemoteDirectory();
            if (dir == null || dir.isEmpty()) dir = ".";
            FTPFile[] files = ftp.listFiles(dir);
            if (files != null) {
                for (FTPFile f : files) {
                    if (!f.isFile()) continue;
                    String name = f.getName();
                    if (name.startsWith(".") || !isEdiFile(name)) continue;
                    result.add(new RemoteFile(name, dir + "/" + name, f.getSize()));
                }
            }
        });
        return result;
    }

    @Override
    public String readFile(CommunicationChannel channel, String remotePath) {
        final String[] content = {null};
        withFtp(channel, ftp -> {
            try (InputStream is = ftp.retrieveFileStream(remotePath);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (is == null) throw new RuntimeException("File not found: " + remotePath);
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
                content[0] = baos.toString(StandardCharsets.UTF_8.name());
                ftp.completePendingCommand();
            }
        });
        return content[0];
    }

    @Override
    public void writeFile(CommunicationChannel channel, String remotePath, String content) {
        withFtp(channel, ftp -> {
            try (OutputStream os = ftp.storeFileStream(remotePath)) {
                if (os == null) throw new RuntimeException("Cannot write to: " + remotePath);
                os.write(content.getBytes(StandardCharsets.UTF_8));
            }
            ftp.completePendingCommand();
        });
    }

    @Override
    public void moveFile(CommunicationChannel channel, String fromPath, String toPath) {
        withFtp(channel, ftp -> {
            // Ensure target directory
            String dir = toPath.substring(0, toPath.lastIndexOf('/'));
            ftp.makeDirectory(dir); // Silently fails if exists
            ftp.rename(fromPath, toPath);
        });
    }

    @Override
    public void deleteFile(CommunicationChannel channel, String remotePath) {
        withFtp(channel, ftp -> ftp.deleteFile(remotePath));
    }

    // -------- helpers --------

    private void withFtp(CommunicationChannel ch, FtpAction action) {
        FTPClient ftp = ch.isUseTls() ? new FTPSClient() : new FTPClient();
        try {
            ftp.setConnectTimeout(ch.getConnectionTimeoutMs());
            ftp.connect(ch.getHost(), ch.getPort() != null ? ch.getPort() : 21);
            ftp.login(ch.getUsername(), ch.getPassword());
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            if (ch.isPassiveMode()) ftp.enterLocalPassiveMode();
            action.execute(ftp);
        } catch (Exception e) {
            throw new RuntimeException("FTP operation failed (" + ch.getHost() + "): " + e.getMessage(), e);
        } finally {
            try {
                if (ftp.isConnected()) { ftp.logout(); ftp.disconnect(); }
            } catch (Exception ignored) {}
        }
    }

    private boolean isEdiFile(String name) {
        String l = name.toLowerCase();
        return l.endsWith(".edi") || l.endsWith(".x12") || l.endsWith(".txt") || l.endsWith(".dat");
    }

    @FunctionalInterface
    private interface FtpAction {
        void execute(FTPClient ftp) throws Exception;
    }
}
