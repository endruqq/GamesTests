package com.endruqq.imperium.client;

import com.endruqq.imperium.network.ActionPacket;
import com.endruqq.imperium.network.ImperiumNet;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Optional;

/** The detached top-down camera of the RTS mode. */
public final class RtsCamera {
    public static boolean active;
    private static Marker camEntity;
    /** ground point the camera looks at */
    public static double focusX, focusZ;
    private static double groundY;
    public static float yaw;
    public static float pitch = 55F;
    public static double height = 28;
    private static long lastNanos;
    /** captured each frame for picking / projection */
    private static final Matrix4f VIEW_PROJ = new Matrix4f();
    private static Vec3 camPos = Vec3.ZERO;
    private static boolean matricesValid;
    /** when false (e.g. strategic map open) keyboard panning is disabled */
    public static boolean keyboardPan = true;

    private RtsCamera() {}

    public static void enter() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || active) return;
        camEntity = new Marker(EntityType.MARKER, mc.level);
        focusX = mc.player.getX();
        focusZ = mc.player.getZ();
        groundY = mc.player.getY();
        yaw = mc.player.getYRot();
        pitch = 55F;
        height = 26;
        lastNanos = Util.getNanos();
        active = true;
        update(true);
        mc.setCameraEntity(camEntity);
        CompoundTag t = new CompoundTag();
        t.putBoolean("active", true);
        ImperiumNet.sendToServer(new ActionPacket("rts", t));
    }

    public static void exit() {
        Minecraft mc = Minecraft.getInstance();
        if (!active) return;
        active = false;
        matricesValid = false;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        camEntity = null;
        CompoundTag t = new CompoundTag();
        t.putBoolean("active", false);
        if (mc.getConnection() != null) ImperiumNet.sendToServer(new ActionPacket("rts", t));
    }

    public static double maxDistance() {
        Minecraft mc = Minecraft.getInstance();
        return Math.max(32, (mc.options.getEffectiveRenderDistance() - 1) * 16 - 8);
    }

    public static void lookAt(double x, double z) {
        focusX = x;
        focusZ = z;
        clamp();
    }

    private static void clamp() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double dx = focusX - mc.player.getX(), dz = focusZ - mc.player.getZ();
        double max = maxDistance();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > max) {
            focusX = mc.player.getX() + dx / dist * max;
            focusZ = mc.player.getZ() + dz / dist * max;
        }
    }

    private static boolean down(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
    }

    /** Called every frame. */
    public static void update(boolean force) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || camEntity == null || mc.level == null) return;
        long now = Util.getNanos();
        double dt = Math.min(0.1, (now - lastNanos) / 1.0E9);
        lastNanos = now;
        if (keyboardPan && mc.screen != null && !(mc.screen.getFocused() instanceof net.minecraft.client.gui.components.EditBox)) {
            double speed = (height * 0.9 + 12) * (down(GLFW.GLFW_KEY_LEFT_SHIFT) ? 2.5 : 1.0) * dt;
            double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
            double rx = -Math.cos(Math.toRadians(yaw)), rz = -Math.sin(Math.toRadians(yaw));
            double mx = 0, mz = 0;
            if (down(GLFW.GLFW_KEY_W) || down(GLFW.GLFW_KEY_UP)) { mx += fx; mz += fz; }
            if (down(GLFW.GLFW_KEY_S) || down(GLFW.GLFW_KEY_DOWN)) { mx -= fx; mz -= fz; }
            if (down(GLFW.GLFW_KEY_D) || down(GLFW.GLFW_KEY_RIGHT)) { mx += rx; mz += rz; }
            if (down(GLFW.GLFW_KEY_A) || down(GLFW.GLFW_KEY_LEFT)) { mx -= rx; mz -= rz; }
            focusX += mx * speed;
            focusZ += mz * speed;
            if (down(GLFW.GLFW_KEY_Q)) yaw -= (float) (90 * dt);
            if (down(GLFW.GLFW_KEY_E)) yaw += (float) (90 * dt);
            if (down(GLFW.GLFW_KEY_PAGE_UP)) pitch = Math.min(89, pitch + (float) (40 * dt));
            if (down(GLFW.GLFW_KEY_PAGE_DOWN)) pitch = Math.max(25, pitch - (float) (40 * dt));
        }
        clamp();
        int gx = Mth.floor(focusX), gz = Mth.floor(focusZ);
        double target = groundY;
        if (mc.level.hasChunkAt(new net.minecraft.core.BlockPos(gx, 64, gz))) {
            target = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, gx, gz);
        }
        groundY = force ? target : groundY + (target - groundY) * Math.min(1, dt * 4);
        double p = Math.toRadians(pitch);
        double dist = height / Math.sin(p);
        double cx = focusX + Math.sin(Math.toRadians(yaw)) * Math.cos(p) * dist;
        double cz = focusZ - Math.cos(Math.toRadians(yaw)) * Math.cos(p) * dist;
        double cy = Math.min(mc.level.getMaxBuildHeight() + 40, groundY + height);
        camEntity.setPos(cx, cy, cz);
        camEntity.xo = cx;
        camEntity.yo = cy;
        camEntity.zo = cz;
        camEntity.xOld = cx;
        camEntity.yOld = cy;
        camEntity.zOld = cz;
        camEntity.setYRot(yaw);
        camEntity.setXRot(pitch);
        camEntity.yRotO = yaw;
        camEntity.xRotO = pitch;
    }

    public static void zoom(double scroll) {
        height = Mth.clamp(height * (scroll > 0 ? 0.88 : 1.13), 6, 110);
    }

    public static void rotate(double dx, double dy) {
        yaw += (float) (dx * 0.4);
        pitch = (float) Mth.clamp(pitch + dy * 0.3, 25, 89);
    }

    // ------------------------------------------------------------------ projection

    public static void captureMatrices(Matrix4f projection, Matrix4f view, Vec3 cameraPos) {
        VIEW_PROJ.set(projection).mul(view);
        camPos = cameraPos;
        matricesValid = true;
    }

    /** @return GUI coordinates, or null if behind the camera */
    @Nullable
    public static double[] project(double x, double y, double z) {
        if (!matricesValid) return null;
        Minecraft mc = Minecraft.getInstance();
        Vector4f v = new Vector4f((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z), 1F);
        VIEW_PROJ.transform(v);
        if (v.w <= 0.05F) return null;
        double nx = v.x / v.w, ny = v.y / v.w;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        return new double[]{(nx + 1) / 2 * w, (1 - ny) / 2 * h};
    }

    /** @return {origin, direction} of the ray under the mouse */
    @Nullable
    public static Vec3[] ray(double mouseX, double mouseY) {
        if (!matricesValid) return null;
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        float nx = (float) (mouseX / w * 2 - 1), ny = (float) (1 - mouseY / h * 2);
        Matrix4f inv = new Matrix4f(VIEW_PROJ).invert();
        Vector4f near = new Vector4f(nx, ny, -1F, 1F);
        inv.transform(near);
        near.div(near.w);
        Vector4f far = new Vector4f(nx, ny, 1F, 1F);
        inv.transform(far);
        far.div(far.w);
        Vec3 origin = camPos.add(near.x, near.y, near.z);
        Vec3 dir = new Vec3(far.x - near.x, far.y - near.y, far.z - near.z).normalize();
        return new Vec3[]{origin, dir};
    }

    @Nullable
    public static BlockHitResult pickBlock(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Vec3[] r = ray(mouseX, mouseY);
        if (r == null || mc.level == null || mc.player == null) return null;
        Vec3 to = r[0].add(r[1].scale(400));
        BlockHitResult hit = mc.level.clip(new ClipContext(r[0], to, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, mc.player));
        return hit.getType() == HitResult.Type.MISS ? null : hit;
    }

    @Nullable
    public static Entity pickEntity(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Vec3[] r = ray(mouseX, mouseY);
        if (r == null || mc.level == null) return null;
        Vec3 to = r[0].add(r[1].scale(400));
        BlockHitResult block = pickBlock(mouseX, mouseY);
        double maxDist = block == null ? Double.MAX_VALUE : block.getLocation().distanceToSqr(r[0]) + 4;
        Entity best = null;
        double bestDist = maxDist;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == camEntity || !e.isPickable() && !(e instanceof net.minecraft.world.entity.LivingEntity)) continue;
            AABB box = e.getBoundingBox().inflate(0.25);
            Optional<Vec3> hit = box.clip(r[0], to);
            if (hit.isPresent()) {
                double dd = hit.get().distanceToSqr(r[0]);
                if (dd < bestDist) {
                    bestDist = dd;
                    best = e;
                }
            }
        }
        return best;
    }

    public static Vec3 cameraPosition() {
        return camPos;
    }
}
