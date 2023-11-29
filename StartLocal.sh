#!/bin/bash

while read peerId hostName port hasFile; do
    echo "$peerId peer getting started"
    java peerProcess $peerId > $peerId.console.log &
    sleep 1
done < PeerInfo.cfg
echo "Running.. Check peer log files created in this directory for updates"