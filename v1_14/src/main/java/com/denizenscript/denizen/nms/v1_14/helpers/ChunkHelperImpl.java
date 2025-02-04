package com.denizenscript.denizen.nms.v1_14.helpers;

import com.denizenscript.denizen.nms.interfaces.ChunkHelper;
import com.denizenscript.denizen.nms.util.ReflectionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v1_14_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;

public class ChunkHelperImpl implements ChunkHelper {

    public final static Field chunkProviderServerThreadField;
    public final static MethodHandle chunkProviderServerThreadFieldSetter;
    public final static Field worldThreadField;
    public final static MethodHandle worldThreadFieldSetter;

    static {
        chunkProviderServerThreadField = ReflectionHelper.getFields(ChunkProviderServer.class).get("serverThread");
        chunkProviderServerThreadFieldSetter = ReflectionHelper.getFinalSetter(ChunkProviderServer.class, "serverThread");
        worldThreadField = ReflectionHelper.getFields(net.minecraft.server.v1_14_R1.World.class).get("serverThread");
        worldThreadFieldSetter = ReflectionHelper.getFinalSetter(net.minecraft.server.v1_14_R1.World.class, "serverThread");
    }

    public Thread resetServerThread;

    @Override
    public void changeChunkServerThread(World world) {
        if (resetServerThread != null) {
            return;
        }
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        ChunkProviderServer provider = nmsWorld.getChunkProvider();
        try {
            resetServerThread = (Thread) chunkProviderServerThreadField.get(provider);
            chunkProviderServerThreadFieldSetter.invoke(provider, Thread.currentThread());
            worldThreadFieldSetter.invoke(nmsWorld, Thread.currentThread());
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }

    @Override
    public void restoreServerThread(World world) {
        if (resetServerThread == null) {
            return;
        }
        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
        ChunkProviderServer provider = nmsWorld.getChunkProvider();
        try {
            chunkProviderServerThreadFieldSetter.invoke(provider, resetServerThread);
            worldThreadFieldSetter.invoke(nmsWorld, resetServerThread);
            resetServerThread = null;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }

    @Override
    public void refreshChunkSections(Chunk chunk) {
        PacketPlayOutMapChunk lowPacket = new PacketPlayOutMapChunk(((CraftChunk) chunk).getHandle(), 255); // 00000000 11111111
        PacketPlayOutMapChunk highPacket = new PacketPlayOutMapChunk(((CraftChunk) chunk).getHandle(), 65280); // 11111111 00000000
        ChunkCoordIntPair pos = new ChunkCoordIntPair(chunk.getX(), chunk.getZ());
        PlayerChunk playerChunk = ((CraftWorld) chunk.getWorld()).getHandle().getChunkProvider()
                .playerChunkMap.visibleChunks.get(pos.pair());
        if (playerChunk == null) {
            return;
        }
        playerChunk.players.a(pos, false).forEach(player -> {
            player.playerConnection.sendPacket(lowPacket);
            player.playerConnection.sendPacket(highPacket);
        });
    }

    @Override
    public int[] getHeightMap(Chunk chunk) {
        // TODO: 1.14 - is this a valid alternative to the removed LIGHT_BLOCKING?
        long[] lightBlocking = ((CraftChunk) chunk).getHandle().heightMap.get(HeightMap.Type.MOTION_BLOCKING).a();
        int[] heightmap = new int[lightBlocking.length];
        for (int i = 0; i < lightBlocking.length; i++) {
            heightmap[i] = (int) lightBlocking[i];
        }
        return heightmap;
    }
}
