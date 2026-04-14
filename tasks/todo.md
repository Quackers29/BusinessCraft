# Phase 12 ✅ VM Cache/Sync

## Phase 12.3 Packet Boilerplate ✅
**Dupe:** 6+ ViewModelSyncPacket writeVM/readVM identical.
**Plan:** BaseViewModelSyncPacket<T extends ViewModel> generic write/read. Extend for Resource/Prod etc.
Effort: 50L base + refactor 6 packets.
Benefit: 90→30L/packet.


