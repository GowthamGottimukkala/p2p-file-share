JCC = javac
JAVA = java
JFLAGS = -g
REMOTE_PEER_OPTS = -cp .:jsch.jar

default: peerProcess.class

all: peerProcess.class \
     MsgQueue.class MsgDetails.class MsgConstants.class \
     BaseMsg.class LoggingHelper.class LoggingFormatter.class HandshakeMsg.class \
     FilePiece.class PeerUtils.class ConfigSettings.class \
     BitFieldMessage.class

peerProcess.class: peerProcess.java
	$(JCC) $(JFLAGS) peerProcess.java

RemotePeerInfo.class: RemotePeerInfo.java
	$(JCC) $(JFLAGS) RemotePeerInfo.java

MsgQueue.class: MsgQueue.java
	$(JCC) $(JFLAGS) MsgQueue.java

MsgDetails.class: MsgDetails.java
	$(JCC) $(JFLAGS) MsgDetails.java

MsgConstants.class: MsgConstants.java
	$(JCC) $(JFLAGS) MsgConstants.java

BaseMsg.class: BaseMsg.java
	$(JCC) $(JFLAGS) BaseMsg.java

LoggingHelper.class: LoggingHelper.java
	$(JCC) $(JFLAGS) LoggingHelper.java

LoggingFormatter.class: LoggingFormatter.java
	$(JCC) $(JFLAGS) LoggingFormatter.java

HandshakeMsg.class: HandshakeMsg.java
	$(JCC) $(JFLAGS) HandshakeMsg.java

FilePiece.class: FilePiece.java
	$(JCC) $(JFLAGS) FilePiece.java

PeerUtils.class: PeerUtils.java
	$(JCC) $(JFLAGS) PeerUtils.java

ConfigSettings.class: ConfigSettings.java
	$(JCC) $(JFLAGS) ConfigSettings.java

BitFieldMessage.class: BitFieldMessage.java
	$(JCC) $(JFLAGS) BitFieldMessage.java

peerProcess: peerProcess.class
	$(JAVA) peerProcess 1001

clean:
	$(RM) *.class
