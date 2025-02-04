package com.denizenscript.denizen.objects;

import com.denizenscript.denizen.objects.notable.NotableManager;
import com.denizenscript.denizen.objects.properties.material.MaterialSwitchFace;
import com.denizenscript.denizen.objects.properties.material.MaterialLeaves;
import com.denizenscript.denizen.scripts.commands.world.SwitchCommand;
import com.denizenscript.denizen.utilities.MaterialCompat;
import com.denizenscript.denizen.utilities.PathFinder;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizen.utilities.blocks.DirectionalBlocksHelper;
import com.denizenscript.denizen.utilities.blocks.OldMaterialsHelper;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.utilities.entity.DenizenEntityType;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizen.Settings;
import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.NMSVersion;
import com.denizenscript.denizen.nms.interfaces.BlockData;
import com.denizenscript.denizen.nms.interfaces.EntityHelper;
import com.denizenscript.denizen.nms.util.PlayerProfile;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.notable.Notable;
import com.denizenscript.denizencore.objects.notable.Note;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.core.EscapeTagBase;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.Comparator;

public class LocationTag extends org.bukkit.Location implements ObjectTag, Notable, Adjustable {

    // <--[language]
    // @name LocationTag
    // @group Object System
    // @description
    // A LocationTag represents a point in the world.
    //
    // For format info, see <@link language l@>
    //
    // -->

    // <--[language]
    // @name l@
    // @group Object Fetcher System
    // @description
    // l@ refers to the 'object identifier' of a LocationTag. The 'l@' is notation for Denizen's Object
    // Fetcher. Note that 'l' is a lowercase 'L', the first letter in 'location'.
    // The full constructor for a LocationTag is: 'l@<x>,<y>,<z>,<pitch>,<yaw>,<world>'
    // Note that you can leave off the world, and/or pitch and yaw, and/or the z value.
    // You cannot leave off both the z and the pitch+yaw at the same time.
    // For example, 'l@1,2.15,3,45,90,space' or 'l@7.5,99,3.2'
    //
    // For general info, see <@link language LocationTag>
    //
    // -->

    /**
     * The world name if a world reference is bad.
     */
    public String backupWorld;

    public String getWorldName() {
        if (getWorld() != null) {
            return getWorld().getName();
        }
        return backupWorld;
    }

    @Override
    public LocationTag clone() {
        return (LocationTag) super.clone();
    }

    /////////////////////
    //   STATIC METHODS
    /////////////////

    public void makeUnique(String id) {
        NotableManager.saveAs(this, id);
    }

    @Note("Locations")
    public String getSaveObject() {
        return (getX())
                + "," + getY()
                + "," + (getZ())
                + "," + getPitch()
                + "," + getYaw()
                + "," + getWorldName();
    }

    public static String getSaved(LocationTag location) {
        for (LocationTag saved : NotableManager.getAllType(LocationTag.class)) {
            if (saved.getX() != location.getX()) {
                continue;
            }
            if (saved.getY() != location.getY()) {
                continue;
            }
            if (saved.getZ() != location.getZ()) {
                continue;
            }
            if (saved.getYaw() != location.getYaw()) {
                continue;
            }
            if (saved.getPitch() != location.getPitch()) {
                continue;
            }
            if ((saved.getWorldName() == null && location.getWorldName() == null)
                    || (saved.getWorldName() != null && location.getWorldName() != null && saved.getWorldName().equals(location.getWorldName()))) {
                return NotableManager.getSavedId(saved);
            }
        }
        return null;
    }

    public void forget() {
        NotableManager.remove(this);
    }


    //////////////////
    //    OBJECT FETCHER
    ////////////////


    public static LocationTag valueOf(String string) {
        return valueOf(string, null);
    }

    /**
     * Gets a Location Object from a string form of id,x,y,z,world
     * or a dScript argument (location:)x,y,z,world. If including an Id,
     * this location will persist and can be recalled at any time.
     *
     * @param string the string or dScript argument String
     * @return a Location, or null if incorrectly formatted
     */
    @Fetchable("l")
    public static LocationTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }

        if (string.startsWith("l@")) {
            string = string.substring(2);
        }

        if (NotableManager.isSaved(string) && NotableManager.isType(string, LocationTag.class)) {
            return (LocationTag) NotableManager.getSavedObject(string);
        }

        ////////
        // Match location formats

        // Split values
        List<String> split = CoreUtilities.split(string, ',');

        if (split.size() == 2)
        // If 4 values, world-less 2D location format
        // x,y
        {
            try {
                return new LocationTag(null,
                        Double.valueOf(split.get(0)),
                        Double.valueOf(split.get(1)));
            }
            catch (Exception e) {
                if (context == null || context.debug) {
                    Debug.log("Minor: valueOf LocationTag returning null: " + string + "(internal exception:" + e.getMessage() + ")");
                }
                return null;
            }
        }
        else if (split.size() == 3)
        // If 3 values, either worldless location format
        // x,y,z or 2D location format x,y,world
        {
            try {
                World world = Bukkit.getWorld(split.get(2));
                if (world != null) {
                    return new LocationTag(world,
                            Double.valueOf(split.get(0)),
                            Double.valueOf(split.get(1)));
                }
                if (ArgumentHelper.matchesDouble(split.get(2))) {
                    return new LocationTag(null,
                            Double.valueOf(split.get(0)),
                            Double.valueOf(split.get(1)),
                            Double.valueOf(split.get(2)));
                }
                LocationTag output = new LocationTag(null,
                        Double.valueOf(split.get(0)),
                        Double.valueOf(split.get(1)));
                output.backupWorld = split.get(2);
                return output;
            }
            catch (Exception e) {
                if (context == null || context.debug) {
                    Debug.log("Minor: valueOf LocationTag returning null: " + string + "(internal exception:" + e.getMessage() + ")");
                }
                return null;
            }
        }
        else if (split.size() == 4)
        // If 4 values, standard dScript location format
        // x,y,z,world
        {
            try {
                World world = Bukkit.getWorld(split.get(3));
                if (world != null) {
                    return new LocationTag(world,
                            Double.valueOf(split.get(0)),
                            Double.valueOf(split.get(1)),
                            Double.valueOf(split.get(2)));
                }
                LocationTag output = new LocationTag(null,
                        Double.valueOf(split.get(0)),
                        Double.valueOf(split.get(1)),
                        Double.valueOf(split.get(2)));
                output.backupWorld = split.get(3);
                return output;
            }
            catch (Exception e) {
                if (context == null || context.debug) {
                    Debug.log("Minor: valueOf LocationTag returning null: " + string + "(internal exception:" + e.getMessage() + ")");
                }
                return null;
            }
        }
        else if (split.size() == 5)

        // If 5 values, location with pitch/yaw (no world)
        // x,y,z,pitch,yaw
        {
            try {
                LocationTag output = new LocationTag(null,
                        Double.valueOf(split.get(0)),
                        Double.valueOf(split.get(1)),
                        Double.valueOf(split.get(2)),
                        Float.valueOf(split.get(3)),
                        Float.valueOf(split.get(4)));
                return output;
            }
            catch (Exception e) {
                if (context == null || context.debug) {
                    Debug.log("Minor: valueOf LocationTag returning null: " + string + "(internal exception:" + e.getMessage() + ")");
                }
                return null;
            }
        }
        else if (split.size() == 6)

        // If 6 values, location with pitch/yaw
        // x,y,z,pitch,yaw,world
        {
            try {
                World world = Bukkit.getWorld(split.get(5));
                if (world != null) {
                    return new LocationTag(world,
                            Double.valueOf(split.get(0)),
                            Double.valueOf(split.get(1)),
                            Double.valueOf(split.get(2)),
                            Float.valueOf(split.get(3)),
                            Float.valueOf(split.get(4)));
                }
                LocationTag output = new LocationTag(null,
                        Double.valueOf(split.get(0)),
                        Double.valueOf(split.get(1)),
                        Double.valueOf(split.get(2)),
                        Float.valueOf(split.get(3)),
                        Float.valueOf(split.get(4)));
                output.backupWorld = split.get(5);
                return output;
            }
            catch (Exception e) {
                if (context == null || context.debug) {
                    Debug.log("Minor: valueOf LocationTag returning null: " + string + "(internal exception:" + e.getMessage() + ")");
                }
                return null;
            }
        }

        if (context == null || context.debug) {
            Debug.log("Minor: valueOf LocationTag returning null: " + string);
        }

        return null;
    }

    public static boolean matches(String string) {
        if (string == null || string.length() == 0) {
            return false;
        }

        if (string.startsWith("l@")) {
            return true;
        }

        return LocationTag.valueOf(string, new BukkitTagContext(null, null, false, null, false, null)) != null;
    }


    /////////////////////
    //   CONSTRUCTORS
    //////////////////

    private boolean is2D = false;

    /**
     * Turns a Bukkit Location into a LocationTag, which has some helpful methods
     * for working with dScript.
     *
     * @param location the Bukkit Location to reference
     */
    public LocationTag(Location location) {
        // Just save the yaw and pitch as they are; don't check if they are
        // higher than 0, because Minecraft yaws are weird and can have
        // negative values
        super(location.getWorld(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public LocationTag(Vector vector) {
        super(null, vector.getX(), vector.getY(), vector.getZ());
    }

    public LocationTag(World world, double x, double y) {
        this(world, x, y, 0);
        this.is2D = true;
    }

    /**
     * Turns a world and coordinates into a Location, which has some helpful methods
     * for working with dScript.
     *
     * @param world the world in which the location resides
     * @param x     x-coordinate of the location
     * @param y     y-coordinate of the location
     * @param z     z-coordinate of the location
     */
    public LocationTag(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    public LocationTag(World world, double x, double y, double z, float pitch, float yaw) {
        super(world, x, y, z, yaw, pitch);
    }

    public boolean isChunkLoaded() {
        return getWorld() != null && getWorld().isChunkLoaded(getBlockX() >> 4, getBlockZ() >> 4);
    }

    public boolean isChunkLoadedSafe() {
        try {
            NMSHandler.getChunkHelper().changeChunkServerThread(getWorld());
            return isChunkLoaded();
        }
        finally {
            NMSHandler.getChunkHelper().restoreServerThread(getWorld());
        }
    }

    @Override
    public Block getBlock() {
        if (getWorld() == null) {
            Debug.echoError("LocationTag trying to read block, but cannot because no world is specified.");
            return null;
        }
        return super.getBlock();
    }

    public Block getBlockForTag(Attribute attribute) {
        NMSHandler.getChunkHelper().changeChunkServerThread(getWorld());
        try {
            if (getWorld() == null) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("LocationTag trying to read block, but cannot because no world is specified.");
                }
                return null;
            }
            if (!isChunkLoaded()) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("LocationTag trying to read block, but cannot because the chunk is unloaded. Use the 'chunkload' command to ensure the chunk is loaded.");
                }
                return null;
            }
            return super.getBlock();
        }
        finally {
            NMSHandler.getChunkHelper().restoreServerThread(getWorld());
        }
    }

    public Material getBlockTypeForTag(Attribute attribute) {
        NMSHandler.getChunkHelper().changeChunkServerThread(getWorld());
        try {
            if (getWorld() == null) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("LocationTag trying to read block, but cannot because no world is specified.");
                }
                return null;
            }
            if (!isChunkLoaded()) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("LocationTag trying to read block, but cannot because the chunk is unloaded. Use the 'chunkload' command to ensure the chunk is loaded.");
                }
                return null;
            }
            return super.getBlock().getType();
        }
        finally {
            NMSHandler.getChunkHelper().restoreServerThread(getWorld());
        }
    }

    public static BlockState getBlockStateFor(Block block) {
        return block.getState();
    }

    public static BlockState getBlockStateSafe(Block block) {
        NMSHandler.getChunkHelper().changeChunkServerThread(block.getWorld());
        try {
            return block.getState();
        }
        finally {
            NMSHandler.getChunkHelper().restoreServerThread(block.getWorld());
        }
    }

    public Biome getBiomeForTag(Attribute attribute) {
        NMSHandler.getChunkHelper().changeChunkServerThread(getWorld());
        try {
            if (getWorld() == null) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("LocationTag trying to read block, but cannot because no world is specified.");
                }
                return null;
            }
            if (!isChunkLoaded()) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("LocationTag trying to read block, but cannot because the chunk is unloaded. Use the 'chunkload' command to ensure the chunk is loaded.");
                }
                return null;
            }
            return super.getBlock().getBiome();
        }
        finally {
            NMSHandler.getChunkHelper().restoreServerThread(getWorld());
        }
    }

    public Location getHighestBlockForTag(Attribute attribute) {
        NMSHandler.getChunkHelper().changeChunkServerThread(getWorld());
        try {
            if (getWorld() == null) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("LocationTag trying to read block, but cannot because no world is specified.");
                }
                return null;
            }
            if (!isChunkLoaded()) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("LocationTag trying to read block, but cannot because the chunk is unloaded. Use the 'chunkload' command to ensure the chunk is loaded.");
                }
                return null;
            }
            return getWorld().getHighestBlockAt(this).getLocation();
        }
        finally {
            NMSHandler.getChunkHelper().restoreServerThread(getWorld());
        }
    }

    public Collection<ItemStack> getDropsForTag(Attribute attribute, ItemStack item) {
        NMSHandler.getChunkHelper().changeChunkServerThread(getWorld());
        try {
            if (getWorld() == null) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("LocationTag trying to read block, but cannot because no world is specified.");
                }
                return null;
            }
            if (!isChunkLoaded()) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("LocationTag trying to read block, but cannot because the chunk is unloaded. Use the 'chunkload' command to ensure the chunk is loaded.");
                }
                return null;
            }
            return item == null ? super.getBlock().getDrops() : super.getBlock().getDrops(item);
        }
        finally {
            NMSHandler.getChunkHelper().restoreServerThread(getWorld());
        }
    }

    public BlockState getBlockState() {
        return getBlockStateFor(getBlock());
    }

    public BlockState getBlockStateForTag(Attribute attribute) {
        Block block = getBlockForTag(attribute);
        if (block == null) {
            return null;
        }
        return getBlockStateSafe(block);
    }

    public LocationTag getBlockLocation() {
        return new LocationTag(getWorld(), getBlockX(), getBlockY(), getBlockZ());
    }

    /**
     * Indicates whether this location is forced to identify as a notable or not.
     */
    private boolean raw = false;

    private void setRaw(boolean state) {
        this.raw = state;
    }

    @Override
    public void setPitch(float pitch) {
        super.setPitch(pitch);
    }

    // TODO: Why does this and the above exist?
    @Override
    public void setYaw(float yaw) {
        super.setYaw(yaw);
    }

    public boolean hasInventory() {
        return getBlockState() instanceof InventoryHolder;
    }

    public Inventory getBukkitInventory() {
        return hasInventory() ? ((InventoryHolder) getBlockState()).getInventory() : null;
    }

    public InventoryTag getInventory() {
        return hasInventory() ? InventoryTag.mirrorBukkitInventory(getBukkitInventory()) : null;
    }

    public BlockFace getSkullBlockFace(int rotation) {
        switch (rotation) {
            case 0:
                return BlockFace.NORTH;
            case 1:
                return BlockFace.NORTH_NORTH_EAST;
            case 2:
                return BlockFace.NORTH_EAST;
            case 3:
                return BlockFace.EAST_NORTH_EAST;
            case 4:
                return BlockFace.EAST;
            case 5:
                return BlockFace.EAST_SOUTH_EAST;
            case 6:
                return BlockFace.SOUTH_EAST;
            case 7:
                return BlockFace.SOUTH_SOUTH_EAST;
            case 8:
                return BlockFace.SOUTH;
            case 9:
                return BlockFace.SOUTH_SOUTH_WEST;
            case 10:
                return BlockFace.SOUTH_WEST;
            case 11:
                return BlockFace.WEST_SOUTH_WEST;
            case 12:
                return BlockFace.WEST;
            case 13:
                return BlockFace.WEST_NORTH_WEST;
            case 14:
                return BlockFace.NORTH_WEST;
            case 15:
                return BlockFace.NORTH_NORTH_WEST;
            default:
                return null;
        }
    }

    public byte getSkullRotation(BlockFace face) {
        switch (face) {
            case NORTH:
                return 0;
            case NORTH_NORTH_EAST:
                return 1;
            case NORTH_EAST:
                return 2;
            case EAST_NORTH_EAST:
                return 3;
            case EAST:
                return 4;
            case EAST_SOUTH_EAST:
                return 5;
            case SOUTH_EAST:
                return 6;
            case SOUTH_SOUTH_EAST:
                return 7;
            case SOUTH:
                return 8;
            case SOUTH_SOUTH_WEST:
                return 9;
            case SOUTH_WEST:
                return 10;
            case WEST_SOUTH_WEST:
                return 11;
            case WEST:
                return 12;
            case WEST_NORTH_WEST:
                return 13;
            case NORTH_WEST:
                return 14;
            case NORTH_NORTH_WEST:
                return 15;
        }
        return -1;
    }

    public int compare(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || loc1.equals(loc2)) {
            return 0;
        }
        else {
            double dist = distanceSquared(loc1) - distanceSquared(loc2);
            return dist == 0 ? 0 : (dist > 0 ? 1 : -1);
        }
    }

    @Override
    public int hashCode() {
        return (int) (Math.floor(getX()) + Math.floor(getY()) + Math.floor(getZ()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof LocationTag)) {
            return false;
        }
        LocationTag other = (LocationTag) o;
        if ((other.getWorldName() == null && getWorldName() != null)
                || (getWorldName() == null && other.getWorldName() != null)
                || (getWorldName() != null && other.getWorldName() != null
                && !getWorldName().equalsIgnoreCase(other.getWorldName()))) {
            return false;
        }
        return Math.floor(getX()) == Math.floor(other.getX())
                && Math.floor(getY()) == Math.floor(other.getY())
                && Math.floor(getZ()) == Math.floor(other.getZ());
    }

    String prefix = "Location";

    @Override
    public String getObjectType() {
        return "Location";
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public LocationTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debuggable() {
        if (isUnique()) {
            return "<Y>" + getSaved(this) + "<GR> (" + identifyRaw().replace(",", "<G>,<GR> ") + "<GR>)";
        }
        else {
            return "<Y>" + identifyRaw().replace(",", "<G>,<Y> ");
        }
    }

    @Override
    public boolean isUnique() {
        return getSaved(this) != null;
    }

    @Override
    public String identify() {
        if (!raw && isUnique()) {
            return "l@" + getSaved(this);
        }
        else {
            return identifyRaw();
        }
    }

    @Override
    public String identifySimple() {
        if (isUnique()) {
            return "l@" + getSaved(this);
        }
        else if (getWorldName() == null) {
            return "l@" + getBlockX() + "," + getBlockY() + (!is2D ? "," + getBlockZ() : "");
        }
        else {
            return "l@" + getBlockX() + "," + getBlockY() + (!is2D ? "," + getBlockZ() : "")
                    + "," + getWorldName();
        }
    }

    public String identifyRaw() {
        if (getYaw() != 0.0 || getPitch() != 0.0) {
            return "l@" + CoreUtilities.doubleToString(getX()) + "," + CoreUtilities.doubleToString(getY())
                    + "," + CoreUtilities.doubleToString(getZ()) + "," + CoreUtilities.doubleToString(getPitch())
                    + "," + CoreUtilities.doubleToString(getYaw())
                    + (getWorldName() != null ? "," + getWorldName() : "");
        }
        else {
            return "l@" + CoreUtilities.doubleToString(getX()) + "," + CoreUtilities.doubleToString(getY())
                    + (!is2D ? "," + CoreUtilities.doubleToString(getZ()) : "")
                    + (getWorldName() != null ? "," + getWorldName() : "");
        }
    }

    @Override
    public String toString() {
        return identify();
    }

    public static void registerTags() {

        /////////////////////
        //   BLOCK ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <LocationTag.block_facing>
        // @returns LocationTag
        // @description
        // Returns the relative location vector of where this block is facing.
        // Only works for block types that have directionality (such as signs, chests, stairs, etc.).
        // This can return for example "1,0,0" to mean the block is facing towards the positive X axis.
        // You can use <some_block_location.add[<some_block_location.block_facing>]> to get the block directly in front of this block (based on its facing direction).
        // -->
        registerTag("block_facing", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Vector facing = DirectionalBlocksHelper.getFacing(((LocationTag) object).getBlockForTag(attribute));
                if (facing != null) {
                    return new LocationTag(((LocationTag) object).getWorld(), facing.getX(), facing.getY(), facing.getZ())
                            .getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.above[(<#.#>)]>
        // @returns LocationTag
        // @description
        // Returns the location above this location. Optionally specify a number of blocks to go up.
        // This just moves straight along the Y axis, equivalent to <@link tag LocationTag.add> with input 0,1,0 (or the input value instead of '1').
        // -->
        registerTag("above", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new LocationTag(((LocationTag) object).clone().add(0, attribute.hasContext(1) ? attribute.getDoubleContext(1) : 1, 0))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.below[(<#.#>)]>
        // @returns LocationTag
        // @description
        // Returns the location below this location. Optionally specify a number of blocks to go down.
        // This just moves straight along the Y axis, equivalent to <@link tag LocationTag.sub> with input 0,1,0 (or the input value instead of '1').
        // -->
        registerTag("below", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new LocationTag(((LocationTag) object).clone().subtract(0, attribute.hasContext(1) ? attribute.getDoubleContext(1) : 1, 0))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.forward_flat[(<#.#>)]>
        // @returns LocationTag
        // @description
        // Returns the location in front of this location based on yaw but not pitch. Optionally specify a number of blocks to go forward.
        // -->
        registerTag("forward_flat", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Location loc = ((LocationTag) object).clone();
                loc.setPitch(0);
                Vector vector = loc.getDirection().multiply(attribute.hasContext(1) ? attribute.getDoubleContext(1) : 1);
                return new LocationTag(((LocationTag) object).clone().add(vector))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.backward_flat[(<#.#>)]>
        // @returns LocationTag
        // @description
        // Returns the location behind this location based on yaw but not pitch. Optionally specify a number of blocks to go backward.
        // This is equivalent to <@link tag LocationTag.forward_flat> in the opposite direction.
        // -->
        registerTag("backward_flat", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Location loc = ((LocationTag) object).clone();
                loc.setPitch(0);
                Vector vector = loc.getDirection().multiply(attribute.hasContext(1) ? attribute.getDoubleContext(1) : 1);
                return new LocationTag(((LocationTag) object).clone().subtract(vector))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.forward[(<#.#>)]>
        // @returns LocationTag
        // @description
        // Returns the location in front of this location based on pitch and yaw. Optionally specify a number of blocks to go forward.
        // -->
        registerTag("forward", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Vector vector = ((LocationTag) object).getDirection().multiply(attribute.hasContext(1) ? attribute.getDoubleContext(1) : 1);
                return new LocationTag(((LocationTag) object).clone().add(vector))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.backward[(<#.#>)]>
        // @returns LocationTag
        // @description
        // Returns the location behind this location based on pitch and yaw. Optionally specify a number of blocks to go backward.
        // This is equivalent to <@link tag LocationTag.forward> in the opposite direction.
        // -->
        registerTag("backward", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Vector vector = ((LocationTag) object).getDirection().multiply(attribute.hasContext(1) ? attribute.getDoubleContext(1) : 1);
                return new LocationTag(((LocationTag) object).clone().subtract(vector))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.left[(<#.#>)]>
        // @returns LocationTag
        // @description
        // Returns the location to the left of this location based on pitch and yaw. Optionally specify a number of blocks to go left.
        // This is equivalent to <@link tag LocationTag.forward> with a +90 degree rotation to the yaw and the pitch set to 0.
        // -->
        registerTag("left", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Location loc = ((LocationTag) object).clone();
                loc.setPitch(0);
                Vector vector = loc.getDirection().rotateAroundY(Math.PI / 2).multiply(attribute.hasContext(1) ? attribute.getDoubleContext(1) : 1);
                return new LocationTag(((LocationTag) object).clone().add(vector))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.right[(<#.#>)]>
        // @returns LocationTag
        // @description
        // Returns the location to the right of this location based on pitch and yaw. Optionally specify a number of blocks to go right.
        // This is equivalent to <@link tag LocationTag.forward> with a -90 degree rotation to the yaw and the pitch set to 0.
        // -->
        registerTag("right", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Location loc = ((LocationTag) object).clone();
                loc.setPitch(0);
                Vector vector = loc.getDirection().rotateAroundY(Math.PI / 2).multiply(attribute.hasContext(1) ? attribute.getDoubleContext(1) : 1);
                return new LocationTag(((LocationTag) object).clone().subtract(vector))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.up[(<#.#>)]>
        // @returns LocationTag
        // @description
        // Returns the location above this location based on pitch and yaw. Optionally specify a number of blocks to go up.
        // This is equivalent to <@link tag LocationTag.forward> with a +90 degree rotation to the pitch.
        // -->
        registerTag("up", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Location loc = ((LocationTag) object).clone();
                loc.setPitch(loc.getPitch() - 90);
                Vector vector = loc.getDirection().multiply(attribute.hasContext(1) ? attribute.getDoubleContext(1) : 1);
                return new LocationTag(((LocationTag) object).clone().add(vector))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.down[(<#.#>)]>
        // @returns LocationTag
        // @description
        // Returns the location below this location based on pitch and yaw. Optionally specify a number of blocks to go down.
        // This is equivalent to <@link tag LocationTag.forward> with a -90 degree rotation to the pitch.
        // -->
        registerTag("down", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Location loc = ((LocationTag) object).clone();
                loc.setPitch(loc.getPitch() - 90);
                Vector vector = loc.getDirection().multiply(attribute.hasContext(1) ? attribute.getDoubleContext(1) : 1);
                return new LocationTag(((LocationTag) object).clone().subtract(vector))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.relative[<location>]>
        // @returns LocationTag
        // @description
        // Returns the location relative to this location. Input is a vector location of the form left,up,forward.
        // For example, input -1,1,1 will return a location 1 block to the right, 1 block up, and 1 block forward.
        // -->
        registerTag("relative", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                LocationTag offsetLoc = LocationTag.valueOf(attribute.getContext(1));
                if (offsetLoc == null) {
                    return null;
                }

                Location loc = ((LocationTag) object).clone();
                Vector offset = loc.getDirection().multiply(offsetLoc.getZ());
                loc.setPitch(loc.getPitch() - 90);
                offset = offset.add(loc.getDirection().multiply(offsetLoc.getY()));
                loc.setPitch(0);
                offset = offset.add(loc.getDirection().rotateAroundY(Math.PI / 2).multiply(offsetLoc.getX()));

                return new LocationTag(((LocationTag) object).clone().add(offset))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.block>
        // @returns LocationTag
        // @description
        // Returns the location of the block this location is on,
        // i.e. returns a location without decimals or direction.
        // -->
        registerTag("block", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new LocationTag(((LocationTag) object).getWorld(), ((LocationTag) object).getBlockX(), ((LocationTag) object).getBlockY(), ((LocationTag) object).getBlockZ())
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.center>
        // @returns LocationTag
        // @description
        // Returns the location at the center of the block this location is on.
        // -->
        registerTag("center", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new LocationTag(((LocationTag) object).getWorld(), ((LocationTag) object).getBlockX() + 0.5, ((LocationTag) object).getBlockY() + 0.5, ((LocationTag) object).getBlockZ() + 0.5)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.highest>
        // @returns LocationTag
        // @description
        // Returns the location of the highest solid block at the location.
        // -->
        registerTag("highest", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new LocationTag(((LocationTag) object).getHighestBlockForTag(attribute).add(0, -1, 0))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.base_color>
        // @returns ElementTag
        // @description
        // Returns the base color of the banner at this location.
        // For the list of possible colors, see <@link url http://bit.ly/1dydq12>.
        // As of 1.13+, this tag is no longer relevant.
        // -->
        registerTag("base_color", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_13)) {
                    Debug.echoError("Base_Color tag no longer relevant: banner types are now distinct materials.");
                }
                DyeColor color = ((Banner) ((LocationTag) object).getBlockStateForTag(attribute)).getBaseColor();
                return new ElementTag(color != null ? color.name() : "BLACK").getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.has_inventory>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the block at the location has an inventory.
        // -->
        registerTag("has_inventory", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((LocationTag) object).getBlockStateForTag(attribute) instanceof InventoryHolder).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.inventory>
        // @returns InventoryTag
        // @description
        // Returns the InventoryTag of the block at the location. If the
        // block is not a container, returns null.
        // -->
        registerTag("inventory", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!((LocationTag) object).isChunkLoadedSafe()) {
                    return null;
                }
                ObjectTag obj = ElementTag.handleNull(((LocationTag) object).identify() + ".inventory", ((LocationTag) object).getInventory(), "InventoryTag", attribute.hasAlternative());
                return obj == null ? null : obj.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.material>
        // @returns MaterialTag
        // @description
        // Returns the material of the block at the location.
        // -->
        registerTag("material", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Block block = ((LocationTag) object).getBlockForTag(attribute);
                if (block == null) {
                    return null;
                }
                return new MaterialTag(block).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.patterns>
        // @returns ListTag
        // @group properties
        // @mechanism LocationTag.patterns
        // @description
        // Lists the patterns of the banner at this location in the form "li@COLOR/PATTERN|COLOR/PATTERN" etc.
        // For the list of possible colors, see <@link url http://bit.ly/1dydq12>.
        // For the list of possible patterns, see <@link url http://bit.ly/1MqRn7T>.
        // -->
        registerTag("patterns", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ListTag list = new ListTag();
                for (org.bukkit.block.banner.Pattern pattern : ((Banner) ((LocationTag) object).getBlockStateForTag(attribute)).getPatterns()) {
                    list.add(pattern.getColor().name() + "/" + pattern.getPattern().name());
                }
                return list.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.head_rotation>
        // @returns ElementTag(Number)
        // @description
        // Gets the rotation of the head at this location. Can be 1-16.
        // @mechanism LocationTag.head_rotation
        // -->
        registerTag("head_rotation", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((LocationTag) object).getSkullRotation(((Skull) ((LocationTag) object).getBlockStateForTag(attribute)).getRotation()) + 1)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.switched>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the block at the location is considered to be switched on.
        // (For buttons, levers, etc.)
        // To change this, see <@link command Switch>
        // -->
        registerTag("switched", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(SwitchCommand.switchState(((LocationTag) object).getBlockForTag(attribute)))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.sign_contents>
        // @returns ListTag
        // @mechanism LocationTag.sign_contents
        // @description
        // Returns a list of lines on a sign.
        // -->
        registerTag("sign_contents", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (((LocationTag) object).getBlockStateForTag(attribute) instanceof Sign) {
                    return new ListTag(Arrays.asList(((Sign) ((LocationTag) object).getBlockStateForTag(attribute)).getLines()))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <LocationTag.spawner_type>
        // @mechanism LocationTag.spawner_type
        // @returns EntityTag
        // @description
        // Returns the type of entity spawned by a mob spawner.
        // -->
        registerTag("spawner_type", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (((LocationTag) object).getBlockStateForTag(attribute) instanceof CreatureSpawner) {
                    return new EntityTag(DenizenEntityType.getByName(((CreatureSpawner) ((LocationTag) object).getBlockStateForTag(attribute))
                            .getSpawnedType().name())).getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <LocationTag.lock>
        // @mechanism LocationTag.lock
        // @returns ElementTag
        // @description
        // Returns the password to a locked container.
        // -->
        registerTag("lock", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!(((LocationTag) object).getBlockStateForTag(attribute) instanceof Lockable)) {
                    return null;
                }
                Lockable lock = (Lockable) ((LocationTag) object).getBlockStateForTag(attribute);
                return new ElementTag(lock.isLocked() ? lock.getLock() : null)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.is_locked>
        // @mechanism LocationTag.lock
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the container is locked.
        // -->
        registerTag("is_locked", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!(((LocationTag) object).getBlockStateForTag(attribute) instanceof Lockable)) {
                    return null;
                }
                return new ElementTag(((Lockable) ((LocationTag) object).getBlockStateForTag(attribute)).isLocked())
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.is_lockable>
        // @mechanism LocationTag.lock
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the container is lockable.
        // -->
        registerTag("is_lockable", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((LocationTag) object).getBlockStateForTag(attribute) instanceof Lockable)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.drops[(<item>)]>
        // @returns ListTag(ItemTag)
        // @description
        // Returns what items the block at the location would drop if broken naturally.
        // Optionally specifier a breaker item.
        // -->
        registerTag("drops", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                ItemStack inputItem = null;
                if (attribute.hasContext(1)) {
                    inputItem = ItemTag.valueOf(attribute.getContext(1), attribute.context).getItemStack();
                }
                ListTag list = new ListTag();
                for (ItemStack it : ((LocationTag) object).getDropsForTag(attribute, inputItem)) {
                    list.add(new ItemTag(it).identify());
                }
                return list.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.flowerpot_contents>
        // @returns ElementTag
        // @description
        // Returns the flower pot contents at the location.
        // NOTE: Replaced by materials (such as POTTED_CACTUS) in 1.13 and above.
        // -->
        registerTag("flowerpot_contents", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_13)) {
                    Debug.echoError("As of Minecraft version 1.13 potted flowers each have their own material, such as POTTED_CACTUS.");
                }
                else if (((LocationTag) object).getBlockTypeForTag(attribute) == Material.FLOWER_POT) {
                    MaterialData contents = NMSHandler.getBlockHelper().getFlowerpotContents(((LocationTag) object).getBlockForTag(attribute));
                    return OldMaterialsHelper.getMaterialFrom(contents.getItemType(), contents.getData())
                            .getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });


        // <--[tag]
        // @attribute <LocationTag.skull_type>
        // @returns ElementTag
        // @description
        // Returns the type of the skull.
        // -->
        registerTag("skull_type", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                BlockState blockState = ((LocationTag) object).getBlockStateForTag(attribute);
                if (blockState instanceof Skull) {
                    String t = ((Skull) blockState).getSkullType().name();
                    return new ElementTag(t).getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.skull_name>
        // @returns ElementTag
        // @mechanism LocationTag.skull_skin
        // @description
        // Returns the name of the skin the skull is displaying.
        // -->
        registerTag("skull_name", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                BlockState blockState = ((LocationTag) object).getBlockStateForTag(attribute);
                if (blockState instanceof Skull) {
                    PlayerProfile profile = NMSHandler.getBlockHelper().getPlayerProfile((Skull) blockState);
                    if (profile == null) {
                        return null;
                    }
                    String n = profile.getName();
                    if (n == null) {
                        n = ((Skull) blockState).getOwningPlayer().getName();
                    }
                    return new ElementTag(n).getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.skull_skin>
        // @returns ElementTag
        // @mechanism LocationTag.skull_skin
        // @description
        // Returns the skin the skull is displaying - just the name or UUID as text, not a player object.
        // -->
        registerTag("skull_skin", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                BlockState blockState = ((LocationTag) object).getBlockStateForTag(attribute);
                if (blockState instanceof Skull) {
                    PlayerProfile profile = NMSHandler.getBlockHelper().getPlayerProfile((Skull) blockState);
                    if (profile == null) {
                        return null;
                    }
                    String name = profile.getName();
                    UUID uuid = profile.getUniqueId();
                    String texture = profile.getTexture();
                    attribute = attribute.fulfill(1);
                    // <--[tag]
                    // @attribute <LocationTag.skull_skin.full>
                    // @returns ElementTag|Element
                    // @mechanism LocationTag.skull_skin
                    // @description
                    // Returns the skin the skull item is displaying - just the name or UUID as text, not a player object,
                    // along with the permanently cached texture property.
                    // -->
                    if (attribute.startsWith("full")) {
                        return new ElementTag((uuid != null ? uuid : name)
                                + (texture != null ? "|" + texture : ""))
                                .getObjectAttribute(attribute.fulfill(1));
                    }
                    return new ElementTag(uuid != null ? uuid.toString() : name).getObjectAttribute(attribute);
                }
                else {
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <LocationTag.simple>
        // @returns ElementTag
        // @description
        // Returns a simple version of the LocationTag's block coordinates.
        // In the format: x,y,z,world
        // For example: 1,2,3,world_nether
        // -->
        registerTag("simple", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                // <--[tag]
                // @attribute <LocationTag.simple.formatted>
                // @returns ElementTag
                // @description
                // Returns the formatted simple version of the LocationTag's block coordinates.
                // In the format: X 'x', Y 'y', Z 'z', in world 'world'
                // For example, X '1', Y '2', Z '3', in world 'world_nether'
                // -->
                if (attribute.getAttributeWithoutContext(2).equals("formatted")) {
                    return new ElementTag("X '" + ((LocationTag) object).getBlockX()
                            + "', Y '" + ((LocationTag) object).getBlockY()
                            + "', Z '" + ((LocationTag) object).getBlockZ()
                            + "', in world '" + ((LocationTag) object).getWorldName() + "'").getObjectAttribute(attribute.fulfill(2));
                }
                if (((LocationTag) object).getWorldName() == null) {
                    return new ElementTag(((LocationTag) object).getBlockX() + "," + ((LocationTag) object).getBlockY() + "," + ((LocationTag) object).getBlockZ())
                            .getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return new ElementTag(((LocationTag) object).getBlockX() + "," + ((LocationTag) object).getBlockY() + "," + ((LocationTag) object).getBlockZ()
                            + "," + ((LocationTag) object).getWorldName()).getObjectAttribute(attribute.fulfill(1));
                }
            }
        });


        /////////////////////
        //   DIRECTION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <LocationTag.precise_impact_normal[<range>]>
        // @returns LocationTag
        // @description
        // Returns the exact impact normal at the location this location is pointing at.
        // Optionally, specify a maximum range to find the location from (defaults to 200).
        // -->
        registerTag("precise_impact_normal", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                int range = attribute.getIntContext(1);
                if (range < 1) {
                    range = 200;
                }
                double xzLen = Math.cos((((LocationTag) object).getPitch() % 360) * (Math.PI / 180));
                double nx = xzLen * Math.sin(-((LocationTag) object).getYaw() * (Math.PI / 180));
                double ny = Math.sin(((LocationTag) object).getPitch() * (Math.PI / 180));
                double nz = xzLen * Math.cos(((LocationTag) object).getYaw() * (Math.PI / 180));
                Location location = NMSHandler.getEntityHelper().getImpactNormal((LocationTag) object, new Vector(nx, -ny, nz), range);
                if (location != null) {
                    return new LocationTag(location).getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <LocationTag.precise_cursor_on_block[<range>]>
        // @returns LocationTag
        // @description
        // Returns the block location this location is pointing at.
        // Optionally, specify a maximum range to find the location from (defaults to 200).
        // -->
        registerTag("precise_cursor_on_block", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                int range = attribute.getIntContext(1);
                if (range < 1) {
                    range = 200;
                }
                double xzLen = Math.cos((((LocationTag) object).getPitch() % 360) * (Math.PI / 180));
                double nx = xzLen * Math.sin(-((LocationTag) object).getYaw() * (Math.PI / 180));
                double ny = Math.sin(((LocationTag) object).getPitch() * (Math.PI / 180));
                double nz = xzLen * Math.cos(((LocationTag) object).getYaw() * (Math.PI / 180));
                Location location = NMSHandler.getEntityHelper().rayTraceBlock((LocationTag) object, new Vector(nx, -ny, nz), range);
                if (location != null) {
                    return new LocationTag(location).getBlockLocation().getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <LocationTag.precise_cursor_on[<range>]>
        // @returns LocationTag
        // @description
        // Returns the exact location this location is pointing at.
        // Optionally, specify a maximum range to find the location from (defaults to 200).
        // -->
        registerTag("precise_cursor_on", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                int range = attribute.getIntContext(1);
                if (range < 1) {
                    range = 200;
                }
                double xzLen = Math.cos((((LocationTag) object).getPitch() % 360) * (Math.PI / 180));
                double nx = xzLen * Math.sin(-((LocationTag) object).getYaw() * (Math.PI / 180));
                double ny = Math.sin(((LocationTag) object).getPitch() * (Math.PI / 180));
                double nz = xzLen * Math.cos(((LocationTag) object).getYaw() * (Math.PI / 180));
                Location location = NMSHandler.getEntityHelper().rayTrace((LocationTag) object, new Vector(nx, -ny, nz), range);
                if (location != null) {
                    return new LocationTag(location).getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <LocationTag.points_between[<location>]>
        // @returns ListTag(LocationTag)
        // @description
        // Finds all locations between this location and another, separated by 1 block-width each.
        // -->
        registerTag("points_between", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                LocationTag target = LocationTag.valueOf(attribute.getContext(1));
                if (target == null) {
                    return null;
                }
                attribute = attribute.fulfill(1);
                // <--[tag]
                // @attribute <LocationTag.points_between[<location>].distance[<#.#>]>
                // @returns ListTag(LocationTag)
                // @description
                // Finds all locations between this location and another, separated by the specified distance each.
                // -->
                double rad = 1d;
                if (attribute.startsWith("distance")) {
                    rad = attribute.getDoubleContext(1);
                    attribute = attribute.fulfill(1);
                }
                ListTag list = new ListTag();
                org.bukkit.util.Vector rel = target.toVector().subtract(((LocationTag) object).toVector());
                double len = rel.length();
                rel = rel.multiply(1d / len);
                for (double i = 0d; i <= len; i += rad) {
                    list.add(new LocationTag(((LocationTag) object).clone().add(rel.clone().multiply(i))).identify());
                }
                return list.getObjectAttribute(attribute);
            }
        });

        // <--[tag]
        // @attribute <LocationTag.facing_blocks[<#>]>
        // @returns ListTag(LocationTag)
        // @description
        // Finds all block locations in the direction this location is facing,
        // optionally with a custom range (default is 100).
        // For example a location at 0,0,0 facing straight up
        // will include 0,1,0 0,2,0 and so on.
        // -->
        registerTag("facing_blocks", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                int range = attribute.getIntContext(1);
                if (range < 1) {
                    range = 100;
                }
                ListTag list = new ListTag();
                try {
                    NMSHandler.getChunkHelper().changeChunkServerThread(((LocationTag) object).getWorld());
                    BlockIterator iterator = new BlockIterator((LocationTag) object, 0, range);
                    while (iterator.hasNext()) {
                        list.add(new LocationTag(iterator.next().getLocation()).identify());
                    }
                }
                finally {
                    NMSHandler.getChunkHelper().restoreServerThread(((LocationTag) object).getWorld());
                }
                return list.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.line_of_sight[<location>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the specified location is within this location's
        // line of sight.
        // -->
        registerTag("line_of_sight", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                LocationTag location = LocationTag.valueOf(attribute.getContext(1));
                if (location != null) {
                    try {
                        NMSHandler.getChunkHelper().changeChunkServerThread(((LocationTag) object).getWorld());
                        return new ElementTag(NMSHandler.getEntityHelper().canTrace(((LocationTag) object).getWorld(), ((LocationTag) object).toVector(), location.toVector()))
                                .getObjectAttribute(attribute.fulfill(1));
                    }
                    finally {
                        NMSHandler.getChunkHelper().restoreServerThread(((LocationTag) object).getWorld());
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.direction[<location>]>
        // @returns ElementTag
        // @description
        // Returns the compass direction between two locations.
        // If no second location is specified, returns the direction of the location.
        // Example returns include "north", "southwest", ...
        // -->
        registerTag("direction", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                // <--[tag]
                // @attribute <LocationTag.direction.vector>
                // @returns LocationTag
                // @description
                // Returns the location's direction as a one-length vector.
                // -->
                if (attribute.getAttributeWithoutContext(2).equals("vector")) {
                    double xzLen = Math.cos((((LocationTag) object).getPitch() % 360) * (Math.PI / 180));
                    double nx = xzLen * Math.sin(-((LocationTag) object).getYaw() * (Math.PI / 180));
                    double ny = Math.sin(((LocationTag) object).getPitch() * (Math.PI / 180));
                    double nz = xzLen * Math.cos(((LocationTag) object).getYaw() * (Math.PI / 180));
                    return new LocationTag(((LocationTag) object).getWorld(), nx, -ny, nz).getObjectAttribute(attribute.fulfill(2));
                }
                // Get the cardinal direction from this location to another
                if (attribute.hasContext(1) && LocationTag.matches(attribute.getContext(1))) {
                    // Subtract this location's vector from the other location's vector,
                    // not the other way around
                    LocationTag target = LocationTag.valueOf(attribute.getContext(1));
                    attribute = attribute.fulfill(1);
                    EntityHelper entityHelper = NMSHandler.getEntityHelper();
                    // <--[tag]
                    // @attribute <LocationTag.direction[<location>].yaw>
                    // @returns ElementTag(Decimal)
                    // @description
                    // Returns the yaw direction between two locations.
                    // -->
                    if (attribute.startsWith("yaw")) {
                        return new ElementTag(entityHelper.normalizeYaw(entityHelper.getYaw
                                (target.toVector().subtract(((LocationTag) object).toVector())
                                        .normalize())))
                                .getObjectAttribute(attribute.fulfill(1));
                    }
                    else {
                        return new ElementTag(entityHelper.getCardinal(entityHelper.getYaw
                                (target.toVector().subtract(((LocationTag) object).toVector())
                                        .normalize())))
                                .getObjectAttribute(attribute);
                    }
                }
                // Get a cardinal direction from this location's yaw
                else {
                    return new ElementTag(NMSHandler.getEntityHelper().getCardinal(((LocationTag) object).getYaw()))
                            .getObjectAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <LocationTag.face[<location>]>
        // @returns LocationTag
        // @description
        // Returns a location containing a yaw/pitch that point from the current location
        // to the target location.
        // -->
        registerTag("face", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                Location two = LocationTag.valueOf(attribute.getContext(1));
                return new LocationTag(NMSHandler.getEntityHelper().faceLocation((LocationTag) object, two))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.facing[<entity>/<location>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the location's yaw is facing another
        // entity or location.
        // -->
        registerTag("facing", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (attribute.hasContext(1)) {

                    // The default number of degrees if there is no degrees attribute
                    int degrees = 45;

                    // The attribute to fulfill from
                    int attributePos = 1;

                    // <--[tag]
                    // @attribute <LocationTag.facing[<entity>/<location>].degrees[<#>(,<#>)]>
                    // @returns ElementTag(Boolean)
                    // @description
                    // Returns whether the location's yaw is facing another
                    // entity or location, within a specified degree range.
                    // Optionally specify a pitch limit as well.
                    // -->
                    if (attribute.getAttribute(2).startsWith("degrees") &&
                            attribute.hasContext(2)) {
                        String context = attribute.getContext(2);
                        if (context.contains(",")) {
                            String yaw = context.substring(0, context.indexOf(','));
                            String pitch = context.substring(context.indexOf(',') + 1);
                            degrees = ArgumentHelper.getIntegerFrom(yaw);
                            int pitchDegrees = ArgumentHelper.getIntegerFrom(pitch);
                            if (LocationTag.matches(attribute.getContext(1))) {
                                return new ElementTag(NMSHandler.getEntityHelper().isFacingLocation
                                        ((LocationTag) object, LocationTag.valueOf(attribute.getContext(1)), degrees, pitchDegrees))
                                        .getObjectAttribute(attribute.fulfill(attributePos));
                            }
                            else if (EntityTag.matches(attribute.getContext(1))) {
                                return new ElementTag(NMSHandler.getEntityHelper().isFacingLocation
                                        ((LocationTag) object, EntityTag.valueOf(attribute.getContext(1))
                                                .getBukkitEntity().getLocation(), degrees, pitchDegrees))
                                        .getObjectAttribute(attribute.fulfill(attributePos));
                            }
                        }
                        else {
                            degrees = attribute.getIntContext(2);
                            attributePos++;
                        }
                    }

                    if (LocationTag.matches(attribute.getContext(1))) {
                        return new ElementTag(NMSHandler.getEntityHelper().isFacingLocation
                                ((LocationTag) object, LocationTag.valueOf(attribute.getContext(1)), degrees))
                                .getObjectAttribute(attribute.fulfill(attributePos));
                    }
                    else if (EntityTag.matches(attribute.getContext(1))) {
                        return new ElementTag(NMSHandler.getEntityHelper().isFacingLocation
                                ((LocationTag) object, EntityTag.valueOf(attribute.getContext(1))
                                        .getBukkitEntity().getLocation(), degrees))
                                .getObjectAttribute(attribute.fulfill(attributePos));
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.pitch>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the pitch of the object at the location.
        // -->
        registerTag("pitch", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((LocationTag) object).getPitch()).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.with_pose[<entity>/<pitch>,<yaw>]>
        // @returns LocationTag
        // @description
        // Returns the location with pitch and yaw.
        // -->
        registerTag("with_pose", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                String context = attribute.getContext(1);
                float pitch = 0f;
                float yaw = 0f;
                if (EntityTag.matches(context)) {
                    EntityTag ent = EntityTag.valueOf(context);
                    if (ent.isSpawnedOrValidForTag()) {
                        pitch = ent.getBukkitEntity().getLocation().getPitch();
                        yaw = ent.getBukkitEntity().getLocation().getYaw();
                    }
                }
                else if (context.split(",").length == 2) {
                    String[] split = context.split(",");
                    pitch = Float.parseFloat(split[0]);
                    yaw = Float.parseFloat(split[1]);
                }
                LocationTag loc = LocationTag.valueOf(((LocationTag) object).identify());
                loc.setPitch(pitch);
                loc.setYaw(yaw);
                return loc.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.yaw>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the normalized yaw of the object at the location.
        // -->
        registerTag("yaw", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                // <--[tag]
                // @attribute <LocationTag.yaw.simple>
                // @returns ElementTag
                // @description
                // Returns the yaw as 'North', 'South', 'East', or 'West'.
                // -->
                if (attribute.getAttributeWithoutContext(2).equals("simple")) {
                    float yaw = NMSHandler.getEntityHelper().normalizeYaw(((LocationTag) object).getYaw());
                    if (yaw < 45) {
                        return new ElementTag("South")
                                .getObjectAttribute(attribute.fulfill(2));
                    }
                    else if (yaw < 135) {
                        return new ElementTag("West")
                                .getObjectAttribute(attribute.fulfill(2));
                    }
                    else if (yaw < 225) {
                        return new ElementTag("North")
                                .getObjectAttribute(attribute.fulfill(2));
                    }
                    else if (yaw < 315) {
                        return new ElementTag("East")
                                .getObjectAttribute(attribute.fulfill(2));
                    }
                    else {
                        return new ElementTag("South")
                                .getObjectAttribute(attribute.fulfill(2));
                    }
                }
                // <--[tag]
                // @attribute <LocationTag.yaw.raw>
                // @returns ElementTag(Decimal)
                // @description
                // Returns the raw yaw of the object at the location.
                // -->
                if (attribute.getAttributeWithoutContext(2).equals("raw")) {
                    return new ElementTag(((LocationTag) object).getYaw())
                            .getObjectAttribute(attribute.fulfill(2));
                }
                return new ElementTag(NMSHandler.getEntityHelper().normalizeYaw(((LocationTag) object).getYaw()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.rotate_around_x[<#.#>]>
        // @returns LocationTag
        // @description
        // Returns the location rotated around the x axis by a specified angle in radians.
        // -->
        registerTag("rotate_around_x", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                double angle = attribute.getDoubleContext(1);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                double y = (((LocationTag) object).getY() * cos) - (((LocationTag) object).getZ() * sin);
                double z = (((LocationTag) object).getY() * sin) + (((LocationTag) object).getZ() * cos);
                Location location = ((LocationTag) object).clone();
                location.setY(y);
                location.setZ(z);
                return new LocationTag(location).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.rotate_around_y[<#.#>]>
        // @returns LocationTag
        // @description
        // Returns the location rotated around the y axis by a specified angle in radians.
        // -->
        registerTag("rotate_around_y", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                double angle = attribute.getDoubleContext(1);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                double x = (((LocationTag) object).getX() * cos) + (((LocationTag) object).getZ() * sin);
                double z = (((LocationTag) object).getX() * -sin) + (((LocationTag) object).getZ() * cos);
                Location location = ((LocationTag) object).clone();
                location.setX(x);
                location.setZ(z);
                return new LocationTag(location).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.rotate_around_z[<#.#>]>
        // @returns LocationTag
        // @description
        // Returns the location rotated around the z axis by a specified angle in radians.
        // -->
        registerTag("rotate_around_z", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                double angle = attribute.getDoubleContext(1);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                double x = (((LocationTag) object).getX() * cos) - (((LocationTag) object).getY() * sin);
                double y = (((LocationTag) object).getZ() * sin) + (((LocationTag) object).getY() * cos);
                Location location = ((LocationTag) object).clone();
                location.setX(x);
                location.setY(y);
                return new LocationTag(location).getObjectAttribute(attribute.fulfill(1));
            }
        });


        /////////////////////
        //   ENTITY AND BLOCK LIST ATTRIBUTES
        /////////////////

        registerTag("find", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                attribute.fulfill(1);

                // <--[tag]
                // @attribute <LocationTag.find.blocks[<block>|...].within[<#>]>
                // @returns ListTag
                // @description
                // Returns a list of matching blocks within a radius.
                // Note: current implementation measures the center of nearby block's distance from the exact given location.
                // -->
                if (attribute.startsWith("blocks")
                        && attribute.getAttribute(2).startsWith("within")
                        && attribute.hasContext(2)) {
                    ArrayList<LocationTag> found = new ArrayList<>();
                    int radius = ArgumentHelper.matchesInteger(attribute.getContext(2)) ? attribute.getIntContext(2) : 10;
                    List<MaterialTag> materials = new ArrayList<>();
                    if (attribute.hasContext(1)) {
                        materials = ListTag.valueOf(attribute.getContext(1)).filter(MaterialTag.class, attribute.context);
                    }
                    // Avoid NPE from invalid materials
                    if (materials == null) {
                        return null;
                    }
                    int max = Settings.blockTagsMaxBlocks();
                    int index = 0;

                    attribute.fulfill(2);
                    Location tstart = ((LocationTag) object).getBlockForTag(attribute).getLocation();
                    double tstartY = tstart.getY();

                    fullloop:
                    for (int x = -(radius); x <= radius; x++) {
                        for (int y = -(radius); y <= radius; y++) {
                            double newY = y + tstartY;
                            if (newY < 0 || newY > 255) {
                                continue;
                            }
                            for (int z = -(radius); z <= radius; z++) {
                                index++;
                                if (index > max) {
                                    break fullloop;
                                }
                                if (Utilities.checkLocation((LocationTag) object, tstart.clone().add(x + 0.5, y + 0.5, z + 0.5), radius)) {
                                    if (!materials.isEmpty()) {
                                        for (MaterialTag material : materials) {
                                            if (NMSHandler.getVersion().isAtMost(NMSVersion.v1_12) && material.hasData() && material.getData() != 0) {
                                                BlockState bs = new LocationTag(tstart.clone().add(x, y, z)).getBlockStateForTag(attribute);
                                                if (bs != null && material.matchesMaterialData(bs.getData())) {
                                                    found.add(new LocationTag(tstart.clone().add(x, y, z)));
                                                }
                                            }
                                            else if (material.getMaterial() == new LocationTag(tstart.clone().add(x, y, z)).getBlockTypeForTag(attribute)) {
                                                found.add(new LocationTag(tstart.clone().add(x, y, z)));
                                            }
                                        }
                                    }
                                    else {
                                        found.add(new LocationTag(tstart.clone().add(x, y, z)));
                                    }
                                }
                            }
                        }
                    }

                    Collections.sort(found, new Comparator<LocationTag>() {
                        @Override
                        public int compare(LocationTag loc1, LocationTag loc2) {
                            return ((LocationTag) object).compare(loc1, loc2);
                        }
                    });

                    return new ListTag(found).getObjectAttribute(attribute);
                }

                // <--[tag]
                // @attribute <LocationTag.find.surface_blocks[<block>|...].within[<#.#>]>
                // @returns ListTag
                // @description
                // Returns a list of matching surface blocks within a radius.
                // -->
                else if (attribute.startsWith("surface_blocks")
                        && attribute.getAttribute(2).startsWith("within")
                        && attribute.hasContext(2)) {
                    ArrayList<LocationTag> found = new ArrayList<>();
                    double radius = ArgumentHelper.matchesDouble(attribute.getContext(2)) ? attribute.getDoubleContext(2) : 10;
                    List<MaterialTag> materials = new ArrayList<>();
                    if (attribute.hasContext(1)) {
                        materials = ListTag.valueOf(attribute.getContext(1)).filter(MaterialTag.class, attribute.context);
                    }
                    // Avoid NPE from invalid materials
                    if (materials == null) {
                        return null;
                    }
                    int max = Settings.blockTagsMaxBlocks();
                    int index = 0;

                    attribute.fulfill(2);
                    Location blockLoc = ((LocationTag) object).getBlockLocation();
                    Location loc = blockLoc.clone().add(0.5f, 0.5f, 0.5f);

                    fullloop:
                    for (double x = -(radius); x <= radius; x++) {
                        for (double y = -(radius); y <= radius; y++) {
                            for (double z = -(radius); z <= radius; z++) {
                                index++;
                                if (index > max) {
                                    break fullloop;
                                }
                                if (Utilities.checkLocation(loc, blockLoc.clone().add(x + 0.5, y + 0.5, z + 0.5), radius)) {
                                    LocationTag l = new LocationTag(blockLoc.clone().add(x, y, z));
                                    if (!materials.isEmpty()) {
                                        for (MaterialTag material : materials) {
                                            if (material.matchesBlock(l.getBlockForTag(attribute))) {
                                                if (new LocationTag(l.clone().add(0, 1, 0)).getBlockTypeForTag(attribute) == Material.AIR
                                                        && new LocationTag(l.clone().add(0, 2, 0)).getBlockTypeForTag(attribute) == Material.AIR
                                                        && l.getBlockTypeForTag(attribute) != Material.AIR) {
                                                    found.add(new LocationTag(blockLoc.clone().add(x + 0.5, y, z + 0.5)));
                                                }
                                            }
                                        }
                                    }
                                    else {
                                        if (new LocationTag(l.clone().add(0, 1, 0)).getBlockTypeForTag(attribute) == Material.AIR
                                                && new LocationTag(l.clone().add(0, 2, 0)).getBlockTypeForTag(attribute) == Material.AIR
                                                && l.getBlockTypeForTag(attribute) != Material.AIR) {
                                            found.add(new LocationTag(blockLoc.clone().add(x + 0.5, y, z + 0.5)));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Collections.sort(found, new Comparator<LocationTag>() {
                        @Override
                        public int compare(LocationTag loc1, LocationTag loc2) {
                            return ((LocationTag) object).compare(loc1, loc2);
                        }
                    });

                    return new ListTag(found).getObjectAttribute(attribute);
                }

                // <--[tag]
                // @attribute <LocationTag.find.players.within[<#.#>]>
                // @returns ListTag
                // @description
                // Returns a list of players within a radius.
                // -->
                else if (attribute.startsWith("players")
                        && attribute.getAttribute(2).startsWith("within")
                        && attribute.hasContext(2)) {
                    ArrayList<PlayerTag> found = new ArrayList<>();
                    double radius = ArgumentHelper.matchesDouble(attribute.getContext(2)) ? attribute.getDoubleContext(2) : 10;
                    attribute.fulfill(2);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!player.isDead() && Utilities.checkLocation((LocationTag) object, player.getLocation(), radius)) {
                            found.add(new PlayerTag(player));
                        }
                    }

                    Collections.sort(found, new Comparator<PlayerTag>() {
                        @Override
                        public int compare(PlayerTag pl1, PlayerTag pl2) {
                            return ((LocationTag) object).compare(pl1.getLocation(), pl2.getLocation());
                        }
                    });

                    return new ListTag(found).getObjectAttribute(attribute);
                }

                // <--[tag]
                // @attribute <LocationTag.find.npcs.within[<#.#>]>
                // @returns ListTag
                // @description
                // Returns a list of NPCs within a radius.
                // -->
                else if (attribute.startsWith("npcs")
                        && attribute.getAttribute(2).startsWith("within")
                        && attribute.hasContext(2)) {
                    ArrayList<NPCTag> found = new ArrayList<>();
                    double radius = ArgumentHelper.matchesDouble(attribute.getContext(2)) ? attribute.getDoubleContext(2) : 10;
                    attribute.fulfill(2);
                    for (NPC npc : CitizensAPI.getNPCRegistry()) {
                        if (npc.isSpawned() && Utilities.checkLocation(((LocationTag) object).getBlockForTag(attribute).getLocation(), npc.getStoredLocation(), radius)) {
                            found.add(new NPCTag(npc));
                        }
                    }

                    Collections.sort(found, new Comparator<NPCTag>() {
                        @Override
                        public int compare(NPCTag npc1, NPCTag npc2) {
                            return ((LocationTag) object).compare(npc1.getLocation(), npc2.getLocation());
                        }
                    });

                    return new ListTag(found).getObjectAttribute(attribute);
                }

                // <--[tag]
                // @attribute <LocationTag.find.entities[<entity>|...].within[<#.#>]>
                // @returns ListTag
                // @description
                // Returns a list of entities within a radius, with an optional search parameter
                // for the entity type.
                // -->
                else if (attribute.startsWith("entities")
                        && attribute.getAttribute(2).startsWith("within")
                        && attribute.hasContext(2)) {
                    ListTag ent_list = new ListTag();
                    if (attribute.hasContext(1)) {
                        ent_list = ListTag.valueOf(attribute.getContext(1));
                    }
                    ArrayList<EntityTag> found = new ArrayList<>();
                    double radius = ArgumentHelper.matchesDouble(attribute.getContext(2)) ? attribute.getDoubleContext(2) : 10;
                    attribute.fulfill(2);
                    for (Entity entity : new WorldTag(((LocationTag) object).getWorld()).getEntitiesForTag()) {
                        if (Utilities.checkLocation((LocationTag) object, entity.getLocation(), radius)) {
                            EntityTag current = new EntityTag(entity);
                            if (!ent_list.isEmpty()) {
                                for (String ent : ent_list) {
                                    if (current.comparedTo(ent)) {
                                        found.add(current);
                                        break;
                                    }
                                }
                            }
                            else {
                                found.add(current);
                            }
                        }
                    }

                    Collections.sort(found, new Comparator<EntityTag>() {
                        @Override
                        public int compare(EntityTag ent1, EntityTag ent2) {
                            return ((LocationTag) object).compare(ent1.getLocation(), ent2.getLocation());
                        }
                    });

                    return new ListTag(found).getObjectAttribute(attribute);
                }

                // <--[tag]
                // @attribute <LocationTag.find.living_entities.within[<#.#>]>
                // @returns ListTag
                // @description
                // Returns a list of living entities within a radius.
                // -->
                else if (attribute.startsWith("living_entities")
                        && attribute.getAttribute(2).startsWith("within")
                        && attribute.hasContext(2)) {
                    ArrayList<EntityTag> found = new ArrayList<>();
                    double radius = ArgumentHelper.matchesDouble(attribute.getContext(2)) ? attribute.getDoubleContext(2) : 10;
                    attribute.fulfill(2);
                    for (Entity entity : new WorldTag(((LocationTag) object).getWorld()).getEntitiesForTag()) {
                        if (entity instanceof LivingEntity
                                && Utilities.checkLocation((LocationTag) object, entity.getLocation(), radius)) {
                            found.add(new EntityTag(entity));
                        }
                    }

                    Collections.sort(found, new Comparator<EntityTag>() {
                        @Override
                        public int compare(EntityTag ent1, EntityTag ent2) {
                            return ((LocationTag) object).compare(ent1.getLocation(), ent2.getLocation());
                        }
                    });

                    return new ListTag(found).getObjectAttribute(attribute);
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.find_path[<location>]>
        // @returns ListTag(LocationTag)
        // @description
        // Returns a full list of points along the path from this location to the given location.
        // Uses a max range of 100 blocks from the start.
        // -->
        registerTag("find_path", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                LocationTag two = LocationTag.valueOf(attribute.getContext(1));
                if (two == null) {
                    return null;
                }
                List<LocationTag> locs = PathFinder.getPath((LocationTag) object, two);
                ListTag list = new ListTag();
                for (LocationTag loc : locs) {
                    list.add(loc.identify());
                }
                return list.getObjectAttribute(attribute.fulfill(1));
            }
        });


        /////////////////////
        //   IDENTIFICATION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <LocationTag.formatted>
        // @returns ElementTag
        // @description
        // Returns the formatted version of the LocationTag.
        // In the format: X 'x.x', Y 'y.y', Z 'z.z', in world 'world'
        // For example: X '1.0', Y '2.0', Z '3.0', in world 'world_nether'
        // -->
        registerTag("formatted", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                // <--[tag]
                // @attribute <LocationTag.formatted.citizens>
                // @returns ElementTag
                // @description
                // Returns the location formatted for a Citizens command.
                // In the format: x.x:y.y:z.z:world
                // For example: 1.0:2.0:3.0:world_nether
                // -->
                if (attribute.getAttributeWithoutContext(2).equals("citzens")) {
                    return new ElementTag(((LocationTag) object).getX() + ":" + ((LocationTag) object).getY() + ":" + ((LocationTag) object).getZ() + ":" + ((LocationTag) object).getWorldName()).getObjectAttribute(attribute.fulfill(2));
                }
                return new ElementTag("X '" + ((LocationTag) object).getX()
                        + "', Y '" + ((LocationTag) object).getY()
                        + "', Z '" + ((LocationTag) object).getZ()
                        + "', in world '" + ((LocationTag) object).getWorldName() + "'").getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.chunk>
        // @returns ChunkTag
        // @description
        // Returns the chunk that this location belongs to.
        // -->
        registerTag("chunk", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ChunkTag((LocationTag) object).getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("get_chunk", tagProcessor.registeredObjectTags.get("chunk"));

        // <--[tag]
        // @attribute <LocationTag.raw>
        // @returns LocationTag
        // @description
        // Returns the raw representation of this location,
        //         ignoring any notables it might match.
        // -->
        registerTag("raw", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                LocationTag rawLocation = new LocationTag((LocationTag) object);
                rawLocation.setRaw(true);
                return rawLocation.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.world>
        // @returns WorldTag
        // @description
        // Returns the world that the location is in.
        // -->
        registerTag("world", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return WorldTag.mirrorBukkitWorld(((LocationTag) object).getWorld())
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.x>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the X coordinate of the location.
        // -->
        registerTag("x", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((LocationTag) object).getX()).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.y>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the Y coordinate of the location.
        // -->
        registerTag("y", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((LocationTag) object).getY()).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.z>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the Z coordinate of the location.
        // -->
        registerTag("z", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((LocationTag) object).getZ()).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.with_x[<number>]>
        // @returns LocationTag
        // @description
        // Returns a copy of the location with a changed X value.
        // -->
        registerTag("with_x", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                LocationTag output = ((LocationTag) object).clone();
                output.setX(attribute.getDoubleContext(1));
                return output.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.with_y[<number>]>
        // @returns LocationTag
        // @description
        // Returns a copy of the location with a changed Y value.
        // -->
        registerTag("with_y", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                LocationTag output = ((LocationTag) object).clone();
                output.setY(attribute.getDoubleContext(1));
                return output.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.with_z[<number>]>
        // @returns LocationTag
        // @description
        // Returns a copy of the location with a changed Z value.
        // -->
        registerTag("with_z", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                LocationTag output = ((LocationTag) object).clone();
                output.setZ(attribute.getDoubleContext(1));
                return output.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.with_yaw[<number>]>
        // @returns LocationTag
        // @description
        // Returns a copy of the location with a changed yaw value.
        // -->
        registerTag("with_yaw", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                LocationTag output = ((LocationTag) object).clone();
                output.setYaw((float) attribute.getDoubleContext(1));
                return output.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.with_pitch[<number>]>
        // @returns LocationTag
        // @description
        // Returns a copy of the location with a changed pitch value.
        // -->
        registerTag("with_pitch", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                LocationTag output = ((LocationTag) object).clone();
                output.setPitch((float) attribute.getDoubleContext(1));
                return output.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.with_world[<world>]>
        // @returns LocationTag
        // @description
        // Returns a copy of the location with a changed world value.
        // -->
        registerTag("with_world", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                LocationTag output = ((LocationTag) object).clone();
                WorldTag world = WorldTag.valueOf(attribute.getContext(1));
                output.setWorld(world.getWorld());
                return output.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.notable_name>
        // @returns ElementTag
        // @description
        // Gets the name of a Notable LocationTag. If the location isn't noted,
        // this is null.
        // -->
        registerTag("notable_name", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                String notname = NotableManager.getSavedId(((LocationTag) object));
                if (notname == null) {
                    return null;
                }
                return new ElementTag(notname).getObjectAttribute(attribute.fulfill(1));
            }
        });


        /////////////////////
        //   MATHEMATICAL ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <LocationTag.add[<location>]>
        // @returns LocationTag
        // @description
        // Returns the location with the specified coordinates added to it.
        // -->
        registerTag("add", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                String[] ints = attribute.getContext(1).replace("l@", "").split(",", 4); // TODO: Just LocationTag.valueOf?
                if (ints.length >= 3) {
                    if ((ArgumentHelper.matchesDouble(ints[0]) || ArgumentHelper.matchesInteger(ints[0]))
                            && (ArgumentHelper.matchesDouble(ints[1]) || ArgumentHelper.matchesInteger(ints[1]))
                            && (ArgumentHelper.matchesDouble(ints[2]) || ArgumentHelper.matchesInteger(ints[2]))) {
                        return new LocationTag(((LocationTag) object).clone().add(Double.valueOf(ints[0]),
                                Double.valueOf(ints[1]),
                                Double.valueOf(ints[2]))).getObjectAttribute(attribute.fulfill(1));
                    }
                }
                else if (LocationTag.matches(attribute.getContext(1))) {
                    return new LocationTag(((LocationTag) object).clone().add(LocationTag.valueOf(attribute.getContext(1))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.sub[<location>]>
        // @returns LocationTag
        // @description
        // Returns the location with the specified coordinates subtracted from it.
        // -->
        registerTag("sub", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                String[] ints = attribute.getContext(1).replace("l@", "").split(",", 4); // TODO: Just LocationTag.valueOf?
                if (ints.length == 3 || ints.length == 4) {
                    if ((ArgumentHelper.matchesDouble(ints[0]) || ArgumentHelper.matchesInteger(ints[0]))
                            && (ArgumentHelper.matchesDouble(ints[1]) || ArgumentHelper.matchesInteger(ints[1]))
                            && (ArgumentHelper.matchesDouble(ints[2]) || ArgumentHelper.matchesInteger(ints[2]))) {
                        return new LocationTag(((LocationTag) object).clone().subtract(Double.valueOf(ints[0]),
                                Double.valueOf(ints[1]),
                                Double.valueOf(ints[2]))).getObjectAttribute(attribute.fulfill(1));
                    }
                }
                else if (LocationTag.matches(attribute.getContext(1))) {
                    return new LocationTag(((LocationTag) object).clone().subtract(LocationTag.valueOf(attribute.getContext(1))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.mul[<length>]>
        // @returns LocationTag
        // @description
        // Returns the location multiplied by the specified length.
        // -->
        registerTag("mul", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                return new LocationTag(((LocationTag) object).clone().multiply(Double.parseDouble(attribute.getContext(1))))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.div[<length>]>
        // @returns LocationTag
        // @description
        // Returns the location divided by the specified length.
        // -->
        registerTag("div", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                return new LocationTag(((LocationTag) object).clone().multiply(1D / Double.parseDouble(attribute.getContext(1))))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.normalize>
        // @returns LocationTag
        // @description
        // Returns a 1-length vector in the same direction as this vector location.
        // -->
        registerTag("normalize", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                double len = Math.sqrt(Math.pow(((LocationTag) object).getX(), 2) + Math.pow(((LocationTag) object).getY(), 2) + Math.pow(((LocationTag) object).getZ(), 2));
                if (len == 0) {
                    len = 1;
                }
                return new LocationTag(((LocationTag) object).clone().multiply(1D / len))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.vector_length>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the 3D length of the vector/location.
        // -->
        registerTag("vector_length", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(Math.sqrt(Math.pow(((LocationTag) object).getX(), 2) + Math.pow(((LocationTag) object).getY(), 2) + Math.pow(((LocationTag) object).getZ(), 2)))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.vector_to_face>
        // @returns ElementTag
        // @description
        // Returns the name of the BlockFace represented by a vector.
        // Result can be any of the following:
        // NORTH, EAST, SOUTH, WEST, UP, DOWN, NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST,
        // WEST_NORTH_WEST, NORTH_NORTH_WEST, NORTH_NORTH_EAST, EAST_NORTH_EAST, EAST_SOUTH_EAST,
        // SOUTH_SOUTH_EAST, SOUTH_SOUTH_WEST, WEST_SOUTH_WEST, SELF
        // -->
        registerTag("vector_to_face", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                BlockFace face = Utilities.faceFor(((LocationTag) object).toVector());
                if (face != null) {
                    return new ElementTag(face.name())
                            .getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.distance_squared[<location>]>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the distance between 2 locations, squared.
        // -->
        registerTag("distance_squared", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                if (LocationTag.matches(attribute.getContext(1))) {
                    LocationTag toLocation = LocationTag.valueOf(attribute.getContext(1));
                    if (!((LocationTag) object).getWorldName().equalsIgnoreCase(toLocation.getWorldName())) {
                        if (!attribute.hasAlternative()) {
                            Debug.echoError("Can't measure distance between two different worlds!");
                        }
                        return null;
                    }
                    return new ElementTag(((LocationTag) object).distanceSquared(toLocation))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.distance[<location>]>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the distance between 2 locations.
        // -->
        registerTag("distance", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                if (LocationTag.matches(attribute.getContext(1))) {
                    LocationTag toLocation = LocationTag.valueOf(attribute.getContext(1));

                    // <--[tag]
                    // @attribute <LocationTag.distance[<location>].horizontal>
                    // @returns ElementTag(Decimal)
                    // @description
                    // Returns the horizontal distance between 2 locations.
                    // -->
                    if (attribute.getAttribute(2).startsWith("horizontal")) {

                        // <--[tag]
                        // @attribute <LocationTag.distance[<location>].horizontal.multiworld>
                        // @returns ElementTag(Decimal)
                        // @description
                        // Returns the horizontal distance between 2 multiworld locations.
                        // -->
                        if (attribute.getAttribute(3).startsWith("multiworld")) {
                            return new ElementTag(Math.sqrt(
                                    Math.pow(((LocationTag) object).getX() - toLocation.getX(), 2) +
                                            Math.pow(((LocationTag) object).getZ() - toLocation.getZ(), 2)))
                                    .getObjectAttribute(attribute.fulfill(3));
                        }
                        else if (((LocationTag) object).getWorldName().equalsIgnoreCase(toLocation.getWorldName())) {
                            return new ElementTag(Math.sqrt(
                                    Math.pow(((LocationTag) object).getX() - toLocation.getX(), 2) +
                                            Math.pow(((LocationTag) object).getZ() - toLocation.getZ(), 2)))
                                    .getObjectAttribute(attribute.fulfill(2));
                        }
                    }

                    // <--[tag]
                    // @attribute <LocationTag.distance[<location>].vertical>
                    // @returns ElementTag(Decimal)
                    // @description
                    // Returns the vertical distance between 2 locations.
                    // -->
                    else if (attribute.getAttribute(2).startsWith("vertical")) {

                        // <--[tag]
                        // @attribute <LocationTag.distance[<location>].vertical.multiworld>
                        // @returns ElementTag(Decimal)
                        // @description
                        // Returns the vertical distance between 2 multiworld locations.
                        // -->
                        if (attribute.getAttribute(3).startsWith("multiworld")) {
                            return new ElementTag(Math.abs(((LocationTag) object).getY() - toLocation.getY()))
                                    .getObjectAttribute(attribute.fulfill(3));
                        }
                        else if (((LocationTag) object).getWorldName().equalsIgnoreCase(toLocation.getWorldName())) {
                            return new ElementTag(Math.abs(((LocationTag) object).getY() - toLocation.getY()))
                                    .getObjectAttribute(attribute.fulfill(2));
                        }
                    }

                    if (!((LocationTag) object).getWorldName().equalsIgnoreCase(toLocation.getWorldName())) {
                        if (!attribute.hasAlternative()) {
                            Debug.echoError("Can't measure distance between two different worlds!");
                        }
                        return null;
                    }
                    else {
                        return new ElementTag(((LocationTag) object).distance(toLocation))
                                .getObjectAttribute(attribute.fulfill(1));
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.is_within_border>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the location is within the world border.
        // -->
        registerTag("is_within_border", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((LocationTag) object).getWorld().getWorldBorder().isInside((LocationTag) object))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.is_within[<cuboid>/<ellipsoid>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the location is within the cuboid or ellipsoid.
        // -->
        registerTag("is_within", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                if (EllipsoidTag.matches(attribute.getContext(1))) {
                    EllipsoidTag ellipsoid = EllipsoidTag.valueOf(attribute.getContext(1));
                    if (ellipsoid != null) {
                        return new ElementTag(ellipsoid.contains((LocationTag) object))
                                .getObjectAttribute(attribute.fulfill(1));
                    }
                }
                else {
                    CuboidTag cuboid = CuboidTag.valueOf(attribute.getContext(1));
                    if (cuboid != null) {
                        return new ElementTag(cuboid.isInsideCuboid((LocationTag) object))
                                .getObjectAttribute(attribute.fulfill(1));
                    }
                }
                return null;
            }
        });


        /////////////////////
        //   STATE ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <LocationTag.biome>
        // @mechanism LocationTag.biome
        // @returns BiomeTag
        // @description
        // Returns the biome at the location.
        // -->
        registerTag("biome", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                attribute = attribute.fulfill(1);
                // <--[tag]
                // @attribute <LocationTag.biome.formatted>
                // @returns ElementTag
                // @description
                // Returns the formatted biome name at the location.
                // -->
                if (attribute.startsWith("formatted")) {
                    return new ElementTag(CoreUtilities.toLowerCase(((LocationTag) object).getBiomeForTag(attribute).name()).replace('_', ' '))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                return new BiomeTag(((LocationTag) object).getBiomeForTag(attribute))
                        .getObjectAttribute(attribute);
            }
        });

        // <--[tag]
        // @attribute <LocationTag.cuboids>
        // @returns ListTag(CuboidTag)
        // @description
        // Returns a ListTag of all notable CuboidTags that include this location.
        // -->
        registerTag("cuboids", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                List<CuboidTag> cuboids = CuboidTag.getNotableCuboidsContaining((LocationTag) object);
                ListTag cuboid_list = new ListTag();
                for (CuboidTag cuboid : cuboids) {
                    cuboid_list.add(cuboid.identify());
                }
                return cuboid_list.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.ellipsoids>
        // @returns ListTag(CuboidTag)
        // @description
        // Returns a ListTag of all notable EllipsoidTags that include this location.
        // -->
        registerTag("ellipsoids", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                List<EllipsoidTag> ellipsoids = EllipsoidTag.getNotableEllipsoidsContaining((LocationTag) object);
                ListTag ellipsoid_list = new ListTag();
                for (EllipsoidTag ellipsoid : ellipsoids) {
                    ellipsoid_list.add(ellipsoid.identify());
                }
                return ellipsoid_list.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.is_liquid>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the block at the location is a liquid.
        // -->
        registerTag("is_liquid", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Block b = ((LocationTag) object).getBlockForTag(attribute);
                if (b != null) {
                    try {
                        NMSHandler.getChunkHelper().changeChunkServerThread(((LocationTag) object).getWorld());
                        return new ElementTag(b.isLiquid()).getObjectAttribute(attribute.fulfill(1));
                    }
                    finally {
                        NMSHandler.getChunkHelper().restoreServerThread(((LocationTag) object).getWorld());
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.light>
        // @returns ElementTag(Number)
        // @description
        // Returns the total amount of light on the location.
        // -->
        registerTag("light", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Block b = ((LocationTag) object).getBlockForTag(attribute);
                if (b != null) {
                    try {
                        NMSHandler.getChunkHelper().changeChunkServerThread(((LocationTag) object).getWorld());
                        // <--[tag]
                        // @attribute <LocationTag.light.blocks>
                        // @returns ElementTag(Number)
                        // @description
                        // Returns the amount of light from light blocks that is
                        // on the location.
                        // -->
                        if (attribute.getAttributeWithoutContext(2).equals("blocks")) {
                            return new ElementTag(((LocationTag) object).getBlockForTag(attribute).getLightFromBlocks())
                                    .getObjectAttribute(attribute.fulfill(2));
                        }
                        // <--[tag]
                        // @attribute <LocationTag.light.sky>
                        // @returns ElementTag(Number)
                        // @description
                        // Returns the amount of light from the sky that is
                        // on the location.
                        // -->
                        if (attribute.getAttributeWithoutContext(2).equals("sky")) {
                            return new ElementTag(((LocationTag) object).getBlockForTag(attribute).getLightFromSky())
                                    .getObjectAttribute(attribute.fulfill(2));
                        }
                        return new ElementTag(((LocationTag) object).getBlockForTag(attribute).getLightLevel())
                                .getObjectAttribute(attribute.fulfill(1));
                    }
                    finally {
                        NMSHandler.getChunkHelper().restoreServerThread(((LocationTag) object).getWorld());
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.power>
        // @returns ElementTag(Number)
        // @description
        // Returns the current redstone power level of a block.
        // -->
        registerTag("power", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                Block b = ((LocationTag) object).getBlockForTag(attribute);
                if (b != null) {
                    try {
                        NMSHandler.getChunkHelper().changeChunkServerThread(((LocationTag) object).getWorld());
                        return new ElementTag(((LocationTag) object).getBlockForTag(attribute).getBlockPower())
                                .getObjectAttribute(attribute.fulfill(1));
                    }
                    finally {
                        NMSHandler.getChunkHelper().restoreServerThread(((LocationTag) object).getWorld());
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.tree_distance>
        // @returns ElementTag(Number)
        // @group properties
        // @description
        // Returns a number of how many blocks away from a connected tree leaves are.
        // Defaults to 7 if not connected to a tree.
        // -->
        registerTag("tree_distance", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                MaterialTag material = new MaterialTag(((LocationTag) object).getBlockForTag(attribute));
                if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_13)
                        && MaterialLeaves.describes(material)) {
                    return new ElementTag(MaterialLeaves.getFrom(material).getDistance())
                            .getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.type>
        // @returns ElementTag
        // @description
        // Always returns 'Location' for LocationTag objects. All objects fetchable by the Object Fetcher will return the
        // type of object that is fulfilling this attribute.
        // -->
        registerTag("type", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag("Location").getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.command_block_name>
        // @returns ElementTag
        // @mechanism LocationTag.command_block_name
        // @description
        // Returns the name a command block is set to.
        // -->
        registerTag("command_block_name", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!(((LocationTag) object).getBlockStateForTag(attribute) instanceof CommandBlock)) {
                    return null;
                }
                return new ElementTag(((CommandBlock) ((LocationTag) object).getBlockStateForTag(attribute)).getName())
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.command_block>
        // @returns ElementTag
        // @mechanism LocationTag.command_block
        // @description
        // Returns the command a command block is set to.
        // -->
        registerTag("command_block", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!(((LocationTag) object).getBlockStateForTag(attribute) instanceof CommandBlock)) {
                    return null;
                }
                return new ElementTag(((CommandBlock) ((LocationTag) object).getBlockStateForTag(attribute)).getCommand())
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.furnace_burn_time>
        // @returns ElementTag(Number)
        // @mechanism LocationTag.furnace_burn_time
        // @description
        // Returns the burn time a furnace has left.
        // -->
        registerTag("furnace_burn_time", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((Furnace) ((LocationTag) object).getBlockStateForTag(attribute)).getBurnTime())
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.furnace_cook_time>
        // @returns ElementTag(Number)
        // @mechanism LocationTag.furnace_cook_time
        // @description
        // Returns the cook time a furnace has left.
        // -->
        registerTag("furnace_cook_time", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((Furnace) ((LocationTag) object).getBlockStateForTag(attribute)).getCookTime())
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <LocationTag.attached_to>
        // @returns LocationTag
        // @description
        // Returns the block this block is attached to.
        // (Only if it is a lever or button!)
        // -->
        registerTag("attached_to", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                BlockFace face = BlockFace.SELF;
                MaterialTag material = new MaterialTag(((LocationTag) object).getBlockForTag(attribute));
                if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_13)
                        && MaterialSwitchFace.describes(material)) {
                    face = MaterialSwitchFace.getFrom(material).getAttachedTo();
                }
                else {
                    MaterialData data = ((LocationTag) object).getBlockStateForTag(attribute).getData();
                    if (data instanceof Attachable) {
                        face = ((Attachable) data).getAttachedFace();
                    }
                }
                if (face != BlockFace.SELF) {
                    return new LocationTag(((LocationTag) object).getBlockForTag(attribute).getRelative(face).getLocation())
                            .getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.other_block>
        // @returns LocationTag
        // @description
        // If the location is part of a double-block structure
        // (double chests, doors, beds, etc), returns the location of the other block in the double-block structure.
        // -->
        registerTag("other_block", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                BlockState state = ((LocationTag) object).getBlockStateForTag(attribute);
                if (state instanceof Chest
                        && NMSHandler.getVersion().isAtLeast(NMSVersion.v1_13)) {
                    Vector direction = DirectionalBlocksHelper.getFacing(((LocationTag) object).getBlockForTag(attribute));
                    if (DirectionalBlocksHelper.isLeftHalf(((LocationTag) object).getBlockForTag(attribute))) {
                        direction = new Vector(-direction.getZ(), 0, direction.getX());
                    }
                    else if (DirectionalBlocksHelper.isRightHalf(((LocationTag) object).getBlockForTag(attribute))) {
                        direction = new Vector(direction.getZ(), 0, -direction.getX());
                    }
                    else {
                        if (!attribute.hasAlternative()) {
                            Debug.echoError("Block is a single-block chest.");
                        }
                        return null;
                    }
                    return new LocationTag(((LocationTag) object).clone().add(direction)).getObjectAttribute(attribute.fulfill(1));
                }
                else if (state instanceof Chest) {
                    // There is no remotely sane API for this.
                    InventoryHolder holder = ((Chest) state).getBlockInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        Location left = ((DoubleChest) holder).getLeftSide().getInventory().getLocation();
                        Location right = ((DoubleChest) holder).getRightSide().getInventory().getLocation();
                        if (left.getBlockX() == ((LocationTag) object).getBlockX() && left.getBlockY() == ((LocationTag) object).getBlockY() && left.getBlockZ() == ((LocationTag) object).getBlockZ()) {
                            return new LocationTag(right).getObjectAttribute(attribute.fulfill(1));
                        }
                        else {
                            return new LocationTag(left).getObjectAttribute(attribute.fulfill(1));
                        }
                    }
                }
                else if (state instanceof Bed
                        && NMSHandler.getVersion().isAtLeast(NMSVersion.v1_13)) {
                    // There's no pre-1.13 API for this *at all*, and the new API isn't very sane, but can be used.
                    boolean isTop = DirectionalBlocksHelper.isTopHalf(((LocationTag) object).getBlockForTag(attribute));
                    BlockFace direction = DirectionalBlocksHelper.getFace(((LocationTag) object).getBlockForTag(attribute));
                    if (!isTop) {
                        direction = direction.getOppositeFace();
                    }
                    return new LocationTag(((LocationTag) object).clone().add(direction.getDirection())).getObjectAttribute(attribute.fulfill(1));
                }
                else if (state.getData() instanceof Door) {
                    if (((Door) state.getData()).isTopHalf()) {
                        return new LocationTag(((LocationTag) object).clone().subtract(0, 1, 0)).getObjectAttribute(attribute.fulfill(1));
                    }
                    else {
                        return new LocationTag(((LocationTag) object).clone().add(0, 1, 0)).getObjectAttribute(attribute.fulfill(1));
                    }
                }
                else {
                    if (!attribute.hasAlternative()) {
                        Debug.echoError("Block of type " + ((LocationTag) object).getBlockTypeForTag(attribute).name() + " isn't supported by other_block.");
                    }
                    return null;
                }
                if (!attribute.hasAlternative()) {
                    Debug.echoError("Block of type " + ((LocationTag) object).getBlockTypeForTag(attribute).name() + " doesn't have an other block.");
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <LocationTag.custom_name>
        // @returns ElementTag
        // @mechanism LocationTag.custom_name
        // @description
        // Returns the custom name of this block.
        // Only works for nameable blocks, such as chests and dispensers.
        // -->
        registerTag("custom_name", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (((LocationTag) object).getBlockStateForTag(attribute) instanceof Nameable) {
                    return new ElementTag(((Nameable) ((LocationTag) object).getBlockStateForTag(attribute)).getCustomName())
                            .getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });
    }

    public static ObjectTagProcessor tagProcessor = new ObjectTagProcessor();

    public static void registerTag(String name, TagRunnable.ObjectForm runnable) {
        tagProcessor.registerTag(name, runnable);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a location!");
    }

    @Override
    public void adjust(Mechanism mechanism) {

        if (mechanism.matches("data") && mechanism.hasValue()) {
            Deprecations.materialIds.warn(mechanism.context);
            BlockData blockData = NMSHandler.getBlockHelper().getBlockData(getBlock().getType(), (byte) mechanism.getValue().asInt());
            blockData.setBlock(getBlock(), false);
        }

        // <--[mechanism]
        // @object LocationTag
        // @name block_facing
        // @input LocationTag
        // @description
        // Sets the facing direction of the block, as a vector.
        // @tags
        // <LocationTag.block_facing>
        // -->
        if (mechanism.matches("block_facing") && mechanism.requireObject(LocationTag.class)) {
            LocationTag faceVec = mechanism.valueAsType(LocationTag.class);
            DirectionalBlocksHelper.setFacing(getBlock(), faceVec.toVector());
        }

        // <--[mechanism]
        // @object LocationTag
        // @name block_type
        // @input MaterialTag
        // @description
        // Sets the type of the block.
        // @tags
        // <LocationTag.material>
        // -->
        if (mechanism.matches("block_type") && mechanism.requireObject(MaterialTag.class)) {
            MaterialTag mat = mechanism.valueAsType(MaterialTag.class);
            mat.getNmsBlockData().setBlock(getBlock(), false);
        }

        // <--[mechanism]
        // @object LocationTag
        // @name biome
        // @input BiomeTag
        // @description
        // Sets the biome of the block.
        // @tags
        // <LocationTag.biome>
        // -->
        if (mechanism.matches("biome") && mechanism.requireObject(BiomeTag.class)) {
            mechanism.valueAsType(BiomeTag.class).getBiome().changeBlockBiome(this);
        }

        // <--[mechanism]
        // @object LocationTag
        // @name spawner_type
        // @input EntityTag
        // @description
        // Sets the entity that a mob spawner will spawn.
        // @tags
        // <LocationTag.spawner_type>
        // -->
        if (mechanism.matches("spawner_type") && mechanism.requireObject(EntityTag.class)
                && getBlockState() instanceof CreatureSpawner) {
            CreatureSpawner spawner = ((CreatureSpawner) getBlockState());
            spawner.setSpawnedType(mechanism.valueAsType(EntityTag.class).getBukkitEntityType());
            spawner.update();
        }

        // <--[mechanism]
        // @object LocationTag
        // @name lock
        // @input Element
        // @description
        // Sets the container's lock password.
        // Locked containers can only be opened while holding an item with the name of the lock.
        // Leave blank to remove a container's lock.
        // @tags
        // <LocationTag.lock>
        // <LocationTag.is_locked>
        // <LocationTag.is_lockable>
        // -->
        if (mechanism.matches("lock") && getBlockState() instanceof Lockable) {
            BlockState state = getBlockState();
            ((Lockable) state).setLock(mechanism.hasValue() ? mechanism.getValue().asString() : null);
            state.update();
        }

        // <--[mechanism]
        // @object LocationTag
        // @name sign_contents
        // @input ListTag
        // @description
        // Sets the contents of a sign block.
        // Note that this takes an escaped list.
        // See <@link language property escaping>.
        // @tags
        // <LocationTag.sign_contents>
        // -->
        if (mechanism.matches("sign_contents") && getBlockState() instanceof Sign) {
            Sign state = (Sign) getBlockState();
            for (int i = 0; i < 4; i++) {
                state.setLine(i, "");
            }
            ListTag list = mechanism.valueAsType(ListTag.class);
            if (list.size() > 4) {
                Debug.echoError("Sign can only hold four lines!");
            }
            else {
                for (int i = 0; i < list.size(); i++) {
                    state.setLine(i, EscapeTagBase.unEscape(list.get(i)));
                }
            }
            state.update();
        }

        // <--[mechanism]
        // @object LocationTag
        // @name skull_skin
        // @input Element(|Element(|Element))
        // @description
        // Sets the skin of a skull block.
        // The first ElementTag is a UUID.
        // Optionally, use the second ElementTag for the skin texture cache.
        // Optionally, use the third ElementTag for a player name.
        // @tags
        // <LocationTag.skull_skin>
        // -->
        if (mechanism.matches("skull_skin")) {
            final BlockState blockState = getBlockState();
            Material material = getBlock().getType();
            if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_13)
                    && material != Material.PLAYER_HEAD && material != Material.PLAYER_WALL_HEAD) {
                Deprecations.skullSkinMaterials.warn(mechanism.context);
            }
            else if (blockState instanceof Skull) {
                ListTag list = mechanism.valueAsType(ListTag.class);
                String idString = list.get(0);
                String texture = null;
                if (list.size() > 1) {
                    texture = list.get(1);
                }
                PlayerProfile profile;
                if (idString.contains("-")) {
                    UUID uuid = UUID.fromString(idString);
                    String name = null;
                    if (list.size() > 2) {
                        name = list.get(2);
                    }
                    profile = new PlayerProfile(name, uuid, texture);
                }
                else {
                    profile = new PlayerProfile(idString, null, texture);
                }
                profile = NMSHandler.getInstance().fillPlayerProfile(profile);
                if (texture != null) { // Ensure we didn't get overwritten
                    profile.setTexture(texture);
                }
                NMSHandler.getBlockHelper().setPlayerProfile((Skull) blockState, profile);
            }
            else {
                Debug.echoError("Unable to set skull_skin on block of type " + material.name() + " with state " + blockState.getClass().getCanonicalName());
            }
        }

        // <--[mechanism]
        // @object LocationTag
        // @name flowerpot_contents
        // @input MaterialTag
        // @description
        // Sets the contents of a flower pot.
        // NOTE: Replaced by materials (such as POTTED_CACTUS) in 1.13 and above.
        // NOTE: Flowerpot contents will not update client-side until players refresh the chunk.
        // Refresh a chunk manually with mechanism: refresh_chunk_sections for ChunkTag objects
        // @tags
        // <LocationTag.flowerpot_contents>
        // -->
        if (mechanism.matches("flowerpot_contents") && mechanism.requireObject(MaterialTag.class)) {
            if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_13)) {
                Deprecations.flowerpotMechanism.warn(mechanism.context);
            }
            else if (getBlock().getType() == Material.FLOWER_POT) {
                MaterialData data = mechanism.valueAsType(MaterialTag.class).getMaterialData();
                NMSHandler.getBlockHelper().setFlowerpotContents(getBlock(), data);
            }
        }

        // <--[mechanism]
        // @object LocationTag
        // @name command_block_name
        // @input Element
        // @description
        // Sets the name of a command block.
        // @tags
        // <LocationTag.command_block_name>
        // -->
        if (mechanism.matches("command_block_name")) {
            if (getBlock().getType() == MaterialCompat.COMMAND_BLOCK) {
                CommandBlock block = ((CommandBlock) getBlockState());
                block.setName(mechanism.getValue().asString());
                block.update();
            }
        }

        // <--[mechanism]
        // @object LocationTag
        // @name command_block
        // @input Element
        // @description
        // Sets the command of a command block.
        // @tags
        // <LocationTag.command_block>
        // -->
        if (mechanism.matches("command_block")) {
            if (getBlock().getType() == MaterialCompat.COMMAND_BLOCK) {
                CommandBlock block = ((CommandBlock) getBlockState());
                block.setCommand(mechanism.getValue().asString());
                block.update();
            }
        }

        // <--[mechanism]
        // @object LocationTag
        // @name custom_name
        // @input Element
        // @description
        // Sets the custom name of the block.
        // Use no value to reset the block's name.
        // @tags
        // <LocationTag.custom_name>
        // -->
        if (mechanism.matches("custom_name")) {
            if (getBlockState() instanceof Nameable) {
                String title = null;
                if (mechanism.hasValue()) {
                    title = mechanism.getValue().asString();
                }
                BlockState state = getBlockState();
                ((Nameable) state).setCustomName(title);
                state.update(true);
            }
        }

        // <--[mechanism]
        // @object LocationTag
        // @name furnace_burn_time
        // @input Element(Number)
        // @description
        // Sets the burn time for a furnace in ticks. Maximum is 32767.
        // @tags
        // <LocationTag.furnace_burn_time>
        // -->
        if (mechanism.matches("furnace_burn_time")) {
            if (MaterialCompat.isFurnace(getBlock().getType())) {
                Furnace furnace = (Furnace) getBlockState();
                furnace.setBurnTime((short) mechanism.getValue().asInt());
                furnace.update();
            }
        }

        // <--[mechanism]
        // @object LocationTag
        // @name furnace_cook_time
        // @input Element(Number)
        // @description
        // Sets the cook time for a furnace in ticks. Maximum is 32767.
        // @tags
        // <LocationTag.furnace_cook_time>
        // -->
        if (mechanism.matches("furnace_cook_time")) {
            if (MaterialCompat.isFurnace(getBlock().getType())) {
                Furnace furnace = (Furnace) getBlockState();
                furnace.setCookTime((short) mechanism.getValue().asInt());
                furnace.update();
            }
        }

        // <--[mechanism]
        // @object LocationTag
        // @name base_color
        // @input Element
        // @description
        // Changes the base color of the banner at this location.
        // For the list of possible colors, see <@link url http://bit.ly/1dydq12>.
        // As of 1.13+, this mechanism is no longer relevant.
        // @tags
        // <LocationTag.base_color>
        // -->
        if (mechanism.matches("base_color")) {
            if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_13)) {
                Debug.echoError("Base_Color mechanism no longer relevant: banner types are now distinct materials.");
            }
            Banner banner = (Banner) getBlockState();
            banner.setBaseColor(DyeColor.valueOf(mechanism.getValue().asString().toUpperCase()));
            banner.update();
        }

        // <--[mechanism]
        // @object LocationTag
        // @name patterns
        // @input ListTag
        // @description
        // Changes the patterns of the banner at this location. Input must be in the form
        // "li@COLOR/PATTERN|COLOR/PATTERN" etc.
        // For the list of possible colors, see <@link url http://bit.ly/1dydq12>.
        // For the list of possible patterns, see <@link url http://bit.ly/1MqRn7T>.
        // @tags
        // <LocationTag.patterns>
        // <server.list_patterns>
        // -->
        if (mechanism.matches("patterns")) {
            List<org.bukkit.block.banner.Pattern> patterns = new ArrayList<>();
            ListTag list = mechanism.valueAsType(ListTag.class);
            List<String> split;
            for (String string : list) {
                try {
                    split = CoreUtilities.split(string, '/', 2);
                    patterns.add(new org.bukkit.block.banner.Pattern(DyeColor.valueOf(split.get(0).toUpperCase()),
                            PatternType.valueOf(split.get(1).toUpperCase())));
                }
                catch (Exception e) {
                    Debug.echoError("Could not apply pattern to banner: " + string);
                }
            }
            Banner banner = (Banner) getBlockState();
            banner.setPatterns(patterns);
            banner.update();
        }

        // <--[mechanism]
        // @object LocationTag
        // @name head_rotation
        // @input Element(Number)
        // @description
        // Sets the rotation of the head at this location. Must be an integer 1 to 16.
        // @tags
        // <LocationTag.head_rotation>
        // -->
        if (mechanism.matches("head_rotation") && mechanism.requireInteger()) {
            Skull sk = (Skull) getBlockState();
            sk.setRotation(getSkullBlockFace(mechanism.getValue().asInt() - 1));
            sk.update();
        }

        // <--[mechanism]
        // @object LocationTag
        // @name generate_tree
        // @input Element
        // @description
        // Generates a tree at this location if possible.
        // For a list of valid tree types, see <@link url http://bit.ly/2o7m1je>
        // @tags
        // <server.list_tree_types>
        // -->
        if (mechanism.matches("generate_tree") && mechanism.requireEnum(false, TreeType.values())) {
            boolean generated = getWorld().generateTree(this, TreeType.valueOf(mechanism.getValue().asString().toUpperCase()));
            if (!generated) {
                Debug.echoError("Could not generate tree at " + identifySimple() + ". Make sure this location can naturally generate a tree!");
            }
        }

        // <--[mechanism]
        // @object LocationTag
        // @name activate
        // @input None
        // @description
        // Activates the block at the location if possible.
        // Works for blocks like dispensers, which have explicit 'activation' methods.
        // -->
        if (mechanism.matches("activate")) {
            BlockState state = getBlockState();
            if (state instanceof Dispenser) {
                ((Dispenser) state).dispense();
            }
            else if (state instanceof Dropper) {
                ((Dropper) state).drop();
            }
        }

        CoreUtilities.autoPropertyMechanism(this, mechanism);
    }
}
