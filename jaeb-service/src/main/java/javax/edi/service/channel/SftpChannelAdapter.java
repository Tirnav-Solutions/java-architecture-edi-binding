package javax.edi.service.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.edi.service.entity.CommunicationChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * SFTP implementation of ChannelAdapter using JSch.
 */
@Component
public class SftpChannelAdapter implements ChannelAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SftpChannelAdapter.class);

    @Override
    public List<RemoteFile> listFiles(CommunicationChannel channel) {
        List<RemoteFile> result = new ArrayList<>();
        withSftp(channel, sftp -> {
            String dir = channel.getRemoteDirectory();
            if (dir == null || dir.isEmpty()) dir = ".";
            Vector<?> entries = sftp.ls(dir);
            for (Object e : entries) {
                if (e instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) e;
                    String name = entry.getFilename();
                    if (name.equals(".") || name.equals("..") || name.startsWith(".")) continue;
                    if (entry.getAttrs().isDir()) continue;
                    if (!isEdiFile(name)) continue;
                    result.add(new RemoteFile(name, dir + "/" + name, entry.getAttrs().getSize()));
                }
            }
        });
        return result;
    }

    @Override
    public String readFile(CommunicationChannel channel, String remotePath) {
        final String[] content = {null};
        withSftp(channel, sftp -> {
            try (InputStream is = sftp.get(remotePath);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
                content[0] = baos.toString(StandardCharsets.UTF_8.name());
            }
        });
        return content[0];
    }

    @Override
    public void writeFile(CommunicationChannel channel, String remotePath, String content) {
        withSftp(channel, sftp -> {
            sftp.put(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), remotePath);
        });
    }

    @Override
    public void moveFile(CommunicationChannel channel, String fromPath, String toPath) {
        withSftp(channel, sftp -> {
            // Ensure target directory exists
            String dir = toPath.substring(0, toPath.lastIndexOf('/'));
            try { sftp.stat(dir); } catch (Exception e) {
                try { sftp.mkdir(dir); } catch (Exception ignored) {}
            }
            sftp.rename(fromPath, toPath);
        });
    }

    @Override
    public void deleteFile(CommunicationChannel channel, String remotePath) {
        withSftp(channel, sftp -> sftp.rm(remotePath));
    }

    // -------- helpers --------

    private void withSftp(CommunicationChannel ch, SftpAction action) {
        Session session = null;
        ChannelSftp sftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(ch.getUsername(), ch.getHost(),
                    ch.getPort() != null ? ch.getPort() : 22);
            session.setPassword(ch.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(ch.getConnectionTimeoutMs());
            session.connect();

            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            action.execute(sftp);

        } catch (Exception e) {
            throw new RuntimeException("SFTP operation failed (" + ch.getHost() + "): " + e.getMessage(), e);
        } finally {
            if (sftp != null && sftp.isConnected()) sftp.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    private boolean isEdiFile(String name) {
        String l = name.toLowerCase();
        return l.endsWith(".edi") || l.endsWith(".x12") || l.endsWith(".txt") || l.endsWith(".dat");
    }

    @FunctionalInterface
    private interface SftpAction {
        void execute(ChannelSftp sftp) throws Exception;
    }
}
