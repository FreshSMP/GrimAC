/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2022 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * This class was taken from MCProtocolLib.
 *
 * https://github.com/Steveice10/MCProtocolLib
 */

package com.github.retrooper.packetevents.protocol.world.chunk.palette;

/**
 * A global palette that maps 1:1.
 *
 * <p>Grim's copy, packed into the Bukkit jar in place of the one in the PacketEvents build it is compiled against:
 * 26.3 has 35723 block states, so a state id no longer fits the 15 bits that build stores in a global palette.
 * When a chunk section gained its 257th different block, the palette went global and every id above 32767 threw
 * "Illegal value: N &gt; 32767" on the netty thread, kicking every player sent that block update. This is
 * upstream PacketEvents' fix (36cee94, GrimAnticheat/packetevents#23, now in #24); remove this class once the
 * PacketEvents build in libs.versions.toml carries it.
 */
//TODO Equals & hashcode
public class GlobalPalette implements Palette {

    // this is the amount of bits required to store the biggest state id number (35723 states on 26.3)
    public static final int BITS_PER_ENTRY = 16;

    public static final GlobalPalette INSTANCE = new GlobalPalette();

    @Override
    public int size() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int stateToId(int state) {
        return state;
    }

    @Override
    public int idToState(int id) {
        return id;
    }

    @Override
    public int getBits() {
        return BITS_PER_ENTRY;
    }
}
