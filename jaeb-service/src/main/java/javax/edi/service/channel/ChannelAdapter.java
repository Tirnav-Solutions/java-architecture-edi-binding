package javax.edi.service.channel;

import java.util.List;

import javax.edi.service.entity.CommunicationChannel;

/**
 * Abstraction for communication channel operations.
 * Each protocol (SFTP, FTP, AS2, HTTP) provides its own adapter.
 */
public interface ChannelAdapter {

    /** List files available in the channel's remote directory. */
    List<RemoteFile> listFiles(CommunicationChannel channel);

    /** Read a remote file's content as a string. */
    String readFile(CommunicationChannel channel, String remotePath);

    /** Write content to a remote file. */
    void writeFile(CommunicationChannel channel, String remotePath, String content);

    /** Move a remote file (used for archiving). */
    void moveFile(CommunicationChannel channel, String fromPath, String toPath);

    /** Delete a remote file. */
    void deleteFile(CommunicationChannel channel, String remotePath);

    /** Simple DTO for a remote file listing. */
    class RemoteFile {
        private final String name;
        private final String path;
        private final long size;

        public RemoteFile(String name, String path, long size) {
            this.name = name;
            this.path = path;
            this.size = size;
        }

        public String getName() { return name; }
        public String getPath() { return path; }
        public long getSize() { return size; }
    }
}
