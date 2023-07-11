package johnydecoder.main;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class DistanceBeam extends JavaPlugin implements CommandExecutor {
    // Configuration keys
    private static final String CONFIG_BEAM_SIZE = "beam-size";
    private static final String CONFIG_ROTATION_SPEED = "rotation-speed";
    private static final String CONFIG_PARTICLE_COUNT = "particle-count";
    private static final String CONFIG_PARTICLE_SPACING = "particle-spacing";
    private static final String CONFIG_CENTER_DISTANCE = "center-distance";
    private static final String CONFIG_BEAM_X = "beam-x";
    private static final String CONFIG_BEAM_Z = "beam-z";
    private static final String CONFIG_PARTICLE_TYPE = "particle-type";
    private static final String CONFIG_AMBIENT_SOUND = "ambient-sound";
    private static final String CONFIG_AMBIENT_VOLUME = "ambient-volume";
    private static final String CONFIG_AMBIENT_PITCH = "ambient-pitch";

    // Configuration values
    private double beamSize;
    private double rotationSpeed;
    private int particleCount;
    private double particleSpacing;
    private double centerDistance;
    private double beamX;
    private double beamZ;
    private Particle particleType;
    private Sound ambientSound;
    private float ambientVolume;
    private float ambientPitch;

    // Current rotation angle of the beacon beam
    private double rotationAngle = 0;

    private long lastAmbientSoundTime = 0;
    private long ambientSoundDelay = 1000; // Tiempo en milisegundos entre reproducciones de sonido
    private Sound lastAmbientSound = null;

    /**
     * Called when the plugin is enabled.
     */
    @Override
    public void onEnable() {
        // Save default config.yml if it doesn't exist
        saveDefaultConfig();

        // Load configuration values from config.yml
        loadConfig();

        // Register /distb command executor
        getCommand("distb").setExecutor(this);

        // Start beacon beam particle task
        new BukkitRunnable() {
            @Override
            public void run() {
                // Update rotation angle
                rotationAngle += rotationSpeed;

                // Show beacon beam particles to all online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    World world = player.getWorld();

                    // Only show particles in the overworld
                    if (world.getEnvironment() != Environment.NORMAL) {
                        continue;
                    }

                    Location playerLocation = player.getLocation();

                    // Only show particles if the player is outside
                    if (!isLocationOutside(world, playerLocation)) {
                        continue;
                    }

                    Location beamLocation = new Location(world, beamX, 0, beamZ);

                    // Play ambient sound if the player is close to the beam location
                    if (playerLocation.distance(beamLocation) <= centerDistance) {
                        long currentTime = System.currentTimeMillis();
                        if (ambientSound != lastAmbientSound || currentTime - lastAmbientSoundTime >= ambientSoundDelay) {
                            player.playSound(playerLocation, ambientSound, ambientVolume, ambientPitch);
                            lastAmbientSoundTime = currentTime;
                            lastAmbientSound = ambientSound;
                        }
                        continue;
                    }

                    // Show beacon beam particles in the direction of the beam location
                    Vector directionToCenter = beamLocation.toVector().subtract(playerLocation.toVector()).normalize();
                    Location particleStartLocation = playerLocation.add(directionToCenter.multiply(centerDistance));
                    for (int i = 0; i < particleCount; i++) {
                        double y = particleStartLocation.getY() + i * particleSpacing;

                        // Show particles in a rotating square shape
                        for (double x = -beamSize / 2; x <= beamSize / 2; x += particleSpacing) {
                            for (double z = -beamSize / 2; z <= beamSize / 2; z += particleSpacing) {
                                double rotatedX = x * Math.cos(rotationAngle) - z * Math.sin(rotationAngle);
                                double rotatedZ = x * Math.sin(rotationAngle) + z * Math.cos(rotationAngle);
                                Location particleLocation = new Location(world, particleStartLocation.getX() + rotatedX, y, particleStartLocation.getZ() + rotatedZ);
                                player.spawnParticle(particleType, particleLocation, 1, 0, 0, 0, 0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 1);
    }

    /**
     * Called when a player executes a command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle /distb command
        if (command.getName().equalsIgnoreCase("distb")) {
            // Handle /distb reload subcommand
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                // Reload config.yml
                reloadConfig();
                loadConfig();

                // Send confirmation message to the player
                sender.sendMessage("[DistanceBeam] Configuration reloaded.");

                return true;
            }
        }

        return false;
    }

    /**
     * Load configuration values from config.yml.
     */
    private void loadConfig() {
        FileConfiguration config = getConfig();

        // Load configuration values from config.yml
        beamSize = config.getDouble(CONFIG_BEAM_SIZE, 1.0);
        rotationSpeed = config.getDouble(CONFIG_ROTATION_SPEED, Math.PI / 60);
        particleCount = config.getInt(CONFIG_PARTICLE_COUNT, 300);
        particleSpacing = config.getDouble(CONFIG_PARTICLE_SPACING, 0.5);
        centerDistance = config.getDouble(CONFIG_CENTER_DISTANCE, 100.0);
        beamX = config.getDouble(CONFIG_BEAM_X, 0.0);
        beamZ = config.getDouble(CONFIG_BEAM_Z, 0.0);
        particleType = Particle.valueOf(config.getString(CONFIG_PARTICLE_TYPE, "END_ROD"));
        ambientSound = Sound.valueOf(config.getString(CONFIG_AMBIENT_SOUND, "BLOCK_CONDUIT_AMBIENT"));
        ambientVolume = (float) config.getDouble(CONFIG_AMBIENT_VOLUME, 1.0);
        ambientPitch = (float) config.getDouble(CONFIG_AMBIENT_PITCH, 1.0);
    }

    /**
     * Check if a location is outside (not in a cave or under a roof).
     */
    private boolean isLocationOutside(World world, Location location) {
        int highestBlockY = world.getHighestBlockYAt(location);
        return location.getY() >= highestBlockY;
    }
}


