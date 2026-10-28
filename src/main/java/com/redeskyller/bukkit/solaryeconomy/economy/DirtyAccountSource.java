package com.redeskyller.bukkit.solaryeconomy.economy;

import java.util.Collection;
import java.util.List;

/** What the write-behind persistence needs from the economy: the accounts changed since the last flush. */
public interface DirtyAccountSource {

	/**
	 * Takes ownership of every pending change. Changes made while the returned snapshots are being
	 * written mark their account dirty again, so nothing is lost.
	 */
	List<AccountSnapshot> drainDirty();

	/** Puts accounts back in the pending set after a failed write. */
	void restoreDirty(Collection<String> keys);
}
