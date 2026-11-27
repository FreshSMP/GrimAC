package ac.grim.grimac.utils.chunks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.SingletonPalette;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.util.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A shared section is copied the first time a player's world writes to it. The copy must hold what the shared one
 * held: one block placed into a shared all-air section is one block, not a section full of it.
 */
class ChunkSectionCacheDetachTest {

    private static ClientVersion version;
    private static int obsidian;
    private static int stone;

    @BeforeAll
    static void packetEvents() {
        PacketEventsAPI<?> api = PacketEvents.getAPI();

        if (api == null) {
            api = Mockito.mock(PacketEventsAPI.class);
            final ServerManager server = Mockito.mock(ServerManager.class);
            Mockito.when(server.getVersion()).thenReturn(ServerVersion.getLatest());
            Mockito.when(api.getServerManager()).thenReturn(server);
            Mockito.when(api.getSettings()).thenReturn(new PacketEventsSettings());
            Mockito.when(api.getLogger()).thenReturn(java.util.logging.Logger.getLogger("packetevents-test"));
            PacketEvents.setAPI(api);
        }

        // the block states say so when they load, through this
        Mockito.when(api.getLogManager()).thenReturn(Mockito.mock(LogManager.class));

        version = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
        obsidian = WrappedBlockState.getDefaultState(version, StateTypes.OBSIDIAN).getGlobalId();
        stone = WrappedBlockState.getDefaultState(version, StateTypes.STONE).getGlobalId();
    }

    @Test
    void oneBlockIntoSharedAirIsOneBlock() {
        final Column column = shared(airFromTheWire());
        final BaseChunk shared = column.chunks()[0];

        final BaseChunk own = ChunkSectionCache.detachSectionForWrite(column, 0, shared);
        own.set(3, 4, 5, obsidian);

        assertNotSame(shared, own);
        assertEquals(0L, column.sectionKeys()[0]);
        assertEquals(1, count(own, obsidian), "the placed block and nothing else");
        assertEquals(4095, count(own, 0));
        assertEquals(obsidian, own.getBlockId(3, 4, 5));
        assertFalse(own.isEmpty());
        assertEquals(4096, count(shared, 0), "the shared section is never written");

        own.set(3, 4, 5, 0);

        assertEquals(4096, count(own, 0));
        assertTrue(own.isEmpty(), "taking the block away again leaves an empty section");
    }

    @Test
    void aCopyCountsItsBlocksAsItsSourceDid() {
        final Chunk_v1_18 source = airFromTheWire();
        source.set(0, 0, 0, stone);
        source.set(1, 0, 0, stone);
        source.set(9, 9, 9, obsidian);
        final Column column = shared(source);

        final Chunk_v1_18 own = (Chunk_v1_18) ChunkSectionCache.detachSectionForWrite(column, 0, column.chunks()[0]);

        assertEquals(3, source.getBlockCount());
        assertEquals(3, own.getBlockCount());
        assertEquals(2, count(own, stone));
        assertEquals(1, count(own, obsidian));
        assertEquals(4093, count(own, 0));
    }

    private static Chunk_v1_18 airFromTheWire() {
        return new Chunk_v1_18(version, 0, 0, new DataPalette(new SingletonPalette(0), null, PaletteType.CHUNK), null);
    }

    private static Column shared(BaseChunk section) {
        final ChunkSectionCache.SharedRef ref = ChunkSectionCache.internRef(section);
        return new Column(0, 0, new BaseChunk[]{ref.section()}, new long[]{ref.key()}, 0);
    }

    private static int count(BaseChunk section, int id) {
        int count = 0;

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (section.getBlockId(x, y, z) == id) count++;
                }
            }
        }

        return count;
    }
}
