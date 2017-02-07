package me.ardacraft.warps;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.service.NucleusWarpService;
import me.dags.data.NodeAdapter;
import me.dags.data.node.Node;
import me.dags.data.node.NodeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.Optional;

/**
 * @author dags <dags@dags.me>
 */
@Plugin(id = "warpconverter", name = "WarpConverter", version = "1.0", dependencies = @Dependency(id = "nucleus"))
public class WarpConverter {

    private final Logger logger = LoggerFactory.getLogger("WarpConverter");
    private final Path configDir;

    @Inject
    public WarpConverter(@ConfigDir(sharedRoot = true) Path dir) {
        this.configDir = dir;
    }

    @Listener
    public void postInit(GamePostInitializationEvent event) {
        NucleusWarpService service = Sponge.getServiceManager().provideUnchecked(NucleusWarpService.class);
        Node node = NodeAdapter.hocon().from(configDir.resolve("convert-warps.conf"));

        if (node.isPresent() && node.isNodeObject()) {

            node.asNodeObject().entries().stream()
                    .filter(e -> e.getValue().isNodeObject())
                    .forEach(e -> {
                        NodeObject warp = e.getValue().asNodeObject();

                        String name = e.getKey().asString();
                        String worldName = warp.map("world", Node::asString, "");
                        Optional<World> world = Sponge.getServer().getWorld(worldName);

                        double x = warp.map("x", n -> n.asNumber().doubleValue(), Double.MIN_VALUE);
                        double y = warp.map("y", n -> n.asNumber().doubleValue(), Double.MIN_VALUE);
                        double z = warp.map("z", n -> n.asNumber().doubleValue(), Double.MIN_VALUE);
                        double pitch = warp.map("pitch", n -> n.asNumber().doubleValue(), Double.MIN_VALUE);
                        double yaw = warp.map("yaw", n -> n.asNumber().doubleValue(), Double.MIN_VALUE);

                        if (valid(name) && world.isPresent() && valid(x) && valid(y) && valid(z) && valid(pitch) && valid(yaw)) {
                            Location<World> location = new Location<>(world.get(), x, y, z);
                            Vector3d rotation = new Vector3d(pitch, yaw, 0D);
                            if (service.setWarp(name, location, rotation)) {
                                logger.info("Converted warp {} to Nuclues warps", name);
                            } else {
                                logger.info("Unable to convert warp {} to Nuclues warps. It probably already exists", name);
                            }
                        } else {
                            logger.info("Found invalid warp: name={} data={}", name, warp);
                        }
                    });
        }
    }

    private static boolean valid(String s) {
        return s != null && !s.isEmpty();
    }

    private static boolean valid(double d) {
        return d != Double.MIN_VALUE;
    }
}
