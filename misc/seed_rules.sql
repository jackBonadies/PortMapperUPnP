--  App Inspection > Database Inspector > app.db > "Open New Query Tab" (must be debuggable build)

INSERT INTO devices (deviceSignature, useWildcardForRemoteHostDelete, lastKnownIp, displayName, friendlyName, lastSeenAtUtcMs)
VALUES ('uuid:old-router-0001', 0, '192.168.0.1', 'Old Router', 'Old Router', strftime('%s','now')*1000);

INSERT INTO port_mappings
 (deviceSignature, protocol, externalPort, deviceIp, description, internalIp, internalPort,
	  autorenew, desiredLeaseDuration, autorenewManualCadence, desiredEnabled, createdAtUtcMs, lastSeenAtUtcMs)
VALUES
 ('uuid:old-router-0001','TCP',25565,'192.168.0.1','minecraft','192.168.0.42',25565,0,0,-1,1,strftime('%s','now')*1000,strftime('%s','now')*1000),
 ('uuid:old-router-0001','UDP',51820,'192.168.0.1','wireguard','192.168.0.42',51820,1,3600,-1,1,strftime('%s','now')*1000,strftime('%s','now')*1000);
 ('uuid:old-router-0001','TCP',51820,'192.168.0.1','bittorrentx','192.168.0.42',51820,1,3600,-1,1,strftime('%s','now')*1000,strftime('%s','now')*1000);
 ('uuid:old-router-0001','TCP',51820,'192.168.0.1','bittorrenty','192.168.0.44',51820,1,3600,-1,1,strftime('%s','now')*1000,strftime('%s','now')*1000);
