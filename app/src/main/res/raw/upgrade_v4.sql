UPDATE entries SET updated = (CAST((julianday('now') - 2440587.5)*86400000.0 AS INTEGER)), synced = 0 WHERE shared = 1;
