import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is used to store remote peer details information
 */
public class RemotePeerInfo {
    private String peerId;
    private String hostAddress;
    private String port;
    private int hasFile;
    private int index;
    private int peerState = -1;
    private int previousPeerState = -1;
    private int isPreferredNeighbor = 0;
    private BitFieldMessage bitFieldMessage;
    private int isOptimisticallyUnchokedNeighbor;
    private int isInterested;
    private int isHandshaked;
    private int isChoked;
    private int isComplete;
    private Date startTime;
    private Date endTime;
    private double downloadRate;

    /**
     * Initializes a new instance of RemotePeerDetails.
     */
    public RemotePeerInfo(String peerId, String hostAddress, String port, int hasFile, int index) {
        this.peerId = peerId;
        this.hostAddress = hostAddress;
        this.port = port;
        this.hasFile = hasFile;
        this.index = index;
        this.downloadRate = 0;
        this.isOptimisticallyUnchokedNeighbor = 0;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String id) {
        this.peerId = id;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    /**
     * @return 1 - peer has file; 0 - peer does not have file
     */
    public int getHasFile() {
        return hasFile;
    }

    public void setHasFile(int hasFile) {
        this.hasFile = hasFile;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * @return 1 - peer is preferred neighbor; 0 - peer is not preferred neighbor
     */
    public int getIsPreferredNeighbor() {
        return this.isPreferredNeighbor;
    }

    public void setIsPreferredNeighbor(int isPreferredNeighbor) {
        this.isPreferredNeighbor = isPreferredNeighbor;
    }

    public int getPeerState() {
        return peerState;
    }

    public void setPeerState(int peerState) {
        this.peerState = peerState;
    }

    public BitFieldMessage getBitFieldMessage() {
        return bitFieldMessage;
    }

    public void setBitFieldMessage(BitFieldMessage bitFieldMessage) {
        this.bitFieldMessage = bitFieldMessage;
    }

    public int getIsInterested() {
        return isInterested;
    }

    public void setIsInterested(int isInterested) {
        this.isInterested = isInterested;
    }

    public int getIsHandShaked() {
        return isHandshaked;
    }

    public void setIsHandShaked(int isHandShaked) {
        this.isHandshaked = isHandShaked;
    }

    public int getIsChoked() {
        return isChoked;
    }

    public void setIsChoked(int isChoked) {
        this.isChoked = isChoked;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public double getDataRate() {
        return downloadRate;
    }

    public void setDataRate(double dataRate) {
        this.downloadRate = dataRate;
    }

    public int getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(int isComplete) {
        this.isComplete = isComplete;
    }

    public int getIsOptimisticallyUnchockedNeighbor() {
        return isOptimisticallyUnchokedNeighbor;
    }

    public void setIsOptimisticallyUnchockedNeighbor(int isOptimisticallyUnchockedNeighbor) {
        this.isOptimisticallyUnchokedNeighbor = isOptimisticallyUnchockedNeighbor;
    }

    public int getPreviousPeerState() {
        return previousPeerState;
    }

    public void setPreviousPeerState(int previousPeerState) {
        this.previousPeerState = previousPeerState;
    }

    /**
     * Updates the peer details in the configuration file.
     *
     * @param currentPeerId - The ID of the peer to be updated.
     * @param hasFile       - The updated file availability status.
     * @throws IOException
     */
    public void updatePeerDetails(String currentPeerId, int hasFile) throws IOException {
        Path configPath = Paths.get("PeerInfo.cfg");
        Stream<String> linesStream = Files.lines(configPath);

        List<String> modifiedLines = linesStream.map(line -> {
            String[] components = line.trim().split("\\s+");
            if (components[0].equals(currentPeerId)) {
                return components[0] + " " + components[1] + " " + components[2] + " " + hasFile;
            } else {
                return line;
            }
        }).collect(Collectors.toList());

        Files.write(configPath, modifiedLines);
        linesStream.close();
    }

    /**
     * Compares the download rates of two peers.
     *
     * @param otherPeer - Another peer to compare with.
     * @return - The comparison result of download rates.
     */
    public int compareTo(RemotePeerInfo otherPeer) {
        if (this.downloadRate > otherPeer.downloadRate)
            return 1;
        else if (this.downloadRate == otherPeer.downloadRate)
            return 0;
        else
            return -1;
    }
}
