package com.redeskyller.bukkit.solaryeconomy.config;

public final class GeneralSettings {

	private final String world;
	private final boolean useVault;
	private final int saveIntervalSeconds;
	private final boolean auditLog;
	private final boolean checkUpdates;
	private final boolean debug;

	public GeneralSettings(String world, boolean useVault, int saveIntervalSeconds, boolean auditLog,
			boolean checkUpdates, boolean debug) {
		this.world = world;
		this.useVault = useVault;
		this.saveIntervalSeconds = saveIntervalSeconds;
		this.auditLog = auditLog;
		this.checkUpdates = checkUpdates;
		this.debug = debug;
	}

	/** Main world, used to look up Vault prefixes of offline players. */
	public String getWorld() {
		return this.world;
	}

	public boolean isUseVault() {
		return this.useVault;
	}

	public int getSaveIntervalSeconds() {
		return this.saveIntervalSeconds;
	}

	public boolean isAuditLog() {
		return this.auditLog;
	}

	public boolean isCheckUpdates() {
		return this.checkUpdates;
	}

	public boolean isDebug() {
		return this.debug;
	}
}
