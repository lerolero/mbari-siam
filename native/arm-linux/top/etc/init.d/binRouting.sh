#!/bin/bash
# Add route to surface-shore ppp network
route add -net 10.1.1.0 netmask 255.255.255.0 gw surface
