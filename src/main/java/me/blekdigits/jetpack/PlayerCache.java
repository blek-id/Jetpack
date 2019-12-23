package me.blekdigits.jetpack;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerCache {

	@Getter @Setter
	private static final Map<UUID, BukkitTask> runningTasks = new HashMap<>();
}
