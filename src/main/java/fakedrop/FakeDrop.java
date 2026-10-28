package fakedrop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class FakeDrop implements ClientModInitializer {
    public static ItemStack item = new ItemStack(Items.DIAMOND);
    public static boolean enabled = true;

    private static final int RADIUS = 6;
    private static final int MAX_LIVE = 60;
    private static final Map<BlockPos, Boolean> last = new HashMap<>();
    private static final List<int[]> pending = new ArrayList<>();      // {x,y,z,ticksLeft}
    private static final List<Object[]> live = new ArrayList<>();      // {ItemEntity, age}
    private static int nextId = -1_000_000;
    private static KeyBinding openKey;
    private static final Random rng = new Random();

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fakedrop.open", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(FakeDrop::tick);
    }

    private static void tick(MinecraftClient mc) {
        while (openKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new PickScreen());
        }
        ClientWorld world = mc.world;
        if (world == null || mc.player == null) { last.clear(); pending.clear(); live.clear(); return; }

        // 1) detect droppers that just got powered
        if (enabled) {
            BlockPos p = mc.player.getBlockPos();
            for (BlockPos pos : BlockPos.iterate(p.add(-RADIUS, -RADIUS, -RADIUS), p.add(RADIUS, RADIUS, RADIUS))) {
                BlockState st = world.getBlockState(pos);
                if (!st.isOf(Blocks.DROPPER)) continue;
                boolean t = st.get(DispenserBlock.TRIGGERED);
                Boolean prev = last.put(pos.toImmutable(), t);
                if (t && Boolean.FALSE.equals(prev)) pending.add(new int[]{pos.getX(), pos.getY(), pos.getZ(), 4});
            }
            if (last.size() > 512) last.clear();
        }

        // 2) delayed spawn (real droppers fire 4 ticks after power)
        Iterator<int[]> it = pending.iterator();
        while (it.hasNext()) {
            int[] pd = it.next();
            if (--pd[3] <= 0) {
                BlockPos pos = new BlockPos(pd[0], pd[1], pd[2]);
                BlockState st = world.getBlockState(pos);
                if (st.isOf(Blocks.DROPPER)) spawn(world, pos, st);
                it.remove();
            }
        }

        // 3) keep fake items forever (until cleared / world change); drop only entities that were removed
        live.removeIf(o -> ((ItemEntity) o[0]).isRemoved());
        while (live.size() > MAX_LIVE) {
            ((ItemEntity) live.remove(0)[0]).discard();
        }
    }

    /** Change the selected item; also swaps the look of fake items already on the ground. */
    public static void setItem(ItemStack stack) {
        item = stack.copy();
        for (Object[] o : live) ((ItemEntity) o[0]).setStack(stack.copy());
    }

    public static void clearAll() {
        for (Object[] o : live) ((ItemEntity) o[0]).discard();
        live.clear();
    }

    private static void spawn(ClientWorld world, BlockPos pos, BlockState st) {
        Direction dir = st.get(DispenserBlock.FACING);
        Vec3d c = Vec3d.ofCenter(pos).offset(dir, 0.7).add(0, -0.3, 0);
        double vx = dir.getOffsetX() * 0.2 + (rng.nextDouble() - 0.5) * 0.02;
        double vy = 0.2 * dir.getOffsetY() + 0.1;
        double vz = dir.getOffsetZ() * 0.2 + (rng.nextDouble() - 0.5) * 0.02;
        ItemEntity e = new ItemEntity(world, c.x, c.y, c.z, item.copy(), vx, vy, vz);
        e.setPickupDelay(32767);
        e.setId(nextId--);
        world.addEntity(e);
        live.add(new Object[]{e, 0});
    }
}
