import java.util.Comparator;

/**
 * Comparator class for sorting RemotePeerInfo objects based on their download rates.
 */
public class PeerDownloadComparator implements Comparator<RemotePeerInfo> {

	private boolean isFirstGreater;

	/**
	 * Default constructor for PeerDownloadComparator.
	 */
	public PeerDownloadComparator() {
		this.isFirstGreater = true;
	}

	/**
	 * Constructor with a boolean parameter to set the sorting order.
	 * @param order Determines the sorting order.
	 */
	public PeerDownloadComparator(boolean order) {
		this.isFirstGreater = order;
	}

	/**
	 * Compares two RemotePeerInfo objects based on their download rates.
	 * @param peer1 The first peer to compare.
	 * @param peer2 The second peer to compare.
	 * @return An integer indicating the comparison result: 1 if peer1 > peer2, -1 if peer1 < peer2, 0 if equal.
	 */
	public int compare(RemotePeerInfo peer1, RemotePeerInfo peer2) {
		if (peer1 == null && peer2 == null) return 0;
		if (peer1 == null) return 1;
		if (peer2 == null) return -1;

		if (peer1 instanceof Comparable) {
			return isFirstGreater ? peer1.compareTo(peer2) : peer2.compareTo(peer1);
		} else {
			return isFirstGreater ? peer1.toString().compareTo(peer2.toString()) : peer2.toString().compareTo(peer1.toString());
		}
	}
}
