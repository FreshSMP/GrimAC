package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityGuardian;
import ac.grim.grimac.utils.enums.Pose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Players are answered before the rest of the width and height chains. This holds the two ways to the same
 * numbers for every pose a player can be in or be moving to, and checks that other entity types, those whose
 * branches come before the player's among them, do not notice the shortcut at all. The baby scale is applied after
 * either way, and no client version is asked on the way to the player branch, so neither is varied here.
 */
class BoundingBoxSizePlayersFirstTest {

    @BeforeAll
    static void packetEvents() {
        // the entity types load their ids through the packet library, which a server would have set up
        if (PacketEvents.getAPI() == null) {
            final PacketEventsAPI<?> api = Mockito.mock(PacketEventsAPI.class);
            final ServerManager server = Mockito.mock(ServerManager.class);
            Mockito.when(server.getVersion()).thenReturn(ServerVersion.getLatest());
            Mockito.when(api.getServerManager()).thenReturn(server);
            Mockito.when(api.getSettings()).thenReturn(new PacketEventsSettings());
            Mockito.when(api.getLogger()).thenReturn(java.util.logging.Logger.getLogger("packetevents-test"));
            PacketEvents.setAPI(api);
        }
    }

    @AfterEach
    void restore() {
        BoundingBoxSize.playersFirst = true;
    }

    @Test
    void shortcutIsOn() {
        // if a player type ever inherits a branch that comes first, the shortcut turns itself off; this says so
        assertTrue(BoundingBoxSize.playersFirst, "a player type now matches a branch ahead of its own");
    }

    @Test
    void playersGetTheChainsAnswer() {
        int compared = 0;
        for (EntityType type : new EntityType[]{EntityTypes.PLAYER, EntityTypes.MANNEQUIN}) {
            final PacketEntity entity = new PacketEntity(null, type);
            for (Pose current : Pose.values()) {
                final List<Pose> transitions = new ArrayList<>(List.of(Pose.values()));
                transitions.add(null);
                for (Pose transitional : transitions) {
                    entity.currentPose = current;
                    entity.transitionalPose = transitional;
                    compared += this.same(entity, type + " " + current + " > " + transitional);
                }
            }
        }
        assertEquals(2 * Pose.values().length * (Pose.values().length + 1), compared);
    }

    @Test
    void otherTypesAreUntouched() {
        final EntityType[] types = {
            EntityTypes.COW, EntityTypes.ZOMBIE, EntityTypes.ENDERMAN, EntityTypes.OAK_BOAT, EntityTypes.MINECART,
            EntityTypes.HOPPER_MINECART, EntityTypes.ITEM, EntityTypes.ARMOR_STAND, EntityTypes.VILLAGER,
            EntityTypes.SLIME, EntityTypes.PHANTOM, EntityTypes.ENDER_DRAGON, EntityTypes.HAPPY_GHAST
        };
        for (EntityType type : types) {
            this.same(new PacketEntity(null, type), String.valueOf(type));
        }
        // a guardian is recognised by its class, ahead of the player branch, whatever type it carries
        for (EntityType type : new EntityType[]{EntityTypes.GUARDIAN, EntityTypes.ELDER_GUARDIAN, EntityTypes.PLAYER}) {
            final PacketEntityGuardian guardian = Mockito.mock(PacketEntityGuardian.class);
            Mockito.when(guardian.getType()).thenReturn(type);
            this.same(guardian, String.valueOf(type));
        }
    }

    private int same(PacketEntity entity, String what) {
        BoundingBoxSize.playersFirst = false;
        final String chain = sizes(null, entity);
        BoundingBoxSize.playersFirst = true;
        assertEquals(chain, sizes(null, entity), what);
        return 1;
    }

    // a branch that asks the player its client version has no player to ask here; both ways must then fail alike
    private static String sizes(GrimPlayer player, PacketEntity entity) {
        String width, height;
        try {
            width = Integer.toHexString(Float.floatToIntBits(BoundingBoxSize.getWidth(player, entity)));
        } catch (NullPointerException needsPlayer) {
            width = "needs a player";
        }
        try {
            height = Integer.toHexString(Float.floatToIntBits(BoundingBoxSize.getHeight(player, entity)));
        } catch (NullPointerException needsPlayer) {
            height = "needs a player";
        }
        return width + " x " + height;
    }
}
