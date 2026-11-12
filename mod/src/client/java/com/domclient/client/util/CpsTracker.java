package com.domclient.client.util;

import net.minecraft.util.Util;

import java.util.ArrayDeque;
import java.util.Deque;

/** Counts mouse clicks in the last second. Fed by the mouse mixin. */
public final class CpsTracker {
	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();

	private CpsTracker() {
	}

	public static synchronized void onClick(int button) {
		long now = Util.getMillis();
		if (button == 0) {
			LEFT.addLast(now);
		} else if (button == 1) {
			RIGHT.addLast(now);
		}
	}

	public static synchronized int left() {
		return count(LEFT);
	}

	public static synchronized int right() {
		return count(RIGHT);
	}

	private static int count(Deque<Long> clicks) {
		long cutoff = Util.getMillis() - 1000;
		while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
			clicks.pollFirst();
		}
		return clicks.size();
	}
}
